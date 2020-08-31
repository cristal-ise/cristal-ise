package org.cristalise.dsl.lifecycle.definition

import groovy.transform.CompileStatic

@CompileStatic
class LayoutActivity extends LayoutElement {
    public static final List<String> keys = ['name', 'activityReference', 'activityVersion']

    String activityReference
    Integer activityVersion
    
    public void setActivityVersion(String v) {
        activityVersion = v as Integer
    }
}
