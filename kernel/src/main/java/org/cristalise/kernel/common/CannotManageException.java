package org.cristalise.kernel.common;

public class CannotManageException extends CriseVertxException {
    private final static int FAILURE_CODE = 102;
    private static final long serialVersionUID = 979223649124910315L;

    public CannotManageException() {
        super(FAILURE_CODE);
    }

    public CannotManageException(Exception e) {
        super(FAILURE_CODE, e);
    }

    public CannotManageException(String msg) {
        super(FAILURE_CODE, msg);
    }

}
