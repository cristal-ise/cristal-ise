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
package org.cristalise.kernel.property;

import java.util.List;
import org.cristalise.kernel.utils.CastorArrayList;

public class PropertyArrayList extends CastorArrayList<Property> {
    
    public PropertyArrayList() {
        super();
    }

    /**
     * Puts all Properties in order, so later ones with the same name overwrite earlier ones
     * 
     * @param aList the list to initialise this list
     */
    public PropertyArrayList(List<Property> aList) {
        super();
        for (Property property : aList) {
            put(property);
        }
    }

    public void put(Property p) {
        if (contains(p.getName())) remove(p);
        list.add(p);
    }

    public boolean contains(String name) {
        for (Property p : list) {
            if (p.getName().equals(name)) return true;
        }
        return false;
    }

    public Property get(String name) {
        for (Property p : list) {
            if (p.getName().equals(p.getName())) return p;
        }
        return null;
    }

    /**
     * @param p
     */
    private void remove(Property p) {
        for (Property thisProp : list) {
            if (thisProp.getName().equals(p.getName())) {
                list.remove(thisProp);
                break;
            }
        }
    }

    /**
     * Merge properties
     * 
     * @param newProps the new properties to be merged
     */
    public void merge(PropertyArrayList newProps) {
        for (Property newProp : newProps.list) {
            put(newProp);
        }
    }
}
