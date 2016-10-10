import java.nio.ByteBuffer;

public class ErrorPacket extends TFTPPacket {
    public final static int ERROR_CODE_SIZE = 2;

    private ErrorCode errorCode;
    private String errorMsg;

    public ErrorPacket(int errorCode, String errorMsg) {
        this(ErrorCode.createErrorcode(errorCode), errorMsg);
    }

    public ErrorPacket(ErrorCode errorCode, String errorMsg) {
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;

        byte[] errorMsgAsBytes = stringToBytes(errorCode.toString());
        this.size = OPCODE_SIZE + ERROR_CODE_SIZE + errorMsgAsBytes.length;

        byteBuffer = ByteBuffer.allocate(size);
        byteBuffer.put(valueToBytes(getType().getOpcode(), OPCODE_SIZE));
        byteBuffer.put(valueToBytes(errorCode.getValue(), ERROR_CODE_SIZE));
        byteBuffer.put(errorMsgAsBytes);
    }

    public ErrorCode getErrorCode() {
        return this.errorCode;
    }

    public String getErrorMsg() {
        return this.errorMsg;
    }

    public PacketType getType() {
        return PacketType.ERROR;
    }
}
