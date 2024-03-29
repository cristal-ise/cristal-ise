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
class Field extends Attribute {
    /**
     * Keys used for reading the header in the TabularSchemaBuilder
     */
    public static final List<String> keys = [
        'name', 'type', 'documentation', 'multiplicity', 'values', 'pattern', 'default',
        'length', 'minLength', 'maxLength',
        'range', 'minInclusive', 'maxInclusive', 'minExclusive', 'maxExclusive',
        'totalDigits', 'fractionDigits'
    ]

    String minOccurs = '1'
    String maxOccurs = '1'

    Unit unit
    DynamicForms dynamicForms
    ListOfValues listOfValues

    List<Attribute> attributes = []

    private String getMultiplicityVal(String m) {
        def dec = /^\d+$/

        switch(m) {
            case "*"     : return ''
            case ~/$dec/ : return m
            default      : throw new InvalidDataException("Invalid value for multiplicity : '$m'")
        }
    }

    /**
     * 
     * @param m
     * @return
     */
    public void setMultiplicity(String m) {
        m = m?.trim()

        if (!m) {
            minOccurs = ''; maxOccurs = '';
        }
        else if (m.contains("..")) {
            def vals = m.split(/\.\./)

            def v = getMultiplicityVal(vals[0])

            if (v) minOccurs = v
            else   throw new InvalidDataException("Invalid value for multiplicity : '$m'")

            v = getMultiplicityVal(vals[1])

            if (v) maxOccurs = v
            else   maxOccurs = 'unbounded'

            multiplicityString = m
        }
        else {
            def v = getMultiplicityVal(m)

            if (!v) { minOccurs = '0'; maxOccurs = 'unbounded'; }
            else    { minOccurs = v;   maxOccurs = v; }

            multiplicityString = m
       }
    }

    def setUnit(Unit u) {
        if (!u.values) throw new InvalidDataException("Unit must specify values")

        if (values)     throw new InvalidDataException("UNIMPLEMENTED: Cannot use unit and values together")
        if (attributes) throw new InvalidDataException("UNIMPLEMENTED: Cannot use unit and attributes together")

        unit = u
    }

    /**
     * 
     * @return
     */
    public boolean hasAdditional() {
        return dynamicForms.additional || dynamicForms.updateScriptRef || dynamicForms.updateQuerytRef || dynamicForms.warning || dynamicForms.updateFields;
    }

    /**
     * 
     * @return
     */
    public boolean isFileUpload() {
        return dynamicForms.inputType == 'file'
    }

    @Override
    public boolean isRequired() {
        return minOccurs == null || minOccurs != '0'
    }
}
