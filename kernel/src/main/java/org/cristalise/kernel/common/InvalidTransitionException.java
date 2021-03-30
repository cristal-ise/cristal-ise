package org.cristalise.kernel.common;

public class InvalidTransitionException extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = -6403980570839158164L;

    public InvalidTransitionException() {
        super();
    }

    public InvalidTransitionException(Exception e) {
        super(e);
    }

    public InvalidTransitionException(String ex) {
        super(ex);
    }

}
