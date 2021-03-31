package org.cristalise.kernel.common;

public class ObjectNotFoundException extends VertxException {
    private final static int FAILURE_CODE = 105;
    private static final long serialVersionUID = 6881043115092110048L;

    public ObjectNotFoundException() {
        super(FAILURE_CODE);
    }

    public ObjectNotFoundException(Exception e) {
        super(FAILURE_CODE, e);
    }

    public ObjectNotFoundException(String msg) {
        super(FAILURE_CODE, msg);
    }
}
