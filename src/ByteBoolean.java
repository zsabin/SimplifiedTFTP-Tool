public enum ByteBoolean {
    BYTE_FALSE(false, (byte)0),
    BYTE_TRUE(true, (byte)1);

    public static final int sizeInBytes = 1;

    private boolean boolValue;
    private byte byteValue;

    ByteBoolean(boolean boolValue, byte byteValue) {
        this.boolValue = boolValue;
        this.byteValue = byteValue;
    }

    public boolean getBoolValue() {
        return this.boolValue;
    }

    public byte getByteValue() {
        return this.byteValue;
    }

    public static ByteBoolean createByteBoolean(boolean boolValue) {
        if (BYTE_TRUE.getBoolValue()) {
            return BYTE_TRUE;
        }
        return BYTE_FALSE;
    }

    public static ByteBoolean createByteBoolean(byte byteValue) {
        if (byteValue == BYTE_TRUE.getByteValue()) {
            return BYTE_TRUE;
        }
        return BYTE_FALSE;
    }

}
