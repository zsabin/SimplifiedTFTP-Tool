import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

public class TFTPServerWriteConnection extends TFTPConnection {

    private int lastPacketReceived;
    private int largestAcceptablePacket;
    private int packetToACK;
    private int receiveWindowSize;
    private PacketTransmissionData outstandingACK;
    private ArrayList<DataPacket> bufferedPackets;
    private int lastBlockWritten;

    private BufferedOutputStream fileOutputStream;

    private TFTPServerWriteConnection(PacketTransmissionData outstandingAck,
                                      BufferedOutputStream fileOutputStream,
                                      DatagramSocket socket,
                                      InetAddress remoteAddress,
                                      int destPort,
                                      boolean slidingWindow,
                                      boolean simulateDrop) {
        this.fileOutputStream = fileOutputStream;
        this.socket = socket;
        this.remoteAddress = remoteAddress;
        this.destPort = destPort;
        this.slidingWindow = slidingWindow;
        this.receiveWindowSize = slidingWindow ? RECEIVE_WINDOW_SIZE : 1;
        this.simulateDrop = simulateDrop;

        this.lastPacketReceived = 0;
        updateReceiveWindow();
        this.packetToACK = 1;
        this.outstandingACK = outstandingAck;
        this.bufferedPackets = new ArrayList<DataPacket>();
        this.lastBlockWritten = 0;
    }

    public static TFTPServerWriteConnection receiveWRQ(DatagramSocket connectionSocket, boolean simulateDrop) throws IOException {
        DatagramPacket datagramRequest = buildIncomingDatagram(TFTPProtocol.MAX_PACKET_SIZE);

        connectionSocket.receive(datagramRequest);
        int destPort = datagramRequest.getPort();
        InetAddress remoteAddress = datagramRequest.getAddress();

        //we assign a predefined port to be used for the duration of the transfer in order to regulate
        // traffic on the cs servers,normally we would assign a random port for this purpose.
        DatagramSocket socket = new DatagramSocket(TFTPProtocol.OPERATIONAL_PORT + 1);
        socket.setSoTimeout(TIMEOUT);

        return parseWRQPacket(datagramRequest, socket, remoteAddress, destPort, simulateDrop);
    }

    private static TFTPServerWriteConnection parseWRQPacket(DatagramPacket datagramRequest,
                                                            DatagramSocket socket,
                                                            InetAddress remoteAddress,
                                                            int destPort,
                                                            boolean simulateDrop) throws IOException {
        TFTPPacket tftpRequest = TFTPProtocol.parsePacket(datagramRequest.getData());
        if (tftpRequest == null) {
            throw new IOException("Response packet was corrupted: Unable to send data");
        }

        switch(tftpRequest.getType()) {
            case WRQ :
                PacketTransmissionData outstandingACK;
                BufferedOutputStream fileOutputStream;
                WRQPacket wrqPacket = (WRQPacket)tftpRequest;
                File file = new File(wrqPacket.getFileName() + "_server");
                boolean slidingWindow = wrqPacket.usesSlidingWindow();
                try {
                    fileOutputStream = new BufferedOutputStream(new FileOutputStream(file));
                } catch (FileNotFoundException e) {
                    String errorMsg = "File Not Found";
                    ErrorPacket errorPacket = new ErrorPacket(ErrorCode.FILE_NOT_FOUND, errorMsg);
                    DatagramPacket errorDatagram = buildOutgoingDatagram(errorPacket, remoteAddress, destPort);
                    sendDatagram(socket, errorDatagram, simulateDrop);
                    throw new FileNotFoundException();
                }
                ACKPacket ackPacket = new ACKPacket(0);
                DatagramPacket ackDatagram = buildOutgoingDatagram(ackPacket, remoteAddress, destPort);
                sendDatagram(socket, ackDatagram, simulateDrop, 0);

                outstandingACK = new PacketTransmissionData(ackPacket, System.currentTimeMillis());
                return new TFTPServerWriteConnection(outstandingACK, fileOutputStream, socket, remoteAddress,
                        destPort, slidingWindow, simulateDrop);
            default :
                String errorMsg = "Illegal Operation";
                ErrorPacket errorPacket = new ErrorPacket(ErrorCode.ILLEGAL_OPERATION, errorMsg);
                DatagramPacket errorDatagram = buildOutgoingDatagram(errorPacket, remoteAddress, destPort);
                sendDatagram(socket, errorDatagram, simulateDrop);
                throw new IOException();
        }
    }

    public void receiveData() throws IOException {
        boolean dataTransferComplete = false;
        boolean lastACKReceived = false;
        DatagramPacket datagramPacket;
        do {
            try {
                datagramPacket = buildIncomingDatagram(TFTPProtocol.MAX_PACKET_SIZE);
                socket.receive(datagramPacket);

                byte[] bytePacket = Arrays.copyOfRange(datagramPacket.getData(), 0, datagramPacket.getLength());
                TFTPPacket tftpPacket = TFTPProtocol.parsePacket(bytePacket);

                if (tftpPacket != null && tftpPacket.getType() == PacketType.DATA) {
                    if (!processDataPacket((DataPacket)tftpPacket)) {
                        throw new SocketTimeoutException();
                    }
                    if (((DataPacket) tftpPacket).getData().length < TFTPProtocol.MAX_DATA_BLOCK_LENGTH) {
                        dataTransferComplete = true;
                    }
                }
                else {
                    throw new IOException("Data packet was corrupted");
                }
            } catch (SocketTimeoutException e) {
                if (outstandingACK.retries == MAX_NUMBER_OF_RETRIES) {
                    throw new SocketTimeoutException();
                }
                DatagramPacket ackDatagram = buildOutgoingDatagram(outstandingACK.packet, destPort);
                sendDatagram(socket, ackDatagram, simulateDrop, outstandingACK.packet.getBlockNumber());
                outstandingACK.timestamp = System.currentTimeMillis();
                outstandingACK.retries++;
                continue;
            }
        } while(!dataTransferComplete && !lastACKReceived);
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    //Processes the specified data packet and returns true if it is a valid data packet withing the receiving window
    private boolean processDataPacket(DataPacket packet) throws IOException {
        if (isInReceiveWindow(packet.getBlockNumber())) {
            bufferedPackets.add(packet);
            Collections.sort(bufferedPackets);
            updatePacketToACK();
            if (packet.getBlockNumber() == lastPacketReceived + 1) {
                this.lastPacketReceived = packetToACK;
                updateReceiveWindow();

                ACKPacket ackPacket = new ACKPacket(packetToACK);
                DatagramPacket ackDatagram = buildOutgoingDatagram(ackPacket, destPort);
                sendDatagram(socket, ackDatagram, simulateDrop, ackPacket.getBlockNumber());

                outstandingACK = new PacketTransmissionData(ackPacket, System.currentTimeMillis());
                try {
                    DataPacket bufferedPacket = bufferedPackets.get(0);
                    while (bufferedPacket != null && bufferedPacket.getBlockNumber() <= packetToACK) {
                        fileOutputStream.write(bufferedPacket.getData());
                        bufferedPackets.remove(0);
                        bufferedPacket = bufferedPackets.size() > 0 ? bufferedPackets.get(0) : null;
                    }
                } catch (IOException e) {
                    ErrorPacket errorPacket = new ErrorPacket(ErrorCode.NOT_DEFINED, "Unable to write to disk");
                    DatagramPacket errorDatagram = buildOutgoingDatagram(errorPacket, destPort);
                    sendDatagram(socket, errorDatagram, simulateDrop);
                    throw new IOException();
                }
            }
            return true;
        }
        return false;
    }

    private boolean isInReceiveWindow(int blockNumber) {
        return blockNumber > lastPacketReceived && blockNumber <= largestAcceptablePacket;
    }

    private void updateReceiveWindow() {
        this.largestAcceptablePacket = lastPacketReceived + receiveWindowSize;
    }

    private void updatePacketToACK() {
        Iterator<DataPacket> iter = bufferedPackets.iterator();
        while (iter.hasNext()) {
            if (iter.next().getBlockNumber() != packetToACK + 1) {
                break;
            }
            packetToACK++;
        }
    }

    public void close() {
        socket.close();
    }
}
