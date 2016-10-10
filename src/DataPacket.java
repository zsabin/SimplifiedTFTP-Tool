import java.nio.ByteBuffer;

public class DataPacket extends AggregablePacket {
    public static final int HEADER_SIZE = OPCODE_SIZE + BLOCK_NUMBER_SIZE;

    private byte[] data;

    public DataPacket(int blockNumber, byte[] data) {
        this.blockNumber = blockNumber;
        this.data = data;

        this.size = OPCODE_SIZE + BLOCK_NUMBER_SIZE + data.length;

        byteBuffer = ByteBuffer.allocate(size);
        byteBuffer.put(valueToBytes(getType().getOpcode(), OPCODE_SIZE));
        byteBuffer.put(valueToBytes(blockNumber, BLOCK_NUMBER_SIZE));
        byteBuffer.put(data);
    }

    public int getBlockNumber() {
        return this.blockNumber;
    }

    public byte[] getData() {
        return this.data;
    }

    public PacketType getType() {
        return PacketType.DATA;
    }
}
