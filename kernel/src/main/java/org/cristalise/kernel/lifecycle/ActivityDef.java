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
package org.cristalise.kernel.lifecycle;

import static org.cristalise.kernel.collection.BuiltInCollections.ACTIVITY;
import static org.cristalise.kernel.collection.BuiltInCollections.QUERY;
import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA;
import static org.cristalise.kernel.collection.BuiltInCollections.SCRIPT;
import static org.cristalise.kernel.collection.BuiltInCollections.STATE_MACHINE;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.NAMESPACE;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.VERSION;
import static org.cristalise.kernel.process.resource.BuiltInResources.ELEM_ACT_DESC_RESOURCE;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Vector;

import org.cristalise.kernel.collection.BuiltInCollections;
import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.collection.Dependency;
import org.cristalise.kernel.collection.DependencyMember;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lifecycle.instance.Activity;
import org.cristalise.kernel.lifecycle.instance.WfVertex;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.querying.Query;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.utils.DescriptionObject;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.LocalObjectLoader;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class ActivityDef extends WfVertexDef implements C2KLocalObject, DescriptionObject {

    private Integer version = null;  // null is 'last', previously was -1
    public boolean  changed  = false;

    ItemPath        itemPath;

    Schema          actSchema;
    Script          actScript;
    Query           actQuery;
    StateMachine    actStateMachine;

    public ActivityDef() {
        mErrors = new Vector<String>(0, 1);
        setProperties(new WfCastorHashMap());
        setIsLayoutable(false);
    }

    public ActivityDef(String n, Integer v) {
        this();
        setName(n);
        setVersion(v);
    }

    @Override
    public void setNamespace(String ns) {
        setBuiltInProperty(NAMESPACE, ns);
    }

    @Override
    public String getNamespace() {
        return (String) getBuiltInProperty(NAMESPACE);
    }

    @Override
    public void setID(int id) {
        super.setID(id);
        if (getName() == null || "".equals(getName())) setName(String.valueOf(id));
    }

    @Override
    public String getItemID() {
        return (itemPath != null) ? itemPath.getUUID().toString() : null;
    }

    @Override
    public void setVersion(Integer v) {
        version = v;
    }

    @Override
    public Integer getVersion() {
        return version;
    }

    @Override
    public String getErrors() {
        return super.getErrors();
    }

    @Override
    public boolean verify() {
        return true;
    }

    @Override
    public ClusterType getClusterType() {
        return null;
    }

    @Override
    public String getClusterPath() {
        return null;
    }

    public String getActName() {
        return getName();
    }

    /**
     */
    public String getDescName() {
        return getName();
    }

    @Override
    public WfVertex instantiate(TransactionKey transactionKey) throws ObjectNotFoundException, InvalidDataException {
        return instantiate(getName(), transactionKey);
    }

    public WfVertex instantiate(String name, TransactionKey transactionKey) throws ObjectNotFoundException, InvalidDataException {
        Activity act = new Activity();
        act.setName(name);

        configureInstance(act, transactionKey);

        if (getItemPath() != null) act.setType(getItemID());

        return act;
    }

    /**
     *
     */
    @Override
    public void configureInstance(WfVertex act, TransactionKey transactionKey) throws InvalidDataException, ObjectNotFoundException {
        super.configureInstance(act, transactionKey);

        try {
            for (String collName : Gateway.getStorage().getClusterContents(itemPath, ClusterType.COLLECTION, transactionKey)) {
                log.debug("configureInstance("+getName()+") - Processing collection:"+collName);

                String verStr = (version == null || version == -1) ? "last" : String.valueOf(version);
                try {
                    Dependency dep = (Dependency) Gateway.getStorage().get(itemPath, ClusterType.COLLECTION+"/"+collName+"/"+verStr, transactionKey);
                    dep.addToVertexProperties(act.getProperties(), transactionKey);
                }
                catch (ObjectNotFoundException e) {
                    log.trace("Unavailable Collection path:"+itemPath+"/"+ClusterType.COLLECTION+"/"+collName+"/"+verStr);
                }
                catch (PersistencyException e) {
                    log.error("Collection:"+collName, e);
                    throw new InvalidDataException("Collection:"+collName+" error:"+e.getMessage());
                }
            }
        }
        catch (PersistencyException e) {
            log.error("", e);
            throw new InvalidDataException(e.getMessage());
        }
    }

    @Override
    public ItemPath getItemPath() {
        return itemPath;
    }

    @Override
    public void setItemPath(ItemPath path) {
        itemPath = path;
    }

    public Schema getSchema() throws InvalidDataException, ObjectNotFoundException {
        return getSchema(null);
    }

    public Schema getSchema(TransactionKey transactionKey) throws InvalidDataException, ObjectNotFoundException {
        if (actSchema == null) {
            DescriptionObject[] descObjects = getBuiltInCollectionResource(SCHEMA, transactionKey);
            if (descObjects.length > 0) actSchema = (Schema)descObjects[0];

            if (actSchema == null) {
                log.trace("getSchema(actName:"+getName()+") - Loading ...");
                actSchema = LocalObjectLoader.getSchema(getProperties(), transactionKey);
            }
        }
        return actSchema;
    }

    public Script getScript() throws InvalidDataException, ObjectNotFoundException {
        return getScript(null);
    }

    public Script getScript(TransactionKey transactionKey) throws InvalidDataException, ObjectNotFoundException {
        if (actScript == null) {
            DescriptionObject[] descObjects = getBuiltInCollectionResource(SCRIPT, transactionKey);
            if (descObjects.length > 0) actScript = (Script)descObjects[0];

            if (actScript == null) {
                log.trace("getScript(actName:"+getName()+") - Loading ...");
                actScript = LocalObjectLoader.getScript(getProperties(), transactionKey);
            }
        }
        return actScript;
    }

    public Query getQuery() throws InvalidDataException, ObjectNotFoundException {
        return getQuery(null);
    }

    public Query getQuery(TransactionKey transactionKey) throws InvalidDataException, ObjectNotFoundException {
        if (actQuery == null) {
            DescriptionObject[] descObjects = getBuiltInCollectionResource(QUERY, transactionKey);
            if (descObjects.length > 0) actQuery = (Query)descObjects[0];

            if (actQuery == null) {
                log.trace("getQuery(actName:"+getName()+") - Loading ...");
                actQuery = LocalObjectLoader.getQuery(getProperties(), transactionKey);
            }
        }
        return actQuery;
    }

    public StateMachine getStateMachine() throws InvalidDataException, ObjectNotFoundException {
        return getStateMachine(null);
    }

    public StateMachine getStateMachine(TransactionKey transactionKey) throws InvalidDataException, ObjectNotFoundException {
        if (actStateMachine == null) {
            DescriptionObject[] descObjects = getBuiltInCollectionResource(STATE_MACHINE, transactionKey);
            if (descObjects.length > 0) actStateMachine = (StateMachine)descObjects[0];

            if (actStateMachine == null) {
                log.trace("getStateMachine(actName:"+getName()+") - Loading ...");
                actStateMachine = LocalObjectLoader.getStateMachine(getProperties(), transactionKey);
            }
        }
        return actStateMachine;
    }

    protected DescriptionObject[] getBuiltInCollectionResource(BuiltInCollections collection, TransactionKey transactionKey) throws ObjectNotFoundException, InvalidDataException {
        ArrayList<DescriptionObject> retArr = new ArrayList<DescriptionObject>();

        if (itemPath == null) {
            log.debug("getBuiltInCollectionResource(actName:{}, collection:{}) - itemPath is null! CANNOT resolve data in ClusterStorage", getName(), collection);
            return retArr.toArray(new DescriptionObject[0]);
            //throw new InvalidDataException("actName:"+getName()+", collection:"+collection+" - itemPath is null! CANNOT resolve data in ClusterStorage");
        }

        log.info("getBuiltInCollectionResource(actName:{}) - Loading from collection:{}", getName(), collection);

        Dependency resColl;

        try {
            String clusterPath = ClusterType.COLLECTION + "/" + collection + "/" +
                    ((version == null || version == -1) ? "last" : String.valueOf(version));

            String[] contents = Gateway.getStorage().getClusterContents(itemPath, clusterPath, transactionKey);
            if (contents != null && contents.length > 0)
                resColl = (Dependency) Gateway.getStorage().get(itemPath, clusterPath, transactionKey);
            else
                return retArr.toArray(new DescriptionObject[retArr.size()]);
        }
        catch (PersistencyException e) {
            log.error("Error loading description collection " + collection, e);
            throw new InvalidDataException("Error loading description collection " + collection);
        }

        for (DependencyMember resMem : resColl.getMembers().list) {
            String resUUID = resMem.getChildUUID();
            Integer resVer = deriveVersionNumber(resMem.getBuiltInProperty(VERSION));

            if (resVer == null) {
                throw new InvalidDataException("Version is null for Item:" + itemPath + ", Collection:" + collection + ", DependencyMember:" + resUUID);
            }

            if (collection != ACTIVITY && retArr.size() > 0) {
                throw new InvalidDataException("actName:"+getName()+ " has an invalid dependency:" + collection);
            }

            switch (collection) {
                case SCHEMA:
                    retArr.add(LocalObjectLoader.getSchema(resUUID, resVer, transactionKey));
                    break;
                case SCRIPT:
                    retArr.add(LocalObjectLoader.getScript(resUUID, resVer, transactionKey));
                    break;
                case QUERY:
                    retArr.add(LocalObjectLoader.getQuery(resUUID, resVer, transactionKey));
                    break;
                case STATE_MACHINE:
                    retArr.add(LocalObjectLoader.getStateMachine(resUUID, resVer, transactionKey));
                    break;
                case ACTIVITY:
                    retArr.add(LocalObjectLoader.getActDef(resUUID, resVer, transactionKey));
                    break;
                default:
                    throw new InvalidDataException("");
            }
        }
        return retArr.toArray(new DescriptionObject[retArr.size()]);
    }

    public void setSchema(Schema actSchema) {
        this.actSchema = actSchema;
    }

    public void setScript(Script actScript) {
        this.actScript = actScript;
    }

    public void setQuery(Query actQuery) {
        this.actQuery = actQuery;
    }

    public void setStateMachine(StateMachine actStateMachine) {
        this.actStateMachine = actStateMachine;
    }

    public Dependency makeDescCollection(BuiltInCollections collection, TransactionKey transactionKey, DescriptionObject... descs) throws InvalidDataException {
        //TODO: restrict membership based on kernel property desc
        Dependency descDep = new Dependency(collection.getName());
        if (version != null && version > -1) {
            descDep.setVersion(version);
        }

        for (DescriptionObject thisDesc : descs) {
            if (thisDesc == null) continue;
            try {
                DependencyMember descMem = descDep.addMember(thisDesc.getItemPath(), transactionKey);
                descMem.setBuiltInProperty(VERSION, thisDesc.getVersion());
            }
            catch (Exception e) {
                log.error("Problem creating description collection for " + thisDesc + " in " + getName(), e);
                throw new InvalidDataException(e.getMessage());
            }
        }
        return descDep;
    }

    @Override
    public CollectionArrayList makeDescCollections(TransactionKey transactionKey) throws InvalidDataException, ObjectNotFoundException {
        CollectionArrayList retArr = new CollectionArrayList();

        retArr.put(makeDescCollection(SCHEMA,        transactionKey, getSchema(transactionKey)));
        retArr.put(makeDescCollection(SCRIPT,        transactionKey, getScript(transactionKey)));
        retArr.put(makeDescCollection(QUERY,         transactionKey, getQuery(transactionKey)));
        retArr.put(makeDescCollection(STATE_MACHINE, transactionKey, getStateMachine(transactionKey)));

        return retArr;
    }

    @Override
    public void export(Writer imports, File dir, boolean shallow) throws InvalidDataException, ObjectNotFoundException, IOException {
        String actXML;
        String tc = ELEM_ACT_DESC_RESOURCE.getTypeCode();

        if (!shallow) exportCollections(imports, dir);

        try {
            actXML = new Outcome(Gateway.getMarshaller().marshall(this)).getData(true);
        }
        catch (Exception e) {
            log.error("Couldn't marshall activity def " + getActName(), e);
            throw new InvalidDataException("Couldn't marshall activity def " + getActName());
        }

        FileStringUtility.string2File(new File(new File(dir, tc), getActName() + (getVersion() == null ? "" : "_" + getVersion()) + ".xml"), actXML);

        if (imports != null) {
            imports.write("<Activity " + getExportAttributes(tc) + ">" + getExportCollections() + "</Activity>\n");
        }
    }

    protected void exportCollections(Writer imports, File dir) throws InvalidDataException, ObjectNotFoundException, IOException {
        if (getStateMachine(null) != null) getStateMachine(null).export(imports, dir, true);
        if (getSchema(null)       != null) getSchema(null).export(imports, dir, true);
        if (getScript(null)       != null) getScript(null).export(imports, dir, true);
        if (getQuery(null)        != null) getQuery(null).export(imports, dir, true);
    }

    protected String getExportAttributes(String type) throws InvalidDataException, ObjectNotFoundException, IOException {
        if (Gateway.getProperties().getBoolean("Resource.useOldImportFormat", false)) {
            return "name=\"" + getActName() + "\" "
                    + (getItemPath() == null ? "" : "id=\""      + getItemID()  + "\" ")
                    + (getVersion() == null  ? "" : "version=\"" + getVersion() + "\" ")
                    + "resource=\"boot/" + type + "/" + getActName() + (getVersion() == null ? "" : "_" + getVersion()) + ".xml\"";
        }
        else {
            return "name=\"" + getActName() + "\" "
                    + (getItemPath() == null ? "" : "id=\""      + getItemID()  + "\" ")
                    + (getVersion() == null  ? "" : "version=\"" + getVersion() + "\" ");
        }
    }

    protected String getExportCollections() throws InvalidDataException, ObjectNotFoundException, IOException {
        return (getStateMachine(null) == null ? "" : "<StateMachine name=\"" + getStateMachine(null).getName() + "\" id=\"" + getStateMachine(null).getItemID() + "\" version=\"" + getStateMachine(null).getVersion() + "\"/>")
                   + (getSchema(null) == null ? "" : "<Schema name=\""       + getSchema(null).getName()       + "\" id=\"" + getSchema(null).getItemID()       + "\" version=\"" + getSchema(null).getVersion()       + "\"/>")
                   + (getScript(null) == null ? "" : "<Script name=\""       + getScript(null).getName()       + "\" id=\"" + getScript(null).getItemID()       + "\" version=\"" + getScript(null).getVersion()       + "\"/>")
                   + (getQuery(null)  == null ? "" : "<Query name=\""        + getQuery(null).getName()        + "\" id=\"" + getQuery(null).getItemID()        + "\" version=\"" + getQuery(null).getVersion()        + "\"/>");
    }

    @Override
    public String toString() {
        return getActName()+"(uuid:"+getItemPath()+")";
    }
}
