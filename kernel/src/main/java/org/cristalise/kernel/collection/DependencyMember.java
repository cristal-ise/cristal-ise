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
package org.cristalise.kernel.collection;

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.DEPENDENCY_DISABLE_TYPE_CHECK;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCRIPT_NAME;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.graph.model.BuiltInVertexProperties;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyArrayList;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.scripting.ScriptingEngineException;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.KeyValuePair;
import org.cristalise.kernel.utils.LocalObjectLoader;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DependencyMember implements CollectionMember {
    private ItemPath      mItemPath   = null;
    private int           mId         = -1;
    private CastorHashMap mProperties = null;
    private String        mClassProps = "";

    public DependencyMember() {
        mProperties = new CastorHashMap();
    }

    @Override
    public ItemPath getItemPath() {
        return mItemPath;
    }

    public void setProperties(CastorHashMap props) {
        mProperties = props;
    }

    @Override
    public CastorHashMap getProperties() {
        return mProperties;
    }

    public KeyValuePair[] getKeyValuePairs() {
        return mProperties.getKeyValuePairs();
    }

    public void setKeyValuePairs(KeyValuePair[] pairs) {
        mProperties.setKeyValuePairs(pairs);
    }

    @Override
    public int getID() {
        return mId;
    }

    public void setID(int id) {
        mId = id;
    }

    public void setClassProps(String props) {
        mClassProps = props;
    }

    @Override
    public String getClassProps() {
        return mClassProps;
    }

    private boolean checkType() {
        // NOTE that the value of the property ignored
        return getBuiltInProperty(DEPENDENCY_DISABLE_TYPE_CHECK) == null;
    }

    @Override
    public void assignItem(ItemPath itemPath, TransactionKey transactionKey) throws InvalidCollectionModification {
        if (itemPath == null) {
            log.warn("assignItem() -  Assigning null itemPath to a collection of Item:"+mItemPath.getItemName(transactionKey));
        }
        else if (checkType()) {
            if (mClassProps == null || getProperties() == null)
                throw new InvalidCollectionModification("ClassProps not yet set. Cannot check membership validity.");

            // for each mandatory prop check if its in the member property and has the matching value
            StringTokenizer sub = new StringTokenizer(mClassProps, ",");

            while (sub.hasMoreTokens()) {
                String aClassProp = sub.nextToken();
                try {
                    String memberValue = (String) getProperties().get(aClassProp);
                    Property itemProperty = (Property) Gateway.getStorage().get(itemPath, ClusterType.PROPERTY + "/" + aClassProp, transactionKey);

                    if (itemProperty == null)
                        throw new InvalidCollectionModification("Property " + aClassProp + " does not exist for item " + itemPath);

                    if (!itemProperty.getValue().equalsIgnoreCase(memberValue))
                        throw new InvalidCollectionModification("Values of mandatory prop " + aClassProp
                                + " do not match " + itemProperty.getValue() + "!=" + memberValue);
                }
                catch (PersistencyException | ObjectNotFoundException ex) {
                    log.error("assignItem() - Error checking properties", ex);
                    throw new InvalidCollectionModification("Error checking properties", ex);
                }
            }
        }

        mItemPath = itemPath;
    }

    @Override
    public void clearItem() {
        mItemPath = null;
    }

    @Override
    public ItemProxy resolveItem(TransactionKey transactionKey) throws ObjectNotFoundException {
        return Gateway.getProxy(mItemPath, transactionKey);
    }

    public void setChildUUID(String uuid) throws InvalidCollectionModification, InvalidItemPathException {
        mItemPath = new ItemPath(uuid);
    }

    @Override
    public String getChildUUID() {
        return mItemPath.getUUID().toString();
    }

    public Object getProperty(String prop) {
        return mProperties.get(prop);
    }

    public void setProperty(String prop, Object val) {
        mProperties.put(prop, val, false);
    }

    public Object getBuiltInProperty(BuiltInVertexProperties prop) {
        return mProperties.get(prop.getName());
    }

    public void setBuiltInProperty(BuiltInVertexProperties prop, Object val) {
        mProperties.put(prop.getName(), val, false);
    }

    /**
     * 
     * @return either PropertyArrayList or CastorHashMap
     * 
     * @throws InvalidDataException
     * @throws ObjectNotFoundException
     */
    protected Object evaluateScript(TransactionKey transactionKey) throws InvalidDataException, ObjectNotFoundException {
        log.debug("evaluateScript() - memberUUID:" + getChildUUID());
        Script script = LocalObjectLoader.getScript(getProperties(), transactionKey);

        try {
            script.setInputParamValue("dependencyMember", this);

            script.setInputParamValue("storage", Gateway.getStorage());
//            script.setInputParamValue("proxy", Gateway.getProxyManager());
            script.setInputParamValue("lookup", Gateway.getLookup());

            return script.evaluate(getItemPath(), getProperties(), null, transactionKey);
        }
        catch (ScriptingEngineException e) {
            log.error("", e);
            throw new InvalidDataException(e.getMessage());
        }
    }

    /**
     * 
     * @param propDesc
     * @param newMember
     * @throws ObjectNotFoundException
     * @throws InvalidCollectionModification
     */
    public void updatePropertiesFromDescription(CastorHashMap propDesc, DependencyMember newMember) 
            throws ObjectNotFoundException, InvalidCollectionModification
    {
        for(String key: propDesc.keySet()) {
            Object newValue = newMember != null ? newMember.mProperties.get(key) : null;
            Object newDefaultValue = propDesc.get(key);

            if (mProperties.containsKey(key)) {
                if (mClassProps.contains(key)) {
                    // Update default value of Class Identifier
                    mProperties.put(key, newDefaultValue);
                }
                else {
                    // Update if there is a newValue
                    if (newValue != null) mProperties.put(key, newValue);
                }
            }
            else {
                if (mClassProps.contains(key)) {
                    // Add Class Identifier
                    mProperties.put(key, newDefaultValue);
                }
                else {
                    // Add using newValue or the default value from propDesc
                    mProperties.put(key, newValue != null ? newValue : newDefaultValue);
                }
            }
        }

        Iterator<String> propsNames =  mProperties.keySet().iterator();

        while (propsNames.hasNext()) {
            String key = propsNames.next();

            if(! propDesc.containsKey(key)) {
                propsNames.remove();
            }
        }
    }

    /**
     * Only update existing properties otherwise throw an exception
     * 
     * @param newProps the new properties
     * @throws ObjectNotFoundException property does not exists for member
     * @throws InvalidCollectionModification cannot update class properties
     */
    public void updateProperties(CastorHashMap newProps) throws ObjectNotFoundException, InvalidCollectionModification {
        for (Entry<String, Object> newProp: newProps.entrySet()) {
            String key = newProp.getKey();
            if (mClassProps.contains(key)) {
                Object value = newProp.getValue();
                if (! value.equals(getProperties().get(newProp.getKey()))) {
                    throw new InvalidCollectionModification("Dependency cannot change classProperties:"+key);
                }
            }

            if (getProperties().containsKey(newProp.getKey())) {
                getProperties().put(newProp.getKey(), newProp.getValue());
            }
            else {
                throw new ObjectNotFoundException("Property "+newProp.getKey()+" does not exists for slotID:" + getID());
            }
        }
    }

    /**
     * Executes Script if it was defined in the Member properties
     * 
     * @param props the current list of ItemProperties
     * @param member the current DependencyMember
     * @return true when Script was executed
     * @throws InvalidDataException
     * @throws ObjectNotFoundException
     */
    public boolean convertToItemPropertyByScript(PropertyArrayList props, TransactionKey transactionKey)  throws InvalidDataException, ObjectNotFoundException {
        log.debug("convertToItemPropertyByScript() - memberUUID:"+getChildUUID());

        String scriptName = (String)getBuiltInProperty(SCRIPT_NAME);

        if (scriptName != null && scriptName.length() > 0) {
            Object result = evaluateScript(transactionKey);

            if (result != null && result instanceof PropertyArrayList) {
                props.merge((PropertyArrayList)result);
                return true;
            }
            else {
                throw new InvalidDataException("Script '" + scriptName + "' returned null or the wrong type");
            }
        }
        return false;
    }

}
