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

import java.util.ArrayList;
import java.util.HashMap;

import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.resource.BuiltInResources;
import org.cristalise.kernel.utils.CastorArrayList;
import org.cristalise.kernel.utils.DescriptionObject;

import lombok.Getter;
import lombok.Setter;


@Getter @Setter
public class PropertyDescriptionList extends CastorArrayList<PropertyDescription> implements DescriptionObject {
    String   namespace;
    String   name;
    Integer  version;
    ItemPath itemPath;

    public PropertyDescriptionList() {
        super();
    }

    public PropertyDescriptionList(String name, Integer version) {
        super();
        this.name = name;
        this.version = version;
    }

    public PropertyDescriptionList(ArrayList<PropertyDescription> aList) {
        super(aList);
    }

    public PropertyDescriptionList(String name, Integer version, ArrayList<PropertyDescription> aList) {
        super(aList);
        this.name = name;
        this.version = version;
    }

    public String getClassProps() {
        StringBuffer props = new StringBuffer();
        for (PropertyDescription element : list) {
            if (element.getIsClassIdentifier()) {
                if (props.length()>0)
                    props.append(",");
                props.append(element.getName());
            }
        }
        return props.toString();
    }

    public boolean setDefaultValue(String name, String value) {
        for (PropertyDescription element : list) {
            if (element.getName().equals(name)) {
                element.setDefaultValue(value);
                return true;
            }
        }
        return false;
    }

    public void add(String name, String value, boolean isClassId, boolean isMutable, boolean isTransitive) {
        for (PropertyDescription element : list) {
            if (element.getName().equals(name)) {
                list.remove(element);
                break;
            }
        }
        list.add(new PropertyDescription(name, value, isClassId, isMutable, isTransitive));
    }

    public void add(String name, String value, boolean isClassId, boolean isMutable) {
        add(name, value, isClassId, isMutable, false);
    }

    public void add(String name, String value) {
        add(name, value, false, true, false);
    }

    public boolean definesProperty(String name) {
        for (PropertyDescription element : list) {
            if (element.getName().equals(name)) return true;
        }
        return false;
    }

    /**
     * Before instantiating checks that supplied initial Properties exist in description list 
     * 
     * @param initProps initial list of Properties
     * @return instantiated PropertyArrayList for Item
     * @throws InvalidDataException data was inconsistent
     */
    public PropertyArrayList instantiate(PropertyArrayList initProps) throws InvalidDataException {
        HashMap<String, String> validatedInitProps = new HashMap<>();

        for (Property initProp : initProps.list) {
            if (definesProperty(initProp.getName())) {
                validatedInitProps.put(initProp.getName(), initProp.getValue());
            }
            else {
                throw new InvalidDataException("Initial Property '" + initProp.getName()
                    + "' has not been declared in the PropertyDescriptions:" + getName() + ":" + getVersion());
            }
        }

        PropertyArrayList propInst = new PropertyArrayList();

        for (PropertyDescription pd: list) {
            String propName = pd.getName();
            String propVal = pd.getDefaultValue();

            if (validatedInitProps.containsKey(propName)) propVal = validatedInitProps.get(propName);

            propInst.list.add(new Property(propName, propVal, pd.getIsMutable()));
        }

        return propInst;
    }

    @Override
    public String getItemID() {
        return (itemPath != null) ? itemPath.getUUID().toString() : null;
    }

    @Override
    public CollectionArrayList makeDescCollections(TransactionKey transactionKey) throws InvalidDataException, ObjectNotFoundException {
        return new CollectionArrayList();
    }

    @Override
    public BuiltInResources getResourceType() {
        return BuiltInResources.PROPERTY_DESC_RESOURCE;
    }
}
