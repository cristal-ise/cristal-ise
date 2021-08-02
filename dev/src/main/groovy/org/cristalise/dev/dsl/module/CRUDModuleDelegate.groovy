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
package org.cristalise.dev.dsl.module

import org.codehaus.groovy.control.CompilerConfiguration
import org.cristalise.dev.dsl.utils.ObjectGraphBuilderFactory

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@Slf4j @CompileStatic
class CRUDModuleDelegate {

    public CRUDModuleDelegate(Map<String, Object> args) {
        log.debug 'constructor() - args:{}', args
    }

    public CRUDModule processText(String scriptText) {
        CompilerConfiguration cc = new CompilerConfiguration()
        cc.setScriptBaseClass(DelegatingScript.class.getName())

        GroovyShell shell = new GroovyShell(this.class.classLoader, new Binding(), cc)
        DelegatingScript script = (DelegatingScript) shell.parse(scriptText)

        script.setDelegate(ObjectGraphBuilderFactory.create())
        return (CRUDModule) script.run()
    }

    public CRUDModule processClosure(Closure cl) {
        assert cl

        cl.delegate = ObjectGraphBuilderFactory.create()
        cl.resolveStrategy = Closure.DELEGATE_FIRST

        return (CRUDModule) cl()
    }
}
