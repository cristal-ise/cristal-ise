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
package org.cristalise.dev.test.utils

import groovy.xml.MarkupBuilder



/**
 * 
 * @author kovax
 *
 */
class DevXMLUtility {

    /**
     * 
     * @param params
     * @return the XML string
     */
    public static String getNewDevObjectDefXML(params) {
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)

        writer << '<?xml version="1.0" encoding="UTF-8"?>\n'

        assert params.name, "name must be set"
        assert params.folder, "folder must be set"

        if(!params.name)   { params.name   = '' }
        if(!params.folder) { params.folder = '' }

        xml.NewDevObjectDef {
            ObjectName("$params.name")
            SubFolder("$params.folder")
        }

        return writer.toString()
    }


    /**
     * 
     * @param params
     * @return the XML string
     */
    public static String getActivityDefXML(params) {
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)

        writer << '<?xml version="1.0" encoding="UTF-8"?>\n'

        assert params.Name,       "Name must be set"
        assert params.AgentRole,  "AgentRole must be set"
        assert params.SchemaType, "SchemaType must be set"

        if(!params.Name)          { params.Name          = '' }
        if(!params.ID)            { params.ID            = '0' }
        if(!params.Height)        { params.Height        = '0' }
        if(!params.Width)         { params.Width         = '0' }
        if(!params.IsLayoutable)  { params.IsLayoutable  = 'false'}
        if(!params.IsComposite)   { params.IsComposite   = 'false' }

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

        xml.ActivityDef( ID:"1", Name: "$params.Name", Height:"$params.Height", Width:"$params.Width", 
                         IsLayoutable:"$params.IsLayoutable", IsComposite:"$params.IsComposite") {
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
}
