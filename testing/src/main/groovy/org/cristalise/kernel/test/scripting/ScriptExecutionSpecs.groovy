package org.cristalise.kernel.test.scripting

import org.cristalise.dsl.scripting.ScriptBuilder
import org.cristalise.kernel.scripting.Script
import org.cristalise.kernel.scripting.ScriptingEngineException
import org.cristalise.kernel.test.utils.CristalTestSetup
import org.cristalise.kernel.utils.CastorHashMap
import org.cristalise.kernel.utils.LocalObjectLoader

import spock.lang.Specification

class ScriptExecutionSpecs extends Specification implements CristalTestSetup {

    def setup()   { inMemoryServer('src/main/bin/inMemoryServer.conf', 'src/main/bin/inMemory.clc', 8) }
    def cleanup() { cristalCleanup() }

    def 'Script can be used without any input or output paramteres'() {
        given:
        ScriptBuilder.create("integTest", "Modulo", 0) {
            groovy { "3 % 2;" }
        }
        Script script = LocalObjectLoader.getScript("Modulo", 0)

        when:
        def properties = new CastorHashMap()
        def result = script.evaluate(null, properties, null, null)

        then:
        result == ""
    }

    def 'Script can use input parameter and can return a value'() {
        given:
        ScriptBuilder.create("integTest", "Modulo", 0) {
            input("counter", "java.lang.Integer")
            output('java.lang.Integer')
            groovy { "counter % 2;" }
        }
        Script script = LocalObjectLoader.getScript("Modulo", 0)

        when:
        def properties = new CastorHashMap()
        properties['counter'] = 3
        def result = script.evaluate(null, properties, null, null)

        then:
        result == 1
        result instanceof java.lang.Integer
    }

    def 'Script assign return value to named parameter for single output and type matched even if Script does not assign the variable'() {
        given:
        ScriptBuilder.create("integTest", "Modulo", 0) {
            input("counter", "java.lang.Integer")
            output('mod', 'java.lang.Integer')
            groovy { "counter % 2;" }
        }
        Script script = LocalObjectLoader.getScript("Modulo", 0)

        when:
        def properties = new CastorHashMap()
        properties['counter'] = 3
        def result = script.evaluate(null, properties, null, null)

        then:
        result instanceof Map
        result.mod == 1
    }

    def 'Type of Script return value must match declared type'() {
        given:
        ScriptBuilder.create("integTest", "Modulo", 0) {
            input("counter", "java.lang.Integer")
            output('java.lang.String')
            groovy { "counter % 2;" }
        }
        Script script = LocalObjectLoader.getScript("Modulo", 0)

        when:
        def properties = new CastorHashMap()
        properties['counter'] = 3
        def result = script.evaluate(null, properties, null, null)

        then:
        thrown ScriptingEngineException
    }

    def 'Script can declare named output value, which returned as a map'() {
        given:
        ScriptBuilder.create("integTest", "Counter", 0) {
            output('count', 'java.lang.Integer')
            groovy { "count = 2" }
        }
        Script script = LocalObjectLoader.getScript("Counter", 0)

        when:
        def properties = new CastorHashMap()
        def result = script.evaluate(null, properties, null, null)

        then:
        result instanceof Map
        result.count == 2
    }

    def 'Script can declare more than one named outputs, which returned as a map'() {
        given:
        ScriptBuilder.create("integTest", "Counter", 0) {
            output('label', 'java.lang.String')
            output('count', 'java.lang.Integer')
            groovy { "label = 'toto'; count = 2" }
        }
        Script script = LocalObjectLoader.getScript("Counter", 0)

        when:
        def properties = new CastorHashMap()
        def result = script.evaluate(null, properties, null, null)

        then:
        result instanceof Map
        result.label == 'toto'
        result.count == 2
    }

    def 'When having more then one output all of them has to be named'() {
        given:
        ScriptBuilder.create("integTest", "Counter", 0) {
            output('label', 'java.lang.String')
            output('java.lang.Integer')
            groovy { "label = 'toto'; return 2" }
        }
        Script script = LocalObjectLoader.getScript("Counter", 0)

        when:
        def properties = new CastorHashMap()
        def result = script.evaluate(null, properties, null, null)

        then:
        thrown ScriptingEngineException
    }

    def 'Script can use output values from included Script as inputs'() {
        given:
        ScriptBuilder.create("integTest", "Counter", 0) {
            output('label', 'java.lang.String')
            output('count', 'java.lang.Integer')
            groovy { "label = 'toto'; count = 2" }
        }
        ScriptBuilder.create("integTest", "Modulo", 0) {
            include("Counter", 0)
            input("count", "java.lang.Integer")
            output('java.lang.Integer')
            groovy { "count % 2;" }
        }
        Script script = LocalObjectLoader.getScript("Modulo", 0)

        when:
        def properties = new CastorHashMap()
        def result = script.evaluate(null, properties, null, null)

        then:
        result == 0
    }

    def 'Script written in javascript can use function from included Script'() {
        given:
        ScriptBuilder.create("integTest", "Function", 0) {
            javascript { "function func() { return 2; }" }
        }
        ScriptBuilder.create("integTest", "Modulo", 0) {
            include("Function", 0)
            output('java.lang.Double')
            javascript { "func() % 2;" }
        }
        Script script = LocalObjectLoader.getScript("Modulo", 0)

        when:
        def properties = new CastorHashMap()
        def result = script.evaluate(null, properties, null, null)

        then:
        result == 0
    }

    def 'Script written in groovy can use function from included Script'() {
        given:
        ScriptBuilder.create("integTest", "Function", 0) {
            groovy { "def func() { 2 }" }
        }
        ScriptBuilder.create("integTest", "Modulo", 0) {
            include("Function", 0)
            output('java.lang.Integer')
            groovy { "func() % 2;" }
        }
        Script script = LocalObjectLoader.getScript("Modulo", 0)

        when:
        def properties = new CastorHashMap()
        def result = script.evaluate(null, properties, null, null)

        then:
        result == 0
    }
}
