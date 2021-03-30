package org.cristalise.kernel.common;

public class InvalidDataException extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = -4491884465493921352L;

    public InvalidDataException() {
        super();
    }

    public InvalidDataException(Exception e) {
        super(e);
    }

    public InvalidDataException(String ex) {
        super(ex);
    }

}
