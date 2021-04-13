package org.cristalise.kernel.common;

public class InvalidCollectionModification extends CriseVertxException {
    private final static int FAILURE_CODE = 103;
    private static final long serialVersionUID = -8108958427700141393L;

    public InvalidCollectionModification() {
        super(FAILURE_CODE);
    }

    public InvalidCollectionModification(Exception e) {
        super(FAILURE_CODE, e);
    }

    public InvalidCollectionModification(String msg) {
        super(FAILURE_CODE, msg);
    }
}
