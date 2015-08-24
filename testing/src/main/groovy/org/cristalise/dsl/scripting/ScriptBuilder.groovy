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

import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory

import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.scripting.Script
import org.cristalise.kernel.utils.Logger


/**
 *
 */
@CompileStatic
class ScriptBuilder {
    Script script = null
    String scriptXML = null

    Schema schema

    public ScriptBuilder() {
        String xsd = Gateway.getResource().getTextResource(null, "boot/OD/Script.xsd")
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        schema = factory.newSchema(new StreamSource(new StringReader(xsd)))
    }

    public void validateScriptXML(String xml) throws Exception {
        try {
            schema.newValidator().validate(new StreamSource(new StringReader(xml)));

            Logger.debug(5, "ScriptBuilder.validateScriptXML() - DONE");
        }
        catch (Exception e) {
            Logger.error(e);
            Logger.error("\n============== XML ==============\n" + xml + "\n=================================\n");
            throw e;
        }
    }

    public Script build(Closure cl) {
        def sDelegate = new ScriptDelegate()
        sDelegate.processClosure(cl)

        scriptXML = sDelegate.writer.toString()

        Logger.debug(5, "ScriptBuilder.build() - Generated xml: $scriptXML");

        validateScriptXML(scriptXML)

        script = new Script(sDelegate.name, sDelegate.version, scriptXML)
        return script
    }
}
