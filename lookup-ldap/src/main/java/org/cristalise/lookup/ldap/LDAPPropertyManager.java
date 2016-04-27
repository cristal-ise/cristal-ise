/**
 * This file is part of the CRISTAL-iSE LDAP lookup plugin.
 * Copyright (c) 2001-2016 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.lookup.ldap;

import java.util.ArrayList;
import java.util.Enumeration;

import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.utils.Logger;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPEntry;

/**************************************************************************
 *
 * $Revision: 1.3 $
 * $Date: 2006/03/03 13:52:21 $
 *
 * Copyright (C) 2003 CERN - European Organization for Nuclear Research
 * All rights reserved.
 **************************************************************************/

public class LDAPPropertyManager {
    /**
     *
     */
    protected final LDAPLookup ldap;
    protected final LDAPAuthManager auth;

    public LDAPPropertyManager(LDAPLookup ldap, LDAPAuthManager auth) {
        super();
        this.ldap = ldap;
        this.auth = auth;
    }

    /**
     * @param thisItem - EntityPath of the subject entity
     * @return
     * @throws ObjectNotFoundException
     */
    public boolean hasProperties(ItemPath thisItem) throws ObjectNotFoundException {
    	LDAPEntry entityEntry = LDAPLookupUtils.getEntry(auth.getAuthObject(), ldap.getFullDN(thisItem));
    	return entityEntry.getAttribute("cristalprop") != null;
    }

    /**
     * @param thisItem - EntityPath of the subject entity
     * @return array of Property
     * @throws ObjectNotFoundException
     */
    public String[] getPropertyNames(ItemPath thisItem) throws ObjectNotFoundException {
        LDAPEntry entityEntry = LDAPLookupUtils.getEntry(auth.getAuthObject(), ldap.getFullDN(thisItem));
        ArrayList<String> propbag = new ArrayList<String>();
        LDAPAttribute props = entityEntry.getAttribute("cristalprop");
        for (Enumeration<?> e = props.getStringValues(); e.hasMoreElements();) {
            String thisProp = (String)e.nextElement();
            String thisName = thisProp.substring(0, thisProp.indexOf(':'));
            if (thisName.startsWith("!") && thisName.length()>1) thisName = thisName.substring(1);
            propbag.add(thisName);
        }

        String[] retArr = new String[props.size()];
        return propbag.toArray(retArr);
    }

    /**
     * @param thisItem - EntityPath of the subject entity
     * @param propName - the name of the property to retrieve
     * @return The Property object
     * @throws ObjectNotFoundException
     */
    public Property getProperty(ItemPath thisItem, String name) throws ObjectNotFoundException {
        LDAPEntry entityEntry = LDAPLookupUtils.getEntry(auth.getAuthObject(), ldap.getFullDN(thisItem));
        return getProperty(entityEntry, name);
    }

    /**
     * @param thisItem - EntityPath of the subject entity
     * @param name - the property name to delete
     * @throws ObjectNotFoundException
     * @throws ObjectCannotBeUpdated
     */
    public void deleteProperty(ItemPath thisItem, String name) throws ObjectNotFoundException, ObjectCannotBeUpdated {
        LDAPEntry entityEntry = LDAPLookupUtils.getEntry(auth.getAuthObject(), ldap.getFullDN(thisItem));
        Property prop = getProperty(entityEntry, name);
        Logger.msg(6, "LDAPLookupUtils.deleteProperty("+name+") - Deleting property");
        LDAPLookupUtils.removeAttributeValue(auth.getAuthObject(), entityEntry, "cristalprop", getPropertyAttrValue(prop));
    }
    
    private static String getPropertyAttrValue(Property prop) {
    	return (prop.isMutable()?"":"!")+prop.getName()+":"+prop.getValue();
    }

    /**
     * @param thisItem - EntityPath of the subject entity
     * @param prop - the property to store
     * @throws ObjectNotFoundException
     * @throws ObjectCannotBeUpdated
     */
    public void setProperty(ItemPath thisItem, Property prop) throws ObjectNotFoundException, ObjectCannotBeUpdated {
        LDAPEntry entityEntry = LDAPLookupUtils.getEntry(auth.getAuthObject(), ldap.getFullDN(thisItem));
        try {
        	Property oldProp = getProperty(entityEntry, prop.getName());
            Logger.msg(6, "LDAPLookupUtils.setProperty("+prop.getName()+") - Removing old value '"+oldProp.getValue()+"'");
            LDAPLookupUtils.removeAttributeValue(auth.getAuthObject(), entityEntry, "cristalprop", getPropertyAttrValue(oldProp));
        } catch (ObjectNotFoundException ex) {
            Logger.msg(6, "LDAPLookupUtils.setProperty("+prop.getName()+") - creating new property.");
        }
        Logger.msg(6, "LDAPLookupUtils.setProperty("+prop.getName()+") - setting to '"+prop.getValue()+"'");
        LDAPLookupUtils.addAttributeValue(auth.getAuthObject(), entityEntry, "cristalprop", getPropertyAttrValue(prop));
    }

    public static Property getProperty(LDAPEntry myEntry, String propName) throws ObjectNotFoundException {
        // delete existing props
        LDAPAttribute props = myEntry.getAttribute("cristalprop");
        if (props == null)
            throw new ObjectNotFoundException("Property "+propName+" does not exist");
        String propPrefix = propName+":";
        String roPropPrefix = "!"+propPrefix;
        String val = null, name = null; boolean mutable = false;
        for (Enumeration<?> e = props.getStringValues(); name==null && e.hasMoreElements();) {
            String attrVal = (String)e.nextElement();
            if (attrVal.toLowerCase().startsWith(propPrefix.toLowerCase())) {
            	name = attrVal.substring(0, propPrefix.length()-1);
                val = attrVal.substring(propPrefix.length());
                mutable = true; break;
            }
            
            if (attrVal.toLowerCase().startsWith(roPropPrefix.toLowerCase())) {
            	name = attrVal.substring(1, roPropPrefix.length()-1);
                val = attrVal.substring(roPropPrefix.length());
                mutable = false; break;
            }
        }
        if (name == null) 
        	throw new ObjectNotFoundException("Property "+propName+" does not exist");
        Logger.msg(6, "Loaded "+(mutable?"":"Non-")+"Mutable Property: "+name+"="+val);
        return new Property(name, val, mutable);
    }

}
