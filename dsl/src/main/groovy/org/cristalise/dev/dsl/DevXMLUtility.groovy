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
package org.cristalise.dev.dsl

import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.persistency.outcome.Outcome
import org.cristalise.kernel.utils.LocalObjectLoader

import groovy.xml.MarkupBuilder



/**
 * Utility class to create XMLs required to build 'dev' module Items
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
     * Converts a Map to an XML using dynamic groovy. Support arbitrary level of nested structures 
     * through recursive algorithm. It also supports repeating elements specified as List.
     * 
     * @param root the name of the root element
     * @param record the Map to be converted
     * @return the XML string
     */
    public static String recordToXML(String root, Map record) {
        return recordToXML([root: record])
    }

    /**
     * Converts a Map to an XML using dynamic groovy. Support arbitrary level of nested structures 
     * through recursive algorithm. It also supports repeating elements specified as List.
     * 
     * @param record the Map to be converted
     * @return the XML string
     */
    public static String recordToXML(Map record) {
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)

        recordToXML(xml, record)

        return writer.toString()
    }

    /**
     * Converts a Map to an XML using dynamic groovy. Support arbitrary level of nested structures 
     * through recursive algorithm. It also supports repeating elements specified as List.
     * 
     * @param xml the initialised MarkupBuilder to be used to build the XML
     * @param record the Map to be converted
     */
    public static void recordToXML(MarkupBuilder xml, Map record) {
        record.each { key, value ->
            switch (value) {
                case Map:
                    xml."$key" { recordToXML(xml, value) }
                    break;

                case List:
                    value.each { listValue ->
                        xml."$key" {
                            switch (listValue) {
                                case Map: recordToXML(xml, listValue); break;
                                default:  mkp.yield(listValue); break;
                            }
                        }
                    }
                    break;

                default:
                    xml."$key"(value)
                    break;
            }
        }
    }
}
