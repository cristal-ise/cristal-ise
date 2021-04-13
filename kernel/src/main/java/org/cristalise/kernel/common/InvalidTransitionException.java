package org.cristalise.kernel.common;

public class InvalidTransitionException extends CriseVertxException {
    private final static int FAILURE_CODE = 104;
    private static final long serialVersionUID = -6403980570839158164L;

    public InvalidTransitionException() {
        super(FAILURE_CODE);
    }

    public InvalidTransitionException(Exception e) {
        super(FAILURE_CODE, e);
    }

    public InvalidTransitionException(String msg) {
        super(FAILURE_CODE, msg);
    }
}
