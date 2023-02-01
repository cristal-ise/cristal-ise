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
package org.cristalise.kernel.utils;

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.VERSION;
import static org.cristalise.kernel.process.resource.BuiltInResources.SCHEMA_RESOURCE;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import org.cristalise.kernel.collection.BuiltInCollections;
import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.collection.Dependency;
import org.cristalise.kernel.collection.DependencyMember;
import org.cristalise.kernel.common.CriseVertxException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.resource.BuiltInResources;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;

public interface DescriptionObject {

    public String getNamespace();
    public String getName();
    public Integer getVersion();
    public ItemPath getItemPath();

    public void setNamespace(String ns);
    public void setName(String name);
    public void setVersion(Integer version);
    public void setItemPath(ItemPath path);

    public String getItemID();
    public BuiltInResources getResourceType();

    default public String getXml(boolean prettyPrint) throws InvalidDataException {
        try {
            String xml = Gateway.getMarshaller().marshall(this);

            if (prettyPrint) return new Outcome(xml).getData(true);
            else             return xml;
        }
        catch (Exception e) {
            throw new InvalidDataException("Couldn't marshall " + getResourceType().getSchemaName() + " name:" + getName());
        }
    }

    default public String getXml() throws InvalidDataException {
        return getXml(true);
    }

    public CollectionArrayList makeDescCollections(TransactionKey transactionKey) throws InvalidDataException, ObjectNotFoundException;

    default public void export(Writer imports, File dir, boolean shallow) throws InvalidDataException, ObjectNotFoundException, IOException {
        BuiltInResources type = getResourceType();
        String versionPostfix = getVersion() == null ? "" : "_" + getVersion();
        String extention = type == SCHEMA_RESOURCE ? ".xsd" : ".xml";

        String fileName = getName() + versionPostfix + extention;
        File newFile = new File(new File(dir, type.getTypeCode()), fileName);
        FileStringUtility.string2File(newFile, getXml());

        if (imports == null) return;

        if (Gateway.getProperties().getBoolean("Resource.useOldImportFormat", false)) {
            imports.write("<Resource "
                    + "name='" + getName() + "' "
                    + (getItemPath() == null ? "" : "id='"      + getItemID()  + "' ")
                    + (getVersion()  == null ? "" : "version='" + getVersion() + "' ")
                    + "type='" + type.getTypeCode() + "'>boot/" + type.getTypeCode() + "/" + fileName
                    + "</Resource>\n");
        }
        else {
            imports.write("<" + type.getSchemaName() + "Resource "
                    + "name='" + getName() + "' "
                    + (getItemPath() == null ? "" : "id='"      + getItemID()  + "' ")
                    + (getVersion()  == null ? "" : "version='" + getVersion() + "'")
                    + "/>\n");
        }
    }

    default public Dependency makeDescCollection(BuiltInCollections collection, TransactionKey transactionKey, DescriptionObject... descs) throws InvalidDataException {
        //TODO: restrict membership based on kernel property desc
        Dependency descDep = new Dependency(collection.getName());
        if (getVersion() != null && this.getVersion() > -1) {
            descDep.setVersion(this.getVersion());
        }

        for (DescriptionObject thisDesc : descs) {
            if (thisDesc == null) continue;
            try {
                DependencyMember descMem = descDep.addMember(thisDesc.getItemPath(), transactionKey);
                descMem.setBuiltInProperty(VERSION, thisDesc.getVersion());
            }
            catch (Exception e) {
                throw new InvalidDataException(e);
            }
        }
        return descDep;
    }

    default public Outcome toOutcome() throws CriseVertxException, MarshalException, ValidationException, IOException, MappingException {
        String schemaName = getResourceType().getSchemaName();
        Schema schema = LocalObjectLoader.getSchema(schemaName, 0);
        return new Outcome(getXml(false), schema);
    }

    default public boolean exists(TransactionKey transactionKey) {
        String path = getResourceType().getTypeRoot() + "/" + getNamespace() + "/" + getName();
        return new DomainPath(path).exists(transactionKey);
    }
}
