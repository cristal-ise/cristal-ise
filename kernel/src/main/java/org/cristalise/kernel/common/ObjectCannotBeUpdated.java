package org.cristalise.kernel.common;

public class ObjectCannotBeUpdated extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = 8957655670807468315L;

    public ObjectCannotBeUpdated() {
        super();
    }

    public ObjectCannotBeUpdated(Exception e) {
        super(e);
    }

    public ObjectCannotBeUpdated(String ex) {
        super(ex);
    }

}
