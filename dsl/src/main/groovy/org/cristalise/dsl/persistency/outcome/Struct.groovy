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

/**
 *
 */
class Struct {
    String name
    String documentation
    String width
    boolean useSequence = false

    List<Struct> structs = []

    List<Field> fields = []
    List<Attribute> attributes = []
    AnyField anyField = null

    String minOccurs = null
    String maxOccurs = null
    
    private String getMultiplicityVal(String m) {
        def dec = /^\d+$/

        switch(m) {
            case "*"     : return 'unbounded'
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
        if(!m) {
            minOccurs = ''; maxOccurs = '';
        }
        else if(m.contains("..")) {
            def vals = m.split(/\.\./)

            def v = getMultiplicityVal(vals[0])

            if(v) minOccurs = v
            else  throw new InvalidDataException("Invalid value for multiplicity : '$m'")

            v = getMultiplicityVal(vals[1])

            if(v) maxOccurs = v
            else  maxOccurs = ''
        }
        else {
            def v = getMultiplicityVal(m)

            if(!v) { minOccurs = '0' }
            else   {
                if (v == 'unbounded') { maxOccurs = v; maxOccurs = ''; }
                else                  { minOccurs = v; maxOccurs = v; }
            }
        }
    }
}
