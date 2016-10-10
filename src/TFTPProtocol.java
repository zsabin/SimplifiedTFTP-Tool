import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class TFTPProtocol {
    public static final int CONNECTION_PORT = 2691;
    public static final int OPERATIONAL_PORT = CONNECTION_PORT + 1000;

    public static final Charset charset = StandardCharsets.UTF_8;
    public static final byte ETX = 0;

    public static final int MAX_DATA_BLOCK_LENGTH = 512;
    public static final int MAX_PACKET_SIZE = MAX_DATA_BLOCK_LENGTH + DataPacket.HEADER_SIZE;

    public static TFTPPacket parsePacket(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int opcode = getNextValue(buffer, AggregablePacket.BLOCK_NUMBER_SIZE);

        PacketType packetType = PacketType.createTFTPPacketType(opcode);

        switch(packetType) {
            case WRQ:
                boolean slidingWindow = ByteBoolean.createByteBoolean(buffer.get()).getBoolValue();
                String fileName = getNextString(buffer);
                return new WRQPacket(fileName, slidingWindow);
            case DATA:
                int blockNumber = getNextValue(buffer, AggregablePacket.BLOCK_NUMBER_SIZE);
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                return new DataPacket(blockNumber, data);
            case ACK:
                blockNumber = getNextValue(buffer, AggregablePacket.BLOCK_NUMBER_SIZE);
                return new ACKPacket(blockNumber);
            case ERROR:
                int errorCode = getNextValue(buffer, ErrorPacket.ERROR_CODE_SIZE);;
                String errorMsg = getNextString(buffer);
                return new ErrorPacket(errorCode, errorMsg);
            default:
                return null;
        }
    }

    private static String getNextString(ByteBuffer byteBuffer) {
        byte b;
        ByteBuffer subBuffer = ByteBuffer.allocate(byteBuffer.remaining());

        while (( b = byteBuffer.get()) != ETX) {
            subBuffer.put(b);
        }

        return new String(subBuffer.array(), 0, subBuffer.position(), charset);
    }

    private static int getNextValue(ByteBuffer byteBuffer, int sizeInBytes) {
        int value = 0;
        for (int i = 0; i < sizeInBytes; i++) {
            int intValue = byteBuffer.get() & 0xFF;
            value = intValue + (value << Byte.SIZE);
        }
        return value;
    }
}
