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
package org.cristalise.kernel.test.utils;

import groovy.xml.MarkupBuilder

import org.cristalise.kernel.utils.Logger
import org.custommonkey.xmlunit.DetailedDiff
import org.custommonkey.xmlunit.XMLUnit



/**
 * 
 * @author kovax
 *
 */
class KernelXMLUtility {

    /**
     * 
     * @param params
     * @return
     */
    public static String getRoleXML(params) {
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)
		
        writer << '<?xml version="1.0" encoding="UTF-8"?>\n'

        assert params.name, "name must be set"

        if(!params.jobList) { params.jobList = 'false'}

        xml.Role(jobList:params.jobList, params.name);

        return writer.toString()
    }


    /**
     * 
     * @param params
     * @return
     */
    public static String getAgentXML(params) {
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)
		
		//TODO: Agent can have many roles - IMPLEMENT

        writer << '<?xml version="1.0" encoding="UTF-8"?>\n'

        assert params.name, "name must be set"
        assert params.password, "password must be set"
        assert params.Role, "Role must be set"

        xml.Agent(name:"$params.name", password:"$params.password") {
            Role(params.Role)
            Property(name:"Name", "$params.name")
            Property(name:"Type", 'Agent')
        }

        return writer.toString()
    }


    /**
     * 
     * @param params
     * @return
     */
    public static String getItemXML(params) {
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)

        writer << '<?xml version="1.0" encoding="UTF-8"?>\n'

        assert params.name, "name must be set"
        assert params.workflow, "workflow must be set"
        assert params.initialPath, "initialPath must be set"

        if(!params.schema)   { params.schema   = '' }
        if(!params.version)  { params.version  = '' }
        if(!params.viewname) { params.viewname = '' }

        xml.Item(name:"$params.name", workflow:"$params.workflow", initialPath:"$params.initialPath") {
            Property(name:"Name", mutable:"true",  "$params.name")

            if(params.type) Property(name:"Type", mutable:"false", "$params.type")

            if(params.schema) Outcome(schema:"$params.schema", version:"$params.version", viewname:"$params.viewname")
        }

        return writer.toString()
    }

    /**
     * 
     * @param expected
     * @param actual
     * @return
     */
    public static boolean compareXML(String expected, String actual) {
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setIgnoreComments(true);

        DetailedDiff diff = new DetailedDiff( XMLUnit.compareXML( expected, actual) );

        if(!diff.identical()) Logger.error(diff.toString())

        return diff.identical();
    }

}
