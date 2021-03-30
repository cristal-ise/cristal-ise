package org.cristalise.kernel.common;

public class ObjectNotFoundException extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = 6881043115092110048L;

    public ObjectNotFoundException() {
        super();
    }

    public ObjectNotFoundException(Exception e) {
        super(e);
    }

    public ObjectNotFoundException(String ex) {
        super(ex);
    }

}
