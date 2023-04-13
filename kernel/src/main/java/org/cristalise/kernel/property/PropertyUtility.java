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

import static org.cristalise.kernel.persistency.ClusterType.VIEWPOINT;
import static org.cristalise.kernel.process.resource.BuiltInResources.PROPERTY_DESC_RESOURCE;
import static org.cristalise.kernel.property.BuiltInItemProperties.NAME;
import static org.cristalise.kernel.property.BuiltInItemProperties.TYPE;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.LocalObjectLoader;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class to handle operations of ItemProperties and their description
 */
@Slf4j
public class PropertyUtility {
    
    /**
     * Reads the default value of the Property
     * 
     * @param pdlist the list of Properties to search
     * @param name name of the Property to search for
     * @return the defeult value of the property. Can be null.
     * @deprecated Badly named method, use getDefaultValue() instead
     */
    static public String getValue(ArrayList<PropertyDescription> pdlist, String name) {
        return getDefaultValue(pdlist, name);
    }

    /**
     * Reads the default value of the Property
     * 
     * @param pdlist the list of Properties to search
     * @param name name of the Property to search for
     * @return the defeult value of the property. Can be null.
     */
    static public String getDefaultValue(List<PropertyDescription> pdlist, String name) {
        for (PropertyDescription pd : pdlist) {
            if (name.equalsIgnoreCase(pd.getName())) return pd.getDefaultValue();
        }
        return null;
    }

    /**
     * 
     * @param itemPath
     * @param propName
     * @param transactionKey
     * @return
     */
    public static boolean propertyExists(ItemPath itemPath, String propName, TransactionKey transactionKey) {
        try {
            String[] contents = Gateway.getStorage().getClusterContents(itemPath, ClusterType.PROPERTY, transactionKey);

            for (String name: contents) if(name.equals(propName)) return true;
        }
        catch (PersistencyException e) {
            log.error("propertyExists()", e);
        }
        return false;
    }

    /**
     * 
     * @param itemPath
     * @param prop
     * @param transactionKey
     * @return
     * @throws ObjectNotFoundException
     */
    public static Property getProperty(ItemPath itemPath, BuiltInItemProperties prop, TransactionKey transactionKey)
            throws ObjectNotFoundException
    {
        return getProperty(itemPath, prop.getName(), transactionKey);
    }

    /**
     * 
     * @param itemPath
     * @param propName
     * @param transactionKey
     * @return
     * @throws ObjectNotFoundException
     */
    public static Property getProperty(ItemPath itemPath, String propName, TransactionKey transactionKey)
            throws ObjectNotFoundException
    {
        try {
            return (Property)Gateway.getStorage().get(itemPath, ClusterType.PROPERTY+"/"+propName, transactionKey);
        }
        catch (PersistencyException e) {
            log.trace("getProperty()", e);
            throw new ObjectNotFoundException("Could not fetch Property from '"+itemPath+"'", e);
        }
    }

    /**
     * 
     * @param pdlist
     * @return
     */
    static public String getNames(ArrayList<PropertyDescription> pdlist) {
        StringBuffer names = new StringBuffer();

        for (PropertyDescription value : pdlist) {
            names.append(value.getDefaultValue()).append(" ");
        }

        return names.toString();
    }

    /**
     * 
     * @param pdlist
     * @return
     */
    static public String getClassIdNames(ArrayList<PropertyDescription> pdlist) {
        StringBuffer names = new StringBuffer();

        for (Iterator<PropertyDescription> iter = pdlist.iterator(); iter.hasNext();) {
            PropertyDescription pd = iter.next();

            if (pd.getIsClassIdentifier()) names.append(pd.getName());
            if (iter.hasNext()) names.append(",");
        }
        return names.toString();
    }

    /**
     * Reads the PropertyDescriptionList either from a built-in type of Item using LocalObjectLoader 
     * or from the 'last' Viewpoint of the 'PropertyDescription' Outcome in the Item (very likely a Factory Item).
     * 
     * @param itemPath the Item containing the PropertyDescriptionList
     * @param descVer the version of the PropertyDescriptionList
     * @param transactionKey transaction key
     * @return the PropertyDescriptionList
     * @throws ObjectNotFoundException PropertyDescriptionList cannot be retrieved
     */
    static public PropertyDescriptionList getPropertyDescriptionOutcome(ItemPath itemPath, String descVer, TransactionKey transactionKey)
            throws ObjectNotFoundException
    {
        try {
            //the type of the Item is a PropertyDesc
            if (getProperty(itemPath, TYPE, transactionKey).getValue().equals(PROPERTY_DESC_RESOURCE.getSchemaName())) {
                String name = getProperty(itemPath, NAME, transactionKey).getValue();

                int version = getVersionID(itemPath, descVer, PROPERTY_DESC_RESOURCE.getSchemaName(), transactionKey);

                return LocalObjectLoader.getPropertyDescriptionList(name, version, transactionKey);
            }
            else  {
                //the type of the Item is very likely a Factory
                Outcome outc = (Outcome) Gateway.getStorage().get(itemPath, VIEWPOINT+"/PropertyDescription/"+descVer+"/data", transactionKey);
                return (PropertyDescriptionList) Gateway.getMarshaller().unmarshall(outc.getData());
            }
        }
        catch (Exception e) {
            log.trace("getPropertyDescriptionOutcome()", e);
            throw new ObjectNotFoundException("Could not fetch PropertyDescription from '"+itemPath+"'", e);
        }
    }

    /**
     * 
     * @param itemPath
     * @param descVer
     * @param schema
     * @param transactionKey
     * @return
     * @throws PersistencyException
     * @throws ObjectNotFoundException
     */
    private static int getVersionID(ItemPath itemPath, String descVer, String schema, TransactionKey transactionKey)
        throws PersistencyException, ObjectNotFoundException
    {
        int version = 0;

        //find the 'last' version
        if ("last".equals(descVer)) {
            String[] views = Gateway.getStorage().getClusterContents(itemPath, VIEWPOINT+"/"+schema, transactionKey);
            version = -1;

            for (int i = 0; i < views.length; i ++) {
                if (StringUtils.isNumeric(views[i])) {
                    int aVersion = Integer.parseInt(views[i]);
                    if (version < aVersion) version = aVersion;
                }
            }

            if (version == -1)
                throw new ObjectNotFoundException(String.format("itemPath:{} schema:{} does not have any version", itemPath, schema));
        }
        else {
            if (StringUtils.isNumeric(descVer)) version = Integer.parseInt(descVer);
            else throw new ObjectNotFoundException("descVer:'"+descVer+"' must be 'last' or positive integer");
        }

        return version;
    }

    /**
     * Converts transitive PropertyDescriptions to VertexProprties (CastorHashMap). ClassIdentifiers are transitive by default.
     * 
     * @param pdList the PropertyDescriptions to be used
     * @return the initialised CastorHashMap
     */
    static public CastorHashMap convertTransitiveProperties(PropertyDescriptionList pdList) {
        CastorHashMap props = new CastorHashMap();

        for (int i = 0; i < pdList.list.size(); i++) {
            PropertyDescription pd = pdList.list.get(i);

            if (pd.isTransitive()) props.put(pd.getName(), pd.getDefaultValue());
        }
        return props;
    }

    /**
     * Updates (writes) the value of an existing Property
     * 
     * @param item the Path (UUID) of actual Item
     * @param prop the BuiltIn ItemProperty to write
     * @param value the value of the Property
     * @param transactionKey transaction key
     * @throws PersistencyException something went wrong updating the database
     * @throws ObjectCannotBeUpdated the Property is immutable
     * @throws ObjectNotFoundException there is no Property with the given name
     */
    public static void writeProperty(ItemPath item, BuiltInItemProperties prop, String value, TransactionKey transactionKey)
            throws PersistencyException, ObjectCannotBeUpdated, ObjectNotFoundException
    {
        writeProperty(item, prop.getName(), value, transactionKey);
    }

    /**
     * Updates (writes) the value of an existing Property
     * 
     * @param item the Path (UUID) of actual Item
     * @param name the name of the Property to write
     * @param value the value of the Property
     * @param transactionKey transaction key
     * @throws PersistencyException something went wrong updating the database
     * @throws ObjectCannotBeUpdated the Property is immutable
     * @throws ObjectNotFoundException there is no Property with the given name
     */
    public static void writeProperty(ItemPath item, String name, String value, TransactionKey transactionKey)
            throws PersistencyException, ObjectCannotBeUpdated, ObjectNotFoundException
    {
        Property prop = (Property) Gateway.getStorage().get(item, ClusterType.PROPERTY + "/" + name, transactionKey);

        if (!prop.isMutable())
            throw new ObjectCannotBeUpdated("WriteProperty: Property '" + name + "' is not mutable.");

        //only update if the value was changed
        if (!value.equals(prop.getValue())) {
            prop.setValue(value);

            Gateway.getStorage().put(item, prop, transactionKey);
        }
    }

    /**
     * 
     * @param item
     * @param prop
     * @param transactionKey
     * @return
     */
    public static boolean checkProperty(ItemPath item, BuiltInItemProperties prop, TransactionKey transactionKey) {
        return checkProperty(item, prop.getName(), transactionKey);
    }

    /**
     * 
     * @param item
     * @param name
     * @param transactionKey
     * @return
     */
    public static boolean checkProperty(ItemPath item, String name, TransactionKey transactionKey) {
        try {
            for (String key : Gateway.getStorage().getClusterContents(item,  ClusterType.PROPERTY, transactionKey)) {
                if (key.equals(name)) return true;
            }
        }
        catch (PersistencyException e) {}

        return false;
    }

    /**
     * 
     * @param item
     * @param prop
     * @param defaultValue
     * @param transactionKey
     * @return
     */
    public static String getPropertyValue(ItemPath item, BuiltInItemProperties prop, String defaultValue, TransactionKey transactionKey) {
        return getPropertyValue(item, prop.getName(), defaultValue, transactionKey);
    }

    /**
     * 
     * @param item
     * @param name
     * @param defaultValue
     * @param transactionKey
     * @return
     */
    public static String getPropertyValue(ItemPath item, String name, String defaultValue, TransactionKey transactionKey) {
        if (checkProperty(item, name, transactionKey)) {
            try {
                return getProperty(item, name, transactionKey).getValue();
            }
            catch (ObjectNotFoundException e) {
                //This line should never happen because of the use of checkProperty()
            }
        }
        return defaultValue;
    }
}
