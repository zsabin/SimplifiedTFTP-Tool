import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class TFTPServer {

    public static void main(String[] args) throws SocketException {
        if (args.length < 2 ) {
            System.out.println("Invalid command line arguments");
            return;
        }
        boolean simulateDrop = Boolean.parseBoolean(args[0]);
        String ipVersion = args[1];

        if (ipVersion.equals("IPv6")) {
            System.setProperty("java.net.preferIPv6Addresses", "true");
        }

        DatagramSocket connectionSocket = new DatagramSocket(TFTPProtocol.CONNECTION_PORT);

        for (;;) {
            TFTPServerWriteConnection connection;
            try {
                connection = TFTPServerWriteConnection.receiveWRQ(connectionSocket, simulateDrop);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Unable to make connection with client");
                return;
            }
            //System.out.println("Successfully established connection with client");

            try {
                connection.receiveData();
            }
            catch (SocketTimeoutException e) {}
            catch (IOException e) {
                e.printStackTrace();
                System.out.println("Unable to complete write operation");
                connection.close();
                return;
            }

            connection.close();
        }
    }
}
