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
package org.cristalise.kernel.process.resource;

import java.util.HashSet;
import java.util.Set;

import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.lifecycle.ActivityDef;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.PropertyDescriptionList;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.utils.LocalObjectLoader;


public class DefaultResourceImportHandler implements ResourceImportHandler {

    BuiltInResources         type;
    DomainPath               typeRootPath;
    PropertyDescriptionList  props;

    public DefaultResourceImportHandler(BuiltInResources resType) throws Exception {
        type = resType;
        typeRootPath = new DomainPath(type.getTypeRoot());
        props = (PropertyDescriptionList)Gateway.getMarshaller().unmarshall(Gateway.getResource().getTextResource(null, "boot/property/"+resType+"Prop.xml"));
    }

    @Deprecated
    public DefaultResourceImportHandler(String resType) throws Exception {
        this(BuiltInResources.getValue(resType));
    }

    @Override
    public CollectionArrayList getCollections(String name, String ns, String location, Integer version) throws Exception {
        if (type.getSchemaName().endsWith("ActivityDef")) {
            String actData = Gateway.getResource().getTextResource(ns, location);
            ActivityDef actDef = (ActivityDef)Gateway.getMarshaller().unmarshall(actData);
            actDef.setVersion(version);
            return actDef.makeDescCollections();
        }
        else if (type.getSchemaName().equals("Script")) {
            Script thisScript = new Script(name, version, null, Gateway.getResource().getTextResource(ns, location));
            return thisScript.makeDescCollections();
        }
        return new CollectionArrayList();
    }

    @Override
    public DomainPath getTypeRoot() {
        return typeRootPath;
    }

    @Override
    public String getName() {
        return type.getSchemaName();
    }

    @Override
    public DomainPath getPath(String name, String ns) throws Exception {
        //return new DomainPath(type.getTypeRoot()+"/system/"+(ns == null ? "kernel" : ns)+'/'+name);
        return new DomainPath(type.getTypeRoot()+"/"+(ns == null ? "kernel" : ns)+'/'+name);
    }

    @Override
    public Set<Outcome> getResourceOutcomes(String name, String ns, String location, Integer version) throws Exception {
        HashSet<Outcome> retArr = new HashSet<Outcome>();
        String data = Gateway.getResource().getTextResource(ns, location);

        if (data == null) throw new Exception("No data found for "+type.getSchemaName()+" "+name);

        Outcome resOutcome = new Outcome(0, data, LocalObjectLoader.getSchema(type.getSchemaName(), 0));
        retArr.add(resOutcome);
        return retArr;
    }

    @Override
    public PropertyDescriptionList getPropDesc() throws Exception {
        return props;
    }

    @Override
    public String getWorkflowName() throws Exception {
        return type.getWorkflowDef();
    }
}
