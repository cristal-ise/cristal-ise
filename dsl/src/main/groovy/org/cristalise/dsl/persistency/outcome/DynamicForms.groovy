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
package org.cristalise.dsl.persistency.outcome

import java.util.regex.Pattern

import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.querying.Query
import org.cristalise.kernel.scripting.Script
import groovy.transform.CompileStatic;

@CompileStatic
class DynamicForms {
    String accept = null
    Boolean disabled = null
    String errmsg = null
    Boolean hidden = null
    String inputType = null
    String label = null
    String mask = null
    Integer max = null
    Integer min = null
    Boolean multiple = null
	String autoComplete = null
    String pattern = null
    String placeholder = null
    Boolean required = null
    Boolean showSeconds = null
    Boolean hideOnDateTimeSelect = null
    String type = null
    String value = null

    /**
     * List all fields that will be updated once the current field is updated.
     */
    List<String> updateFields = null
    
    /**
     * Sets the grid properties of the base form or of the field
     */
    String container = null
    String control = null
    String labelGrid = null
    
    /**
     * Provided validation rule and message to set a warning on the given field
     */
    Warning warning = null

    /**
     * Sets the width of the form
     */
    String width = null

    /**
     * 
     */
    Additional additional = null

    /**
     * Defines the Script name and version (e.g. GetShiftNames:0) which is executed when
     * the from generated from the XML Schema has to be updated. It can be based on String 
     * or the Script object.
     */
    Object updateScriptRef = null

    /**
     * Converts the updateScriptRef object to the String representation (i.e. 'GetShiftNames:0')
     * used in the generated XSD.
     * 
     * @return the converted String
     */
    String getUpdateScriptRefString() {
        if (updateScriptRef == null) {
            return 'null'
        }
        else if (updateScriptRef instanceof Script) {
            Script s = (Script)updateScriptRef
            return s.getName() + ':' + s.getVersion()
        }
        else {
            return updateScriptRef.toString()
        }
    }

    /**
     * Defines the Query name and version (e.g. GetShiftNames:0) which is executed when
     * the from generated from the XML Schema has to be updated. It can be based on String 
     * or the Query object.
     */
    Object updateQuerytRef = null

    /**
     * Converts the updateQuerytRef object to the String representation (i.e. 'GetShiftNames:0')
     * used in the generated XSD.
     * 
     * @return the converted String 
     */
    String getUpdateQueryRefString() {
        if (updateQuerytRef == null) {
            return 'null'
        }
        else if (updateQuerytRef instanceof Query) {
            Script q = (Script)updateQuerytRef
            return q.getName() + ':' + q.getVersion()
        }
        else {
            return updateScriptRef.toString()
        }
    }

    /**
     * Number of digits that are present in the number. Possible value are: P, P-
     */
    String precision = null
    /**
     * Number of decimal places that are present in the number.Possible value are: S, S-
     */
    String scale = null

    /**
     * 
     * @param p
     */
    public void setPrecision(String p) {
        if (!(p ==~ /^\d+[-]?$/)) throw new InvalidDataException("Invalid precision value ("+p+"). Value should be '5' or '5-'")
        precision = p
    }

    /**
     * 
     * @param s
     */
    public void setScale(String s) {
        if (!(s ==~ /^\d+[-]?$/)) throw new InvalidDataException("Invalid scale value ("+s+"). Value should be '5' or '5-'")
        scale = s
    }
}
