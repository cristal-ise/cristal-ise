package org.cristalise.kernel.common;

public class ObjectAlreadyExistsException extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = 5876015808620266247L;

    public ObjectAlreadyExistsException() {
        super();
    }

    public ObjectAlreadyExistsException(Exception e) {
        super(e);
    }

    public ObjectAlreadyExistsException(String ex) {
        super(ex);
    }

}
