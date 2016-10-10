
import java.nio.ByteBuffer;

public abstract class TFTPPacket {
    public static final int OPCODE_SIZE = 2;

    public static byte[] stringToBytes(String s) {
        byte[] stringAsBytes = s.getBytes(TFTPProtocol.charset);
        byte[] buffer = new byte[stringAsBytes.length + 1];
        System.arraycopy(stringAsBytes, 0, buffer, 0, stringAsBytes.length);
        buffer[buffer.length - 1] = TFTPProtocol.ETX;
        return buffer;
    }

    public static byte[] valueToBytes(int value, int sizeInBytes) {
        byte[] buffer = new byte[sizeInBytes];
        for (int i = 0; i < sizeInBytes; i++) {
            buffer[sizeInBytes - (i + 1)] = (byte)(value >> (Byte.SIZE * i));
        }
        return buffer;
    }

    protected ByteBuffer byteBuffer;
    protected int size;

    public byte[] toBytes() {
        return this.byteBuffer.array();
    }

    public PacketType getType() {
        return PacketType.UNDEF;
    }
}
