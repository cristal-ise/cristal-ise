package org.cristalise.kernel.common;

public class ObjectCannotBeUpdated extends CriseVertxException {
    private final static int FAILURE_CODE = 105;
    private static final long serialVersionUID = 8957655670807468315L;

    public ObjectCannotBeUpdated() {
        super(FAILURE_CODE);
    }

    public ObjectCannotBeUpdated(Exception e) {
        super(FAILURE_CODE, e);
    }

    public ObjectCannotBeUpdated(String msg) {
        super(FAILURE_CODE, msg);
    }

}
