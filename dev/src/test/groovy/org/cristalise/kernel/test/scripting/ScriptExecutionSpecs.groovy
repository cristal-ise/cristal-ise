/**
 * This file is part of the CRISTAL-iSE Development Module.
 * Copyright (c) 2001-2017 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.kernel.test.scripting

import org.cristalise.dsl.scripting.ScriptBuilder
import org.cristalise.kernel.scripting.Script
import org.cristalise.kernel.scripting.ScriptingEngineException
import org.cristalise.kernel.test.utils.CristalTestSetup
import org.cristalise.kernel.utils.CastorHashMap
import org.cristalise.kernel.utils.LocalObjectLoader

import spock.lang.Ignore
import spock.lang.Specification

class ScriptExecutionSpecs extends Specification implements CristalTestSetup {

    def setupSpec()   { inMemoryServer(null, true) }
    def cleanupSpec() { cristalCleanup() }

    def 'Script can be used without any input or output paramteres'() {
        given:
        def sb = ScriptBuilder.create("integTest", "Modulo01", 0) {
            groovy { "3 % 2;" }
        }
        Script script = LocalObjectLoader.getScript("Modulo01", 0)

        when:
        def properties = new CastorHashMap()
        def result = script.evaluate(null, properties, null, null)

        then:
        sb.domainPath.itemPath.stringPath == script.itemPath.stringPath
        result == ""
    }

    def 'Script can use input parameter and can return a value'() {
        given:
        def sb = ScriptBuilder.create("integTest", "Modulo02", 0) {
            input("counter", "java.lang.Integer")
            output('java.lang.Integer')
            groovy { "counter % 2;" }
        }
        Script script = LocalObjectLoader.getScript("Modulo02", 0)

        when:
        def properties = new CastorHashMap()
        properties['counter'] = 3
        def result = script.evaluate(null, properties, null, null)

        then:
        sb.domainPath.itemPath.stringPath == script.itemPath.stringPath
        result == 1
        result instanceof java.lang.Integer
    }

    def 'Script assign return value to named parameter for single output and type matched even if Script does not assign the variable'() {
        given:
        def sb = ScriptBuilder.create("integTest", "Modulo03", 0) {
            input("counter", "java.lang.Integer")
            output('mod', 'java.lang.Integer')
            groovy { "counter % 2;" }
        }
        Script script = LocalObjectLoader.getScript("Modulo03", 0)

        when:
        def properties = new CastorHashMap()
        properties['counter'] = 3
        def result = script.evaluate(null, properties, null, null)

        then:
        sb.domainPath.itemPath.stringPath == script.itemPath.stringPath
        result instanceof Map
        result.mod == 1
    }

    def 'Type of Script return value must match declared type'() {
        given:
        def sb = ScriptBuilder.create("integTest", "Modulo04", 0) {
            input("counter", "java.lang.Integer")
            output('java.lang.String')
            groovy { "counter % 2;" }
        }
        Script script = LocalObjectLoader.getScript("Modulo04", 0)

        when:
        def properties = new CastorHashMap()
        properties['counter'] = 3
        def result = script.evaluate(null, properties, null, null)

        then:
        sb.domainPath.itemPath.stringPath == script.itemPath.stringPath
        thrown ScriptingEngineException
    }

    def 'Script can declare named output value, which returned as a map'() {
        given:
        def sb = ScriptBuilder.create("integTest", "Counter05", 0) {
            output('count', 'java.lang.Integer')
            groovy { "count = 2" }
        }
        Script script = LocalObjectLoader.getScript("Counter05", 0)

        when:
        def properties = new CastorHashMap()
        def result = script.evaluate(null, properties, null, null)

        then:
        sb.domainPath.itemPath.stringPath == script.itemPath.stringPath
        result instanceof Map
        result.count == 2
    }

    def 'Script can declare more than one named outputs, which returned as a map'() {
        given:
        def sb = ScriptBuilder.create("integTest", "Counter06", 0) {
            output('label', 'java.lang.String')
            output('count', 'java.lang.Integer')
            groovy { "label = 'toto'; count = 2" }
        }
        Script script = LocalObjectLoader.getScript("Counter06", 0)

        when:
        def properties = new CastorHashMap()
        def result = script.evaluate(null, properties, null, null)

        then:
        sb.domainPath.itemPath.stringPath == script.itemPath.stringPath
        result instanceof Map
        result.label == 'toto'
        result.count == 2
    }

    def 'When having more then one output all of them has to be named'() {
        given:
        def sb = ScriptBuilder.create("integTest", "Counter07", 0) {
            output('label', 'java.lang.String')
            output('java.lang.Integer')
            groovy { "label = 'toto'; return 2" }
        }
        Script script = LocalObjectLoader.getScript("Counter07", 0)

        when:
        def properties = new CastorHashMap()
        def result = script.evaluate(null, properties, null, null)

        then:
        sb.domainPath.itemPath.stringPath == script.itemPath.stringPath
        thrown ScriptingEngineException
    }

    def 'Script can use output values from included Script as inputs'() {
        given:
        ScriptBuilder.create("integTest", "Counter08", 0) {
            output('label', 'java.lang.String')
            output('count', 'java.lang.Integer')
            groovy { "label = 'toto'; count = 2" }
        }
        def sb = ScriptBuilder.create("integTest", "Modulo08", 0) {
            include("Counter08", 0)
            input("count", "java.lang.Integer")
            output('java.lang.Integer')
            groovy { "count % 2;" }
        }
        Script script = LocalObjectLoader.getScript("Modulo08", 0)

        when:
        def properties = new CastorHashMap()
        def result = script.evaluate(null, properties, null, null)

        then:
        sb.domainPath.itemPath.stringPath == script.itemPath.stringPath
        result == 0
    }

    @Ignore('Include does not work with javascript, check issue #650')
    def 'Script written in javascript can use function from included Script'() {
        given:
        ScriptBuilder.create("integTest", "Function09", 0) {
            javascript { "function func() { return 2; }" }
        }
        def sb = ScriptBuilder.create("integTest", "Modulo09", 0) {
            include("Function09", 0)
            output('java.lang.Double')
            javascript { "func() % 2;" }
        }
        Script script = LocalObjectLoader.getScript("Modulo09", 0)

        when:
        def properties = new CastorHashMap()
        def result = script.evaluate(null, properties, null, null)

        then:
        sb.domainPath.itemPath.stringPath == script.itemPath.stringPath
        result == 0
    }

    @Ignore('Include does not work with groovy, check issue #650')
    def 'Script written in groovy can use function from included Script'() {
        given:
        ScriptBuilder.create("integTest", "Function10", 0) {
            groovy { "def func() { 2 }" }
        }
        def sb = ScriptBuilder.create("integTest", "Modulo10", 0) {
            include("Function10", 0)
            output('java.lang.Integer')
            groovy { "func() % 2;" }
        }
        Script script = LocalObjectLoader.getScript("Modulo10", 0)

        when:
        def properties = new CastorHashMap()
        def result = script.evaluate(null, properties, null, null)

        then:
        sb.domainPath.itemPath.stringPath == script.itemPath.stringPath
        result == 0
    }
}
