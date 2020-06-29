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

import org.xml.sax.SAXException
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.diff.DefaultNodeMatcher
import org.xmlunit.diff.Diff
import org.xmlunit.diff.Difference
import org.xmlunit.diff.ElementSelectors

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder

@Slf4j
class KernelXMLUtility {
    /**
     * 
     * @param params
     * @return
     * @deprecated use marshalled ImportRole
     */
    @Deprecated
    public static String getRoleXML(params) {
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)

        writer << '<?xml version="1.0" encoding="UTF-8"?>\n'

        assert params.name, "name must be set"

        if(!params.jobList) { params.jobList = 'false'}

        xml.Role(name:params.name, jobList:params.jobList);

        return writer.toString()
    }


    /**
     * 
     * @param params
     * @return
     * @deprecated use marshalled ImportAgent
     */
    @Deprecated
    public static String getAgentXML(params) {
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)

        //TODO: Agent can have many roles - IMPLEMENT

        writer << '<?xml version="1.0" encoding="UTF-8"?>\n'

        assert params.name, "name must be set"
        assert params.password, "password must be set"
        assert params.Role, "Role must be set"

        xml.Agent(name:"$params.name", password:"$params.password") {
            Role(name: params.Role)
            Property(name:"Name", "$params.name")
            Property(name:"Type", 'Agent')
        }

        return writer.toString()
    }


    /**
     * 
     * 
     * @param params
     * @return
     * @deprecated use marshalled ImportItem
     */
    @Deprecated
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

    public static String getDescObjectDetailsXML(params) {
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)

        writer << '<?xml version="1.0" encoding="UTF-8"?>\n'
        
        xml.DescObjectDetails {
            ObjectID("$params.id")
            Version("$params.version")
        }
        
        return writer.toString()
    }

    /**
     *
     * @param params
     * @return the XML string
     */
    public static String getActivityDefXML(params) {
        assert params.Name,       "Name must be set"
//        assert params.AgentRole,  "AgentRole must be set"
//        assert params.SchemaType, "SchemaType must be set"

        if(!params.Name)          { params.Name          = '' }
        if(!params.ID)            { params.ID            = '0' }
        if(!params.Height)        { params.Height        = '0' }
        if(!params.Width)         { params.Width         = '0' }
        if(!params.IsLayoutable)  { params.IsLayoutable  = 'false'}

        if(!params.AgentName)     { params.AgentName     = '' }
        if(!params.AgentRole)     { params.AgentRole     = '' }
        if(!params.MailMessage)   { params.MailMessage   = '' }
        if(!params.Mailevent)     { params.Mailevent     = '' }
        if(!params.Description)   { params.Description   = '' }
        if(!params.SchemaType)    { params.SchemaType    = '' }
        if(!params.SchemaVersion) { params.SchemaVersion = '0' }
        if(!params.Showtime)      { params.Showtime      = 'false' }
        if(!params.Viewpoint)     { params.Viewpoint     = '' }
        if(!params.ScriptName)    { params.ScriptName    = '' }
        if(!params.ScriptVersion) { params.ScriptVersion = '0' }
        if(!params.Ignorable)     { params.Ignorable     = 'false' }
        if(!params.Skippable)     { params.Skippable     = 'false' }
        if(!params.Repeatable)    { params.Repeatable    = 'false' }
        if(!params.Autostart)     { params.Autostart     = 'false' }

        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)

        writer << '<?xml version="1.0" encoding="UTF-8"?>\n'

        xml.ActivityDef( ID:"1", Name: "$params.Name", Height:"$params.Height", Width:"$params.Width",
                         IsLayoutable:"$params.IsLayoutable", IsComposite:"false") {
            Properties {
                KeyValuePair(Key:"Agent Name",    String :"$params.AgentName")
                KeyValuePair(Key:"Agent Role",    String :"$params.AgentRole")
                KeyValuePair(Key:"Mail Message",  String :"$params.MailMessage")
                KeyValuePair(Key:"Mail event",    String :"$params.Mailevent")
                KeyValuePair(Key:"Description",   String :"$params.Description")
                KeyValuePair(Key:"SchemaType",    String :"$params.SchemaType")
                KeyValuePair(Key:"SchemaVersion", String :"$params.SchemaVersion")
                KeyValuePair(Key:"Show time",     Boolean:"$params.Showtime")
                KeyValuePair(Key:"Viewpoint",     String :"$params.Viewpoint")
                KeyValuePair(Key:"ScriptName",    String :"$params.ScriptName")
                KeyValuePair(Key:"ScriptVersion", String :"$params.ScriptVersion")
                KeyValuePair(Key:"Ignorable",     Boolean:"$params.Ignorable")
                KeyValuePair(Key:"Skippable",     Boolean:"$params.Skippable")
                KeyValuePair(Key:"Repeatable",    Boolean:"$params.Repeatable")
                KeyValuePair(Key:"Autostart",     Boolean:"$params.Autostart")
            }
        }

        return writer.toString()
    }

    public static String getCompositeActivityDefXML(params) {
        assert params.Name,            "Name must be set"
        assert params.ActivityName,    "ActivityName must be set"
        assert params.ActivityVersion != null, "ActivityVersion must be set"

        if(!params.Height)        { params.Height      = '0' }
        if(!params.Width)         { params.Width       = '0' }
        if(!params.AgentName)     { params.AgentName   = '' }
        if(!params.AgentRole)     { params.AgentRole   = '' }
        if(!params.Description)   { params.Description = '' }
        if(!params.OutcomeInit)   { params.OutcomeInit = '' }
        if(!params.Viewpoint)     { params.Viewpoint   = '' }

        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)

        writer << '<?xml version="1.0" encoding="UTF-8"?>\n'

        xml.CompositeActivityDef( ID:"-1", Name: "$params.Name", Height:"$params.Height", Width:"$params.Width", IsLayoutable:"true", IsComposite:"true") {
            childrenGraphModel {
                GraphModelCastorData(NextId:"2", StartVertexId:"1") {
                    ActivitySlotDef(ID:"1", Name:"$params.ActivityName slot", IsComposite:"false", IsLayoutable:"true", Width:"130", Height:"60") {
                        CentrePoint(  x:"247", y:"142")
                        OutlinePoint( x:"182", y:"112")
                        OutlinePoint( x:"312", y:"112")
                        OutlinePoint( x:"312", y:"172")
                        OutlinePoint( x:"182", y:"172")
                        Properties {
                            KeyValuePair(Key:"Name",    String: "$params.ActivityName",    isAbstract:"false")
                            KeyValuePair(Key:"Version", Integer:"$params.ActivityVersion", isAbstract:"false")
                        }
                        activityDef("$params.ActivityName")
                    }
                }
            }
            Properties {
                KeyValuePair(Key:"Agent Name",    String :"$params.AgentName")
                KeyValuePair(Key:"Agent Role",    String :"$params.AgentRole")
                KeyValuePair(Key:"Description",   String :"$params.Description")
                KeyValuePair(Key:"OutcomeInit",   String :"$params.OutcomeInit")
                KeyValuePair(Key:"Viewpoint",     String :"$params.Viewpoint")
            }
        }
        return writer.toString()
    }

    /**
     * Compares 2 XML string
     *
     * @param expected the reference XML
     * @param actual the xml under test
     * @return whether the two XMLs are identical or not
     * @throws SAXException
     * @throws IOException
     */
    @CompileStatic
    public static boolean compareXML(String expected, String actual) throws SAXException, IOException {
        Diff diff = DiffBuilder.compare(expected).withTest(actual)
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndAllAttributes))
                .ignoreComments()
                .ignoreWhitespace()
                .checkForSimilar()
                .build();

        if(diff.hasDifferences()) {
            log.warn("Actual:\n $actual");
            log.warn("Expected:\n $expected");

            Iterator<Difference> allDiffs = diff.getDifferences().iterator();
            int i = 1;

            while (allDiffs.hasNext()) { log.warn("#" + i++ + ":" + allDiffs.next().toString()); }
        }

        return !diff.hasDifferences();
    }
}
