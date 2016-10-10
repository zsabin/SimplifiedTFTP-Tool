public enum ErrorCode {
    NOT_DEFINED        (0, "Not Defined"),
    FILE_NOT_FOUND     (1, "File Not Found"),
    ILLEGAL_OPERATION  (2, "Illegal Operation");

    private final int errorCode;
    private final String errorMsg;

    ErrorCode(int errorCode, String errorMsg) {
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    public int getValue() {
        return this.errorCode;
    }

    public String toString() {
        return errorMsg;
    }

    public static ErrorCode createErrorcode(int errorCode) {
        switch (errorCode) {
            case 1:
                return FILE_NOT_FOUND;
            case 2:
                return ILLEGAL_OPERATION;
            default:
                return NOT_DEFINED;
        }
    }
}