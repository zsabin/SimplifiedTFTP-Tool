public abstract class AggregablePacket extends TFTPPacket implements Comparable<AggregablePacket> {
    public static final int MAX_BLOCK_NUMBER = (int)Math.pow(2, 16) - 1;
    public static final int BLOCK_NUMBER_SIZE = 2;

    public int blockNumber;

    public int getBlockNumber() {
       return blockNumber;
   }

    public int compareTo(AggregablePacket p) {
        return this.getBlockNumber() - p.getBlockNumber();
    }
}
