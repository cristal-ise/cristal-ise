package org.cristalise.kernel.common;

public class InvalidDataException extends VertxException {
    private final static int FAILURE_CODE = 103;
    private static final long serialVersionUID = -4491884465493921352L;

    public InvalidDataException() {
        super(FAILURE_CODE);
    }

    public InvalidDataException(Exception e) {
        super(FAILURE_CODE, e);
    }

    public InvalidDataException(String msg) {
        super(FAILURE_CODE, msg);
    }

}
