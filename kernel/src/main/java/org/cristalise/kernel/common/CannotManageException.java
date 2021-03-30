package org.cristalise.kernel.common;

public class CannotManageException extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = 979223649124910315L;

    public CannotManageException() {
        super();
    }

    public CannotManageException(Exception e) {
        super(e);
    }

    public CannotManageException(String ex) {
        super(ex);
    }

}
