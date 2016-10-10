import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;

public class TFTPClient {
    public static final int MAX_FILE_SIZE = AggregablePacket.MAX_BLOCK_NUMBER * TFTPProtocol.MAX_DATA_BLOCK_LENGTH - 1;

    int port;
    DatagramSocket socket;

    public static void main (String[] args) throws IOException {
        if (args.length != 5) {
            System.out.println("Invalid command line arguments");
        }
        String remoteHostName = args[0];
        String fileName = args[1];
        String ipVersion = args[2];
        Boolean slidingWindow = Boolean.parseBoolean(args[3]);
        Boolean simulateDrop = Boolean.parseBoolean(args[4]);

        InetAddress remoteAddress;

        if (ipVersion.equals("IPv6")) {
            System.setProperty("java.net.preferIPv6Addresses", "true");
        }

        try {
            remoteAddress = InetAddress.getByName(remoteHostName);
        } catch (UnknownHostException e) {
            System.out.println("Unknown Host: " + remoteHostName);
            return;
        }

	if(remoteAddress instanceof Inet6Address) {
		System.out.println("Using IPv6");
		//return;
	}

        TFTPClient client = new TFTPClient();
        long time = System.currentTimeMillis();
        client.write(remoteAddress, fileName, slidingWindow, simulateDrop);

        System.out.println("Operation completed " + (System.currentTimeMillis() - time) + "ms");
        client.socket.close();
    }

    public TFTPClient() throws SocketException {
        //we assign a predefined port to be used for the duration of the transfer in order to regulate
        // traffic on the cs servers,normally we would assign a random port for this purpose.
        this.port = TFTPProtocol.OPERATIONAL_PORT;
        this.socket = new DatagramSocket(port);
    }

    public long write(InetAddress remoteAddress, String fileName, boolean slidingWindow, boolean simulateDrop) throws IOException {
        byte[] fileData;

        File file = new File(fileName);
        if (file.length() > MAX_FILE_SIZE) {
            throw new IOException("File is too large");
        }
        BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));
        fileData = new byte[(int)file.length()];
        inputStream.read(fileData);
        inputStream.close();

        TFTPClientWriteConnection connection = TFTPClientWriteConnection.makeConnection(fileName, socket,
                remoteAddress, slidingWindow, simulateDrop);
        //System.out.println("Successfully established connection with server");
        //System.out.println("Transferring file");
        long duration = connection.sendData(fileData);
        //System.out.println("File Transfer was successful");
        return duration;
    }
}
