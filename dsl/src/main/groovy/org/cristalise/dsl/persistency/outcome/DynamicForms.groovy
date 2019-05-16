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

import org.cristalise.kernel.common.InvalidDataException

import groovy.transform.CompileStatic;


@CompileStatic
class DynamicForms {
    Boolean disabled = null
    String errmsg = null
    Boolean hidden = null
    String inputType = null
    String label = null
    String mask = null
    Integer max = null
    Integer min = null
    Boolean multiple = null
    String pattern = null
    String placeholder = null
    Boolean required = null
    Boolean showSeconds = null
    String type = null
    String value = null
    String oos = null
    
    /**
     * Defines the Script name and version (e.g. GetShiftNames:0) which is executed when
     * the from generated from the XML Schema has to be updated
     */
    String updateScriptRef = null
    /**
     * Defines the Query name and version (e.g. GetShiftNames:0) which is executed when
     * the from generated from the XML Schema has to be updated
     */
    String updateQuerytRef = null
}
