import java.nio.ByteBuffer;

public class WRQPacket extends TFTPPacket {

    private String fileName;
    private boolean slidingWindow;

    public WRQPacket(String fileName, boolean slidingWindow) {
        this.fileName = fileName;
        this.slidingWindow = slidingWindow;

        byte[] fileNameAsBytes = stringToBytes(fileName);
        this.size = OPCODE_SIZE + ByteBoolean.sizeInBytes + fileNameAsBytes.length;

        byteBuffer = ByteBuffer.allocate(size);
        byteBuffer.put(valueToBytes(getType().getOpcode(), OPCODE_SIZE));
        byteBuffer.put(ByteBoolean.createByteBoolean(slidingWindow).getByteValue());
        byteBuffer.put(fileNameAsBytes);
    }

    public String getFileName() {
        return fileName;
    }

    public boolean usesSlidingWindow() {
        return this.slidingWindow;
    }

    public PacketType getType() {
        return PacketType.WRQ;
    }
}
