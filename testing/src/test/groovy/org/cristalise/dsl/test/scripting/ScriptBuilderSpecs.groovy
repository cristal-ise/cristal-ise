/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.dsl.test.scripting

import org.cristalise.test.CristalTestSetup;

import spock.lang.Specification


/**
 *
 *
 */
class ScriptBuilderSpecs extends Specification implements CristalTestSetup {
    
    ScriptTestBuilder builder = null
    
    def setup() {
        crSetup()
        builder = new ScriptTestBuilder()
    }
    
    def cleanup() {
        crCleanup()
    }

    def 'Specifying new script'() {
        expect:
        builder.build {
            name = "MyFirstScript"
            version = 0
            input("input1", "java.lang.String")
            output("org.cristalise.kernel.scripting.ErrorInfo")
            javascript { ";" }

//            output(name: "errors", type: "org.cristalise.kernel.scripting.ErrorInfo")
//            script('javascript') { ";" }
//            script(language: 'javascript') { ";" }
        }
        builder == """<cristalscript>
                          <param name='input1' type='java.lang.String' />
                          <output type='org.cristalise.kernel.scripting.ErrorInfo' />
                          <script language='javascript' name=''><![CDATA[ ; ]]></script>
                      </cristalscript>"""
    }
}
