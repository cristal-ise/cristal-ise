package org.cristalise.kernel.security;

import lombok.Getter;

@Getter
public enum BuiltInAuthc {

    /**
     * 
     */
    ADMIN_ROLE("Admin"), 

    /**
     * 
     */
    SYSTEM_AGENT("system");
    
    private String name;

    private BuiltInAuthc(final String n) {
        name = n;
    }
}
