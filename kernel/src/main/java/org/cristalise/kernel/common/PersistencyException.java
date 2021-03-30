package org.cristalise.kernel.common;

public class PersistencyException extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = 1176559332900341049L;

    public PersistencyException() {
        super();
    }

    public PersistencyException(Exception e) {
        super(e);
    }

    public PersistencyException(String ex) {
        super(ex);
    }
}
