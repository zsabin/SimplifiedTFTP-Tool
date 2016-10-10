import java.nio.ByteBuffer;

public class ACKPacket extends AggregablePacket {

    public ACKPacket (int blockNumber) {
        this.blockNumber = blockNumber;

        this.size = OPCODE_SIZE + BLOCK_NUMBER_SIZE;

        byteBuffer = ByteBuffer.allocate(size);
        byteBuffer.put(valueToBytes(getType().getOpcode(), OPCODE_SIZE));
        byteBuffer.put(valueToBytes(blockNumber, BLOCK_NUMBER_SIZE));
    }

    public int getBlockNumber() {
        return this.blockNumber;
    }

    public PacketType getType() {
        return PacketType.ACK;
    }
}
