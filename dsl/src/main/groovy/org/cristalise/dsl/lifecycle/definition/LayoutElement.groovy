package org.cristalise.dsl.lifecycle.definition

import groovy.transform.CompileStatic

@CompileStatic
class LayoutElement {
    Map<String, Object> properties = [:]

    Integer id
    String name
}
