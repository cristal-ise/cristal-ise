package org.cristalise.dsl.lifecycle.definition

import groovy.transform.CompileStatic

@CompileStatic
class LayoutElement {
    Map<String, Object> props = [:]

    Integer id // could be auto generated!?
    String name
}
