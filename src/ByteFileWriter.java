import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

public class ByteFileWriter {

    public static void main (String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Invalid command line arguments");
        }
        write(args[0], Integer.parseInt(args[1]));
    }

    public static void write(String fileName, int fileSize) throws IOException {
        Random rand = new Random();

        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fileName));

        byte[] bytes = new byte[fileSize];
        rand.nextBytes(bytes);

        out.write(bytes);
        out.flush();
    }
}
