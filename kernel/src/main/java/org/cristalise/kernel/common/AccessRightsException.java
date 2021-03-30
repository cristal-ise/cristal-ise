package org.cristalise.kernel.common;

public class AccessRightsException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 5606562389374279530L;

    public AccessRightsException() {
        super();
    }

    public AccessRightsException(Exception e) {
        super(e);
    }

    public AccessRightsException(String ex) {
        super(ex);
    }
}
