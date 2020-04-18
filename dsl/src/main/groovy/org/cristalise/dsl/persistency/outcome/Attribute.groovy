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
class Attribute {
    /**
     * accepted types from XSD specification without namespace (i.e. xs:)
     */
    public static final List types = ['string', 'boolean', 'integer', 'decimal', 'date', 'time', 'dateTime']

    String name
    String type = 'xs:string'
    String pattern

    String documentation

    List values
    BigDecimal minExclusive = null, minInclusive= null, maxExclusive= null, maxInclusive= null
    BigInteger length = null, minLength = null, maxLength = null

    def defaultVal

    boolean required = false

    Integer totalDigits = null //precision:
    Integer fractionDigits = null //scale:
    
    Reference reference = null

    /**
     * Checks if the type is acceptable
     * 
     * @param t
     * @return
     */
    def setType(String t) {
        if (types.contains(t)) type = "xs:$t"
        else                    throw new InvalidDataException("Field type '$t' is wrong, it must be one of these: $types")
    }

    /**
     * 'default' is a keyword, so it cannot be used as a variable name, 
     * but this method makes the default keyword usable in the SchemaBuilder DSL
     * 
     * @param val
     * @return
     */
    def setDefault(val) {
        if(values && !values.contains(val)) throw new InvalidDataException("Default value '$val' is wrong, it must be one of these: $values")

        defaultVal = val
    }

    public void setMultiplicity(String m) {
        if(m?.trim()) {
            if(m == "1")         required = true
            else if(m == "1..1") required = true
            else if(m == "0..1") required = false
            else                 throw new InvalidDataException("Invalid value for attribute multiplicity : '$m'")
        }
    }

    /**
     * Inclusive uses [], exclusive uses ()
     * 
     * @param r the string form of the range, e.g. [0..10]
     */
    public void setRange(String r) {
        if(r?.trim() && r.contains("..")) {
            def vals = r.split(/\.\./)
            
            def minTypeChar  = vals[0].getAt(0)
            def minValString = vals[0].substring(1)

            def maxTypeChar = vals[1].getAt(vals[1].length()-1)
            def maxValString = vals[1].substring(0, vals[1].length()-1)
            
            if (     minTypeChar == '[') minInclusive = new BigDecimal(minValString)
            else if (minTypeChar == '(') minExclusive = new BigDecimal(minValString)

            if (     maxTypeChar == ']') maxInclusive = new BigDecimal(maxValString)
            else if (maxTypeChar == ')') maxExclusive = new BigDecimal(maxValString)
        }
        else throw new UnsupportedOperationException("Range must be in the format of '[0..123)")
    }

    /**
     * Sets the minInclusive BigDecimal value from String. It was required for the ExcelSchemaBuilder functionality.
     * @param val of minInclusive
     */
    public void setMinInclusive(String val) {
        minInclusive = new BigDecimal(val)
    }

    /**
     * Sets the maxInclusive BigDecimal value from String. It was required for the ExcelSchemaBuilder functionality.
     * @param val of maxInclusive
     */
    public void setMaxInclusive(String val) {
        maxInclusive = new BigDecimal(val)
    }

    /**
     * Sets the minExclusive BigDecimal value from String. It was required for the ExcelSchemaBuilder functionality.
     * @param val of minExclusive
     */
    public void setMinExclusive(String val) {
        minExclusive = new BigDecimal(val)
    }

    /**
     * Sets the maxExclusive BigDecimal value from String. It was required for the ExcelSchemaBuilder functionality.
     * @param val of maxExclusive
     */
    public void setMaxExclusive(String val) {
        maxExclusive = new BigDecimal(val)
    }
}
