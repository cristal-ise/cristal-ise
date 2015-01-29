package org.cristalise.kernel.test.utils;

import groovy.xml.MarkupBuilder



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

        if(!params.schema)   { params.schema   = ''}
        if(!params.version)  { params.version  = ''}
        if(!params.viewname) { params.viewname = ''}

        xml.Item(name:"$params.name", workflow:"$params.workflow", initialPath:"$params.initialPath") {
            Property(name:"Name", mutable:"true",  "$params.name")

            if(params.type) Property(name:"Type", mutable:"false", "$params.type")

            if(params.schema) Outcome(schema:"$params.schema", version:"$params.version", viewname:"$params.viewname")
        }

        return writer.toString()
    }
}
