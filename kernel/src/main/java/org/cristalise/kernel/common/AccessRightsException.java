package org.cristalise.kernel.common;

public class AccessRightsException extends CriseVertxException {
    private final static int FAILURE_CODE = 101;
    private static final long serialVersionUID = 5606562389374279530L;

    public AccessRightsException() {
        super(FAILURE_CODE);
    }

    public AccessRightsException(Exception e) {
        super(FAILURE_CODE, e);
    }

    public AccessRightsException(String msg) {
        super(FAILURE_CODE, msg);
    }
}
