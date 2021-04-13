package org.cristalise.kernel.common;

public class PersistencyException extends CriseVertxException {
    private final static int FAILURE_CODE = 105;
    private static final long serialVersionUID = 1176559332900341049L;

    public PersistencyException() {
        super(FAILURE_CODE);
    }

    public PersistencyException(Exception e) {
        super(FAILURE_CODE, e);
    }

    public PersistencyException(String msg) {
        super(FAILURE_CODE, msg);
    }
}
