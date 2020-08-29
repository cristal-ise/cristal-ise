package org.cristalise.dsl.lifecycle.definition

import groovy.transform.CompileStatic

@CompileStatic
class LayoutActivity extends LayoutElement {

    def Schema
    def Script
    def Query
    def StateMachine
}
