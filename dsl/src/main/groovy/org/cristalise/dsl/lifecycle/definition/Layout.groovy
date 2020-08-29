package org.cristalise.dsl.lifecycle.definition

import groovy.transform.CompileStatic

/**
 * This class is used to build a Composite Activity layout using DSL/Excel
 */
@CompileStatic
class Layout {
    List<LayoutElement> children = []
}
