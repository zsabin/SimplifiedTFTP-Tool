import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NetworkMetricTest {

    public static final int TEST_COUNT = 100;
    public static final String FILE_NAME_0 = "testFile_1K.bytes";
    public static final String FILE_NAME_1 = "testFile_16K.bytes";
    public static final String FILE_NAME_2 = "testFile_64K.bytes";
    public static final String FILE_NAME_3 = "testFile_256K.bytes";
    public static final String FILE_NAME_4 = "testFile_1MB.bytes";

    public static void main (String[] args) throws IOException {
        if (args.length != 5) {
            System.out.println("Invalid command line arguments");
        }
        String remoteHostName = args[0];
        String ipVersion = args[1];
        Boolean slidingWindow = Boolean.parseBoolean(args[2]);
        Boolean simulateDrop = Boolean.parseBoolean(args[3]);
        String resultsFileName = args[4];

        ByteFileWriter.write(FILE_NAME_0, 1024);
        ByteFileWriter.write(FILE_NAME_1, 16384);
        ByteFileWriter.write(FILE_NAME_2, 65536);
        ByteFileWriter.write(FILE_NAME_3, 262144);
        ByteFileWriter.write(FILE_NAME_4, 1048576);

        PrintWriter resultsWriter = null;
        try {
            resultsWriter = new PrintWriter(resultsFileName);
        } catch (FileNotFoundException e) {
            System.out.println("Results File not found");
            return;
        }

        if (ipVersion.equals("IPv6")) {
            System.setProperty("java.net.preferIPv6Addresses", "true");
        }

        InetAddress remoteAddress;
        try {
            remoteAddress = InetAddress.getByName(remoteHostName);
        } catch (UnknownHostException e) {
            System.out.println("Unknown Host: " + remoteHostName);
            return;
        }

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();

        resultsWriter.println("NETWORK ANALYSIS TEST RESULTS");
        resultsWriter.println("START TIMESTAMP: " + dateFormat.format(date));
        resultsWriter.println("LOCAL HOST: " + InetAddress.getLocalHost().getHostName());
        resultsWriter.println("REMOTE HOST: " + remoteHostName);
        resultsWriter.println("FLOW CONTROL MODE: " + (slidingWindow ? "Sliding Window" : "Stop-And-Wait"));
        resultsWriter.println("SIMULATE PACKET DROP: " + simulateDrop);
        resultsWriter.println();

        resultsWriter.println("1K, 16K, 64K, 256K, 1MB");

        TFTPClient client = new TFTPClient();
        for (int i = 0; i < TEST_COUNT; i++) {
            long startTime = System.currentTimeMillis();
            boolean validTest = false;

            while (!validTest) {
                try {
                    resultsWriter.print(client.write(remoteAddress, FILE_NAME_0, slidingWindow, simulateDrop) + ", ");
                } catch (SocketTimeoutException e) {
                    continue;
                }
                validTest = true;
            }
            validTest = false;
            while (!validTest) {
                try {
                    resultsWriter.print(client.write(remoteAddress, FILE_NAME_1, slidingWindow, simulateDrop) + ", ");
                } catch (SocketTimeoutException e) {
                    continue;
                }
                validTest = true;
            }
            validTest = false;
            while (!validTest) {
                try {
                    resultsWriter.print(client.write(remoteAddress, FILE_NAME_2, slidingWindow, simulateDrop) + ", ");
                } catch (SocketTimeoutException e) {
                    continue;
                }
                validTest = true;
            }
            validTest = false;
            while (!validTest) {
                try {
                    resultsWriter.print(client.write(remoteAddress, FILE_NAME_3, slidingWindow, simulateDrop) + ", ");
                } catch (SocketTimeoutException e) {
                    continue;
                }
                validTest = true;
            }
            validTest = false;
            while (!validTest) {
                try {
                    resultsWriter.print(client.write(remoteAddress, FILE_NAME_4, slidingWindow, simulateDrop) + ", ");
                } catch (SocketTimeoutException e) {
                    continue;
                }
                validTest = true;
            }

            resultsWriter.println();
            long endTime = System.currentTimeMillis();
            double duration = endTime - startTime;
            int remainingTime = (int) Math.round(( (TEST_COUNT - i - 1) * duration ) * Math.pow(10, -3));
            System.out.println(remainingTime + " seconds remaining");
        }
        resultsWriter.flush();
        resultsWriter.close();
    }
}
