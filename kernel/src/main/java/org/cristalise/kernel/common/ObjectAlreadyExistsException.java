package org.cristalise.kernel.common;

public class ObjectAlreadyExistsException extends VertxException {
    private final static int FAILURE_CODE = 105;
    private static final long serialVersionUID = 5876015808620266247L;

    public ObjectAlreadyExistsException() {
        super(FAILURE_CODE);
    }

    public ObjectAlreadyExistsException(Exception e) {
        super(FAILURE_CODE, e);
    }

    public ObjectAlreadyExistsException(String msg) {
        super(FAILURE_CODE, msg);
    }
}
