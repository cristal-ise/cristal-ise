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
package org.cristalise.dsl.scripting

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.lookup.InvalidPathException
import org.cristalise.kernel.persistency.outcome.OutcomeValidator
import org.cristalise.kernel.persistency.outcome.Schema
import org.cristalise.kernel.utils.LocalObjectLoader

import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource

import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.persistency.outcome.Outcome
import org.cristalise.kernel.process.Bootstrap
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.scripting.Script

import javax.xml.validation.SchemaFactory


/**
 *
 */
@CompileStatic @Slf4j
class ScriptBuilder {
    String name = ""
    String module = ""
    int version = -1

    static Schema scriptSchema = LocalObjectLoader.getSchema("Script", 0)

    Script script = null
    String scriptXML = null

    DomainPath domainPath = null

    public ScriptBuilder() {}

    /**
     * 
     * @param module
     * @param name
     * @param version
     */
    public ScriptBuilder(String module, String name, int version) {
        this.module  = module
        this.name    = name
        this.version = version
    }

    /**
     * 
     * @param xml
     * @throws InvalidDataException
     */
    public void validateScriptXML(String xml) throws InvalidDataException {
        OutcomeValidator validator = new OutcomeValidator(scriptSchema)
        def error = validator.validate(xml)

        if (!error) {
            log.debug "validateScriptXML() - DONE"
        }
        else {
            log.error("ScriptBuilder.validateScriptXML() - $error")
            log.error("\n============== XML ==============\n" + xml + "\n=================================\n");
            throw new InvalidPathException(error)
        }
    }

    /**
     * Factory method to build a Script object and create the ResourceItem for it 
     * 
     * @param module the name of the module the Script instance belongs to
     * @param name the name of the Script
     * @param version the version of the Script
     * @param cl the closure to build the Script
     * @return the ScriptBuilder instance full configured
     */
    public static ScriptBuilder create(String module, String name, int version, Closure cl) {
        def sb = build(module, name, version, cl)
        sb.create()
        return sb
    }

    public static Script build(String name, int version, Closure cl) {
        // FIXME: build method should return Script instead of ScriptBuilder
        return build("", name, version, cl).script
    }

    /**
     * Factory method to build a Script object
     * 
     * @param module the name of the module the Script belongs to
     * @param name the name of the Script
     * @param version the version of the Script
     * @param cl the closure to build the Script
     * @return the ScriptBuilder instance full configured
     */
    public static ScriptBuilder build(String module, String name, int version, Closure cl) {
        def sb = new ScriptBuilder(module, name, version)

        def scriptD = new ScriptDelegate(module, name, version)
        scriptD.processClosure(cl)

        //delegate's processClosure() can set these members, so copying the latest values
        sb.module  = scriptD.module
        sb.name    = scriptD.name
        sb.version = scriptD.version

        sb.scriptXML = scriptD.writer.toString()

        log.debug "build() - Generated xml:\n $sb.scriptXML"

        sb.validateScriptXML(sb.scriptXML)

        sb.script = new Script(name, version, (ItemPath)null, sb.scriptXML)
        return sb
    }

    /**
     * Bootstrap method to create the ResourceItem from a fully configured ScriptBuilder
     *  
     * @return the DomainPath of the newly created resource Item
     */
    public DomainPath create() {
        return domainPath = Bootstrap.createResource(module, name, version, "SC", [new Outcome(-1, scriptXML, scriptSchema)] as Set, false)
    }
}
