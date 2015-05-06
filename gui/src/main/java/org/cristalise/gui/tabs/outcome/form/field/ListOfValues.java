/**
 * This file is part of the CRISTAL-iSE default user interface.
 * Copyright (c) 2001-2014 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.gui.tabs.outcome.form.field;

import java.util.ArrayList;
import java.util.HashMap;

import org.cristalise.kernel.utils.Logger;

/**************************************************************************
 *
 * $Revision: 1.2 $
 * $Date: 2005/04/26 06:48:12 $
 *
 * Copyright (C) 2003 CERN - European Organization for Nuclear Research
 * All rights reserved.
 **************************************************************************/

public class ListOfValues extends HashMap<String, Object> {

    String defaultKey = null;
    ArrayList<String> orderedKeys = new ArrayList<String>();

    public ListOfValues() {
        super();
    }

    public String put(String key, Object value, boolean isDefaultKey) {
        if (isDefaultKey) defaultKey = key;
        orderedKeys.add(key);
        return (String)super.put(key, value);
    }
    
    public String[] getKeyArray() {
    	return orderedKeys.toArray(new String[orderedKeys.size()]);
    }

    public String getDefaultKey() {
        return defaultKey;
    }
    
    public void setDefaultValue(String newDefaultVal) {
    	defaultKey = findKey(newDefaultVal);
    }
    
    public String findKey(String value) {
    	for (String key : keySet()) {
			if (get(key).equals(value))
				return key;
		}
    	return null;
    }
    
    public Object getDefaultValue() {
    	return get(defaultKey);
    }

}
