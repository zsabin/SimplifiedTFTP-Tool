import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.LinkedList;

public class TFTPClientWriteConnection extends TFTPConnection {
    int lastAckReceived;
    int lastBlockSent;
    //The first element in this list will be the greatest unacknowledged packet i.e blockNumber = lastAckReceived + 1
    LinkedList<PacketTransmissionData> outstandingPackets = new LinkedList<PacketTransmissionData>();

    private TFTPClientWriteConnection(DatagramSocket socket,
                                      InetAddress remoteAddress,
                                      int destPort,
                                      boolean slidingWindow,
                                      boolean simulateDrop) {
        this.socket = socket;
        this.remoteAddress = remoteAddress;
        this.destPort = destPort;
        this.slidingWindow = slidingWindow;
        this.simulateDrop = simulateDrop;

        this.lastAckReceived = 0;
        this.lastBlockSent = 0;
    }

    public static TFTPClientWriteConnection makeConnection(String fileName,
                                                           DatagramSocket socket,
                                                           InetAddress remoteAddress,
                                                           boolean slidingWindow,
                                                           boolean simulateDrop) throws IOException {
        //build WRQ and response datagrams
        WRQPacket wrqPacket = new WRQPacket(fileName, slidingWindow);
        DatagramPacket wrqDatagram = buildOutgoingDatagram(wrqPacket, remoteAddress, TFTPProtocol.CONNECTION_PORT);
        DatagramPacket responseDatagram = buildIncomingDatagram(TFTPProtocol.MAX_PACKET_SIZE);

        socket.setSoTimeout(TIMEOUT);

        //send WRQ and wait for ACK
        try {
            sendPacket(socket, wrqDatagram, responseDatagram, MAX_NUMBER_OF_RETRIES, simulateDrop);
        } catch (IOException e) {
            throw new IOException("Unable to make connection");
        }

        int destPort = parseWRQResponse(responseDatagram);
        return new TFTPClientWriteConnection(socket, remoteAddress, destPort, slidingWindow, simulateDrop);
    }

    private static void sendPacket(DatagramSocket socket, DatagramPacket packet,
                                   DatagramPacket response, int maxNumberOfRetries, boolean simulateDrop) throws IOException {
        for (int sendAttempt = 0; sendAttempt < maxNumberOfRetries + 1; sendAttempt++) {
            sendDatagram(socket, packet, simulateDrop, 0);
            try {
                socket.receive(response);
                return;
            } catch (SocketTimeoutException e) {}
        }
        throw new SocketTimeoutException();
    }

    //Parses the WRQ response and returns the destination port number if the connection was successful
    private static int parseWRQResponse(DatagramPacket responseDatagram) throws IOException {
        byte[] responseData = Arrays.copyOfRange(responseDatagram.getData(), 0, responseDatagram.getLength());
        TFTPPacket tftpResponse = TFTPProtocol.parsePacket(responseData);
        if (tftpResponse == null) {
            throw new IOException("Packet was corrupted");
        }

        switch (tftpResponse.getType()) {
            case ACK :
                if (((ACKPacket)tftpResponse).getBlockNumber() != 0) {
                    throw new IOException("Invalid block number: unable to make connection");
                }
                return responseDatagram.getPort();
            case ERROR:
                ErrorPacket errorPacket = (ErrorPacket)tftpResponse;
                throw new IOException(errorPacket.getErrorMsg() + ": Unable to make connection");
            default :
                throw new IOException("Response packet was corrupted: Unable to make connection");
        }
    }

    public long sendData(byte[] fileData) throws IOException {
        //The last packet contains a partially-full data block, which may have a length of 0
        int packetCount = (int)((1.0 * fileData.length) / TFTPProtocol.MAX_DATA_BLOCK_LENGTH) + 1;

        long startTime = System.currentTimeMillis();
        while (lastAckReceived < packetCount) {
            if (slidingWindow) {
                fillSendWindow(fileData, packetCount);
            } else {
                sendNextDataPacket(fileData);
            }
            waitForACK();
        }
        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }

    private void fillSendWindow(byte[] data, int packetCount) throws IOException {
        while (lastBlockSent < packetCount && !sendWindowIsFull()) {
            sendNextDataPacket(data);
        }
    }

    private void sendNextDataPacket(byte[] data) throws IOException {
        int blockNumber = lastBlockSent + 1;
        byte[] dataBlock = getDataBlock(data, blockNumber - 1);

        DataPacket tftpPacket = new DataPacket(blockNumber, dataBlock);

        DatagramPacket datagramPacket = buildOutgoingDatagram(tftpPacket, destPort);
        sendDatagram(socket, datagramPacket, simulateDrop, blockNumber);

        outstandingPackets.add(new PacketTransmissionData(tftpPacket, System.currentTimeMillis()));
        lastBlockSent = blockNumber;
    }

    private byte[] getDataBlock(byte[] data, int blockNumber) {
        int startByteIndex = blockNumber * TFTPProtocol.MAX_DATA_BLOCK_LENGTH;
        int endByteIndex = (blockNumber + 1) * TFTPProtocol.MAX_DATA_BLOCK_LENGTH;
        endByteIndex = endByteIndex > data.length ? data.length : endByteIndex;
        return Arrays.copyOfRange(data, startByteIndex, endByteIndex);
    }

    private void waitForACK() throws IOException {
        boolean ackReceived = false;
        while (!ackReceived) {
            byte[] responseBuffer = new byte[TFTPProtocol.MAX_PACKET_SIZE];
            DatagramPacket responseDatagram = new DatagramPacket(responseBuffer, responseBuffer.length);

            PacketTransmissionData outstandingPacketData = outstandingPackets.pop();
            try {
                int adjustedTimeout = calculateRemainingTimeout(outstandingPacketData);
                socket.setSoTimeout(adjustedTimeout);
                socket.receive(responseDatagram);
            } catch (SocketTimeoutException e) {
                if (outstandingPacketData.retries == MAX_NUMBER_OF_RETRIES) {
                    throw new SocketTimeoutException();
                }
                //Resend packet
                DatagramPacket datagramPacket = buildOutgoingDatagram(outstandingPacketData.packet, destPort);
                sendDatagram(socket, datagramPacket, simulateDrop, outstandingPacketData.packet.getBlockNumber());

                //This packet remains the next expected packet to be acknowledged since we will not receive an
                // ACK for any other outstanding packets until this packet is acknowledged.
                outstandingPacketData.timestamp = System.currentTimeMillis();
                outstandingPacketData.retries++;
                outstandingPackets.push(outstandingPacketData);
                continue;
            }

            ackReceived = parseResponse(responseDatagram, outstandingPacketData);
        }
    }

    private int calculateRemainingTimeout(PacketTransmissionData outstandingPacketData) throws SocketTimeoutException {
        int timeElapsed = (int) (System.currentTimeMillis() - outstandingPacketData.timestamp);
        int adjustedTimeout = TIMEOUT - timeElapsed;
        if (adjustedTimeout <= 0) {
            throw new SocketTimeoutException();
        }
        return adjustedTimeout;
    }

    //Parses the response to a data packet.
    //Returns true if it was a valid ACK and false otherwise.
    private boolean parseResponse(DatagramPacket responseDatagram, PacketTransmissionData outstandingPacketData)
            throws IOException {
        byte[] responseData = Arrays.copyOfRange(responseDatagram.getData(), 0, responseDatagram.getLength());
        TFTPPacket tftpResponse = TFTPProtocol.parsePacket(responseData);
        if (tftpResponse == null) {
            throw new IOException("Response packet was corrupted: Unable to send data");
        }
        switch (tftpResponse.getType()) {
            case ACK:
                ACKPacket ackPacket = (ACKPacket) tftpResponse;
                return processACK(ackPacket, outstandingPacketData);
            case ERROR:
                ErrorPacket errorPacket = (ErrorPacket) tftpResponse;
                throw new IOException(errorPacket.getErrorMsg() + ": Unable to send data");
            default:
                throw new IOException("Response packet was corrupted: Unable to send data");
        }
    }

    //Processes the given ACK packet.
    //Returns true if it is a valid ACK, returns false otherwise.
    private boolean processACK(ACKPacket ackPacket, PacketTransmissionData outstandingPacketData){
        //We received a duplicate ACK so we push the outstanding packet back onto the list
        //System.out.println("Processing packet " + ackPacket.getBlockNumber());
        if (ackPacket.getBlockNumber() <= lastAckReceived) {
            //System.out.println("block number <= lastACKReceived: " + lastAckReceived);
            outstandingPackets.push(outstandingPacketData);
            return false;
        }

        lastAckReceived = ackPacket.getBlockNumber();

        if (lastAckReceived != outstandingPacketData.packet.getBlockNumber()) {
            //System.out.println("block number != lastACKReceived: " + lastAckReceived);
            //The last ACK acknowledged multiple packets so we traverse the list of outstanding
            // packets to remove all packets that have been acknowledged
            int outstandingPacketCount = outstandingPackets.size();
            for (int i = 0; i < outstandingPacketCount; i++) {
                DataPacket packet = (DataPacket) outstandingPackets.peek().packet;
                if (packet.getBlockNumber() > lastAckReceived) {
                    break;
                }
                PacketTransmissionData p = outstandingPackets.pop();
            }
        }
        return true;
    }

    private boolean sendWindowIsFull() {
        return lastBlockSent - lastAckReceived >= SEND_WINDOW_SIZE;
    }
}
