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
