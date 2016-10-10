import java.io.*;

public class ByteFileComparator {

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Invalid command line arguments");
        }

        String fileName1 = args[0];
        String fileName2 = args[1];

        File file1 = new File(fileName1);
        File file2 = new File(fileName2);

        if (file1.length() != file2.length()) {
            System.out.println("File lengths do not match");
            System.out.println("file1 length = " + file1.length());
            System.out.println("file2 length = " + file2.length());
            return;
        }

        byte[] fileData1;
        byte[] fileData2;

        BufferedInputStream in = new BufferedInputStream(new FileInputStream(file1));
        fileData1 = new byte[(int)file1.length()];
        in.read(fileData1);

        in = new BufferedInputStream(new FileInputStream(file2));
        fileData2 = new byte[(int)file2.length()];
        in.read(fileData2);

        if (!byteArraysAreEqual(fileData1, fileData2)) {
            System.out.println("File content is not the same");
            return;
        }

        System.out.println("Byte Comparison was Successful: Byte Files are Equal");
    }

    private static boolean byteArraysAreEqual(byte[] b1, byte[] b2) {
        for (int i = 0; i < b1.length ; i++) {
            if (b1[i] != b2[i]) {
                return false;
            }
        }
        return true;
    }
}
