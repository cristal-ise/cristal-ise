package org.cristalise.kernel.common;

public class InvalidCollectionModification extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = -8108958427700141393L;

    public InvalidCollectionModification() {
        super();
    }

    public InvalidCollectionModification(Exception e) {
        super(e);
    }

    public InvalidCollectionModification(String ex) {
        super(ex);
    }

}
