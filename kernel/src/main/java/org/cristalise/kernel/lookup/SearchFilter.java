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
package org.cristalise.kernel.lookup;

import java.util.ArrayList;

import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyArrayList;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SearchFilter {
    protected String searchRoot;
    protected PropertyArrayList properties = new PropertyArrayList();
    protected Integer recordsFound;

    /**
     * Method required to be backward compatible with castor marshalling. check issue #518
     * @return
     */
    public ArrayList<Property> getProperties() {
        return properties.list;
    }

    @Override
    public String toString() {
        return "Filter{"+searchRoot+":"+properties.list+"}";
    }
}
