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

import static org.cristalise.kernel.process.resource.BuiltInResources.QUERY_RESOURCE;
import static org.cristalise.kernel.process.resource.BuiltInResources.SCHEMA_RESOURCE;
import static org.cristalise.kernel.process.resource.BuiltInResources.SCRIPT_RESOURCE;
import static org.cristalise.kernel.process.resource.ResourceImportHandler.Status.CHANGED;
import static org.cristalise.kernel.process.resource.ResourceImportHandler.Status.IDENTICAL;
import static org.cristalise.kernel.process.resource.ResourceImportHandler.Status.NEW;
import static org.cristalise.kernel.process.resource.ResourceImportHandler.Status.OVERWRITTEN;
import static org.cristalise.kernel.process.resource.ResourceImportHandler.Status.UNCHANGED;
import static org.cristalise.kernel.property.BuiltInItemProperties.MODULE;
import static org.cristalise.kernel.property.BuiltInItemProperties.NAME;
import static org.cristalise.kernel.security.BuiltInAuthc.SYSTEM_AGENT;

import java.util.HashSet;
import java.util.Set;

import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lifecycle.CompositeActivityDef;
import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.LookupManager;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyArrayList;
import org.cristalise.kernel.property.PropertyDescription;
import org.cristalise.kernel.property.PropertyDescriptionList;
import org.cristalise.kernel.querying.Query;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.utils.DescriptionObject;
import org.cristalise.kernel.utils.LocalObjectLoader;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultResourceImportHandler implements ResourceImportHandler {

    BuiltInResources         type;
    DomainPath               typeRootPath;
    PropertyDescriptionList  props;

    private Status status = IDENTICAL;

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
        return getCollections(name, version,Gateway.getResource().getTextResource(ns, location));
    }

    @Override
    public CollectionArrayList getCollections(String name, Integer version, Outcome outcome) throws Exception {
        return getCollections(name, version, outcome.getData());
    }

    private CollectionArrayList getCollections(String name, Integer version, String xml) throws Exception {
        if (type == SCHEMA_RESOURCE) {
            return new Schema(name, version, null, xml).makeDescCollections();
        }
        else if (type == SCRIPT_RESOURCE) {
            return new Script(name, version, null, xml).makeDescCollections();
        }
        else if (type == QUERY_RESOURCE) {
            return new Query(name, version, null, xml).makeDescCollections();
        }
        else {
            DescriptionObject descObject = (DescriptionObject)Gateway.getMarshaller().unmarshall(xml);
            descObject.setVersion(version);
            return descObject.makeDescCollections();
        }
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
    public DomainPath getPath(String name, String ns) {
        return new DomainPath(type.getTypeRoot()+"/"+(ns == null ? "kernel" : ns)+'/'+name);
    }

    @Override
    public Set<Outcome> getResourceOutcomes(String name, String ns, String location, Integer version) 
            throws InvalidDataException, ObjectNotFoundException
    {
        HashSet<Outcome> retArr = new HashSet<Outcome>();
        String data = Gateway.getResource().getTextResource(ns, location);

        if (data == null) throw new ObjectNotFoundException("No data found for "+type.getSchemaName()+" "+name);

        Outcome resOutcome = new Outcome(0, data, LocalObjectLoader.getSchema(type.getSchemaName(), 0));
        retArr.add(resOutcome);
        return retArr;
    }

    @Override
    public PropertyDescriptionList getPropDesc() {
        return props;
    }

    @Override
    public String getWorkflowName() {
        return type.getWorkflowDef();
    }


    /********************************
     * Methods migrated from Bootstrap
     ********************************/

    @Override
    public DomainPath createResource(String ns, String itemName, int version, Set<Outcome> outcomes, boolean reset)
            throws Exception
    {
        return verifyResource(ns, itemName, version, null, outcomes, reset);
    }

    @Override
    public DomainPath importResource(String ns, String itemName, int version, ItemPath itemPath, String dataLocation, boolean reset)
            throws Exception
    {
        return verifyResource(ns, itemName, version, itemPath, getResourceOutcomes(itemName, ns, dataLocation, version), reset);
    }

    @Override
    public DomainPath importResource(String ns, String itemName, int version, ItemPath itemPath, Set<Outcome> outcomes, boolean reset)
            throws Exception
    {
        return verifyResource(ns, itemName, version, itemPath, outcomes, reset);
    }

    /**
     * 
     * @param ns
     * @param itemName
     * @param version
     * @param itemPath
     * @param outcomes
     * @param dataLocation
     * @param reset
     * @return
     * @throws Exception
     */
    private DomainPath verifyResource(String ns, String itemName, int version, ItemPath itemPath, Set<Outcome> outcomes, boolean reset) 
            throws Exception
    {
        log.info("verifyResource() - Item '{}' of type '{}' verion '{}'", itemName, getName(), version);

        // Find or create Item for Resource
        ItemProxy thisProxy;
        DomainPath modDomPath = getPath(itemName, ns);

        if (modDomPath.exists()) {
            log.info("verifyResource() - Found "+getName()+" "+itemName + ".");

            thisProxy = verifyPathAndModuleProperty(ns, itemName, itemPath, modDomPath, modDomPath);
        }
        else {
            if (itemPath == null) itemPath = new ItemPath();

            log.info("verifyResource() - "+getName()+" "+itemName+" not found. Creating new.");

            thisProxy = createResourceItem(itemName, ns, itemPath);
        }

        if (outcomes.size() == 0) 
            log.warn("verifyResource() - NO Outcome was found nothing stored for Item '{}' of type '{}' verion '{}'", itemName, getName(), version);

        // Verify/Import Outcomes, creating events and views as necessary
        for (Outcome newOutcome : outcomes) {
            status = checkToStoreOutcomeVersion(thisProxy, newOutcome, version, reset);

            log.info("checkToStoreOutcomeVersion() - {} item:{} schema:{} version:{} ", status.name(), thisProxy.getName(), newOutcome.getSchema().getName(), version);

            if (status != IDENTICAL || status != UNCHANGED) {
                // validate it, but not for kernel objects (ns == null) because those are to validate the rest
                if (ns != null) newOutcome.validateAndCheck();

                PredefinedStep.storeOutcomeEventAndViews(thisProxy.getPath(), newOutcome, version);

                CollectionArrayList cols = getCollections(itemName, version, newOutcome);

                for (Collection<?> col : cols.list) {
                    Gateway.getStorage().put(thisProxy.getPath(), col, null);
                    Gateway.getStorage().clearCache(thisProxy.getPath(), ClusterType.COLLECTION+"/"+col.getName());
                    col.setVersion(null);
                    Gateway.getStorage().put(thisProxy.getPath(), col, null);
                }
            }
        }

        Gateway.getStorage().commit(null);
        return modDomPath;
    }

    /**
     * Verify module property and location
     */
    private ItemProxy verifyPathAndModuleProperty(String ns, String itemName, ItemPath itemPath, DomainPath modDomPath, DomainPath path)
            throws Exception
    {
        LookupManager lookupManager = Gateway.getLookupManager();
        ItemProxy thisProxy = Gateway.getProxyManager().getProxy(path);

        if (itemPath != null && !path.getItemPath().equals(itemPath)) {
            String error = "Resource "+type+"/"+itemName+" should have path "+itemPath+" but was found with path "+path.getItemPath();
            log.error(error);
            throw new InvalidDataException(error);
        }

        if (itemPath == null) itemPath = path.getItemPath();

        String moduleName = (ns==null?"kernel":ns);
        String itemModule;
        try {
            itemModule = thisProxy.getProperty("Module");
            if (itemModule != null && !itemModule.equals("") && !itemModule.equals("null") && !moduleName.equals(itemModule)) {
                String error = "Module clash! Resource '"+itemName+"' included in module "+moduleName+" but is assigned to '"+itemModule + "'.";
                log.error(error);
                throw new InvalidDataException(error);
            }
        }
        catch (ObjectNotFoundException ex) {
            itemModule = "";
        }

        if (!modDomPath.equals(path)) {  // move item to module subtree
            log.info("Module item "+itemName+" found with path "+path.toString()+". Moving to "+modDomPath.toString());
            modDomPath.setItemPath(itemPath);

            if (!modDomPath.exists()) lookupManager.add(modDomPath);
            lookupManager.delete(path);
        }
        return thisProxy;
    }

    /**
     *
     */
    private Status checkToStoreOutcomeVersion(ItemProxy item, Outcome newOutcome, int version, boolean reset)
            throws PersistencyException, InvalidDataException, ObjectNotFoundException
    {
        Schema schema = newOutcome.getSchema();

        if (! item.checkViewpoint(schema.getName(), Integer.toString(version))) {
            return NEW;
        }

        Viewpoint currentData = item.getViewpoint(schema.getName(), Integer.toString(version));

        if (newOutcome.isIdentical(currentData.getOutcome())) {
            return IDENTICAL;
        }
        else {
            if (currentData.getEvent().getStepPath().equals("Bootstrap")) {
                return CHANGED;
            }
            else {
                // Use system property 'Module.reset' or 'Module.<namespace>.reset' to control if bootstrap should overwrite the resource
                if (reset) return OVERWRITTEN;
                else       return UNCHANGED;
            }
        }
    }

    /**
     * 
     * @param itemName
     * @param ns
     * @param itemPath
     * @return
     * @throws Exception
     */
    private ItemProxy createResourceItem(String itemName, String ns, ItemPath itemPath) throws Exception {
        // create props
        PropertyDescriptionList pdList = getPropDesc();
        PropertyArrayList props = new PropertyArrayList();
        LookupManager lookupManager = Gateway.getLookupManager();

        for (int i = 0; i < pdList.list.size(); i++) {
            PropertyDescription pd = pdList.list.get(i);

            String propName = pd.getName();
            String propVal  = pd.getDefaultValue();

            if (propName.equals(NAME.toString()))        propVal = itemName;
            else if (propName.equals(MODULE.toString())) propVal = (ns == null) ? "kernel" : ns;

            props.list.add(new Property(propName, propVal, pd.getIsMutable()));
        }

        CompositeActivity ca = new CompositeActivity();
        try {
            ca = (CompositeActivity) ((CompositeActivityDef)LocalObjectLoader.getActDef(getWorkflowName(), 0)).instantiate();
        }
        catch (ObjectNotFoundException ex) {
            log.error("Module resource workflow "+getWorkflowName()+" not found. Using empty.", ex);
        }

        Gateway.getCorbaServer().createItem(itemPath);
        lookupManager.add(itemPath);
        DomainPath newDomPath = getPath(itemName, ns);
        newDomPath.setItemPath(itemPath);
        lookupManager.add(newDomPath);
        ItemProxy newItemProxy = Gateway.getProxyManager().getProxy(itemPath);
        newItemProxy.initialise((AgentPath)SYSTEM_AGENT.getPath(), props, ca, null);
        return newItemProxy;
    }

    @Override
    public Status getResourceStatus() {
        return status;
    }
}
