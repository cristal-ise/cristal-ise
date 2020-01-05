package org.cristalise.dsl.persistency.outcome;

/**
 * Store arbitrary number of key/value pair which are used to create element in dynamicForms/additional
 */
public class Additional {
    def fields = [:]

    /**
     * Interceptor method of dynamic groovy to handle missing property exception for setter operations
     * 
     * @param name the name of the property
     * @param value the value to be set for the property
     * @return the previous value associated with property
     */
    public Object propertyMissing(String name, Object value) { 
        return fields[name] = value
    }

    /**
     * Interceptor method of dynamic groovy to handle missing property exception for getter operations
     * 
     * @param name the name of the property
     * @return the value associated with property
     */
    public Object propertyMissing(String name) {
        return fields[name]
    }
}
