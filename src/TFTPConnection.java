import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

public abstract class TFTPConnection {
    public static final int MAX_SEND_WINDOW_SIZE = (AggregablePacket.MAX_BLOCK_NUMBER + 1) / 2;
    public static final int SEND_WINDOW_SIZE = 15;
    public static final int RECEIVE_WINDOW_SIZE = SEND_WINDOW_SIZE;
    public static final double SIMULATE_DROP_PERCENT = 0.01;

    public static final int MAX_NUMBER_OF_RETRIES = 10;
    public static final int TIMEOUT = 15;

    protected InetAddress remoteAddress;
    protected int destPort;
    protected DatagramSocket socket;
    protected boolean slidingWindow;
    protected boolean simulateDrop;

    protected DatagramPacket buildOutgoingDatagram(TFTPPacket tftpPacket, int destPort) {
        byte[] bytePacket = tftpPacket.toBytes();
        DatagramPacket datagramPacket = new DatagramPacket(bytePacket, bytePacket.length);
        datagramPacket.setAddress(remoteAddress);
        datagramPacket.setPort(destPort);
        return datagramPacket;
    }

    protected static DatagramPacket buildOutgoingDatagram(TFTPPacket tftpPacket, InetAddress remoteAddress, int destPort) {
        byte[] bytePacket = tftpPacket.toBytes();
        DatagramPacket datagramPacket = new DatagramPacket(bytePacket, bytePacket.length);
        datagramPacket.setAddress(remoteAddress);
        datagramPacket.setPort(destPort);
        return datagramPacket;
    }

    protected static DatagramPacket buildIncomingDatagram(int bufferSize) {
        byte[] responseBuffer = new byte[bufferSize];
        return new DatagramPacket(responseBuffer, responseBuffer.length);
    }

    protected static boolean sendDatagram(DatagramSocket socket, DatagramPacket datagramPacket,
                                       boolean simulateDrop) throws IOException {
        Random rand = new Random();
        if (!simulateDrop || rand.nextDouble() > SIMULATE_DROP_PERCENT) {
            socket.send(datagramPacket);
            return true;
        }
        return false;
    }
}
