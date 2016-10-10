public class PacketTransmissionData {
    public AggregablePacket packet;
    public long timestamp;
    public int retries;

    public PacketTransmissionData(AggregablePacket packet, long timestamp) {
        this.packet = packet;
        this.timestamp = timestamp;
        this.retries = 0;
    }
}
