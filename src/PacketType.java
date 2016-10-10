public enum PacketType {
    UNDEF   (0, "Undefined"),
    RRQ     (1, "Read Request"),
    WRQ     (2, "Write Request"),
    DATA    (3, "Data Packet"),
    ACK     (4, "Acknowledgement"),
    ERROR   (5, "Error Packet");

    private final int opcode;
    private final String description;

    PacketType(int opcode, String description) {
        this.opcode = opcode;
        this.description = description;
    }

    public int getOpcode() {
        return this.opcode;
    }

    public String toString() {
        return this.description;
    }

    public static PacketType createTFTPPacketType(int opcode) {
        switch (opcode) {
            case 1:
                return RRQ;
            case 2:
                return WRQ;
            case 3:
                return DATA;
            case 4:
                return ACK;
            case 5:
                return ERROR;
            default:
                return UNDEF;
        }
    }
}
