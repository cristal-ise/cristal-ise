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
import static org.cristalise.kernel.process.resource.ResourceImportHandler.Status.IDENTICAL;
import static org.cristalise.kernel.process.resource.ResourceImportHandler.Status.NEW;
import static org.cristalise.kernel.process.resource.ResourceImportHandler.Status.OVERWRITTEN;
import static org.cristalise.kernel.process.resource.ResourceImportHandler.Status.SKIPPED;
import static org.cristalise.kernel.process.resource.ResourceImportHandler.Status.REMOVED;
import static org.cristalise.kernel.process.resource.ResourceImportHandler.Status.UPDATED;
import static org.cristalise.kernel.property.BuiltInItemProperties.MODULE;
import static org.cristalise.kernel.property.BuiltInItemProperties.NAME;
import static org.cristalise.kernel.security.BuiltInAuthc.SYSTEM_AGENT;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lifecycle.CompositeActivityDef;
import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep;
import org.cristalise.kernel.lifecycle.instance.predefined.item.CreateItemFromDescription;
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
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.ObjectProperties;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultResourceImportHandler implements ResourceImportHandler {

    BuiltInResources         type;
    DomainPath               typeRootPath;
    PropertyDescriptionList  props;

    String resourceChangeDetails = "";

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
    public Outcome getResourceOutcome(String name, String ns, String location, Integer version) 
            throws InvalidDataException, ObjectNotFoundException
    {
        String data = Gateway.getResource().getTextResource(ns, location);

        if (data == null) throw new ObjectNotFoundException("No data found for "+type.getSchemaName()+" "+name);

        return new Outcome(-1, data, LocalObjectLoader.getSchema(type.getSchemaName(), version));
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
    public DomainPath createResource(String ns, String itemName, int version, Outcome outcome, boolean reset, Object transactionKey)
            throws Exception
    {
        return verifyResource(ns, itemName, version, null, outcome, reset, transactionKey);
    }

    @Override
    public DomainPath importResource(String ns, String itemName, int version, ItemPath itemPath, String dataLocation, boolean reset, Object transactionKey)
            throws Exception
    {
        return verifyResource(ns, itemName, version, itemPath, getResourceOutcome(itemName, ns, dataLocation, version), reset, transactionKey);
    }

    @Override
    public DomainPath importResource(String ns, String itemName, int version, ItemPath itemPath, Outcome outcome, boolean reset, Object transactionKey)
            throws Exception
    {
        return verifyResource(ns, itemName, version, itemPath, outcome, reset, transactionKey);
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
    private DomainPath verifyResource(String ns, String itemName, int version, ItemPath itemPath, Outcome outcome, boolean reset, Object transactionKey) 
            throws Exception
    {
        if (outcome == null) {
            log.warn("verifyResource() - NO Outcome was found nothing stored for Item '{}' of type '{}' version '{}'", itemName, getName(), version);
            return null;
        }

        log.debug("verifyResource() - Item '{}' of type '{}' version '{}'", itemName, getName(), version);

        // Find or create Item for Resource
        ItemProxy thisProxy;
        DomainPath modDomPath = getPath(itemName, ns);

        if (modDomPath.exists()) {
            log.debug("verifyResource() - Found "+getName()+" "+itemName + ".");

            thisProxy = verifyPathAndModuleProperty(ns, itemName, itemPath, modDomPath, modDomPath, transactionKey);
        }
        else {
            log.debug("verifyResource() - "+getName()+" "+itemName+" not found. Creating new.");

            if (itemPath == null) itemPath = new ItemPath(); //itemPath can be hardcoded in the bootstrap for example
            thisProxy = createResourceItem(itemName, version, ns, itemPath, transactionKey);
        }

        // Verify/Import Outcome, creating events and views as necessary
        Status status = checkToStoreOutcomeVersion(thisProxy, outcome, version, reset, transactionKey);

        log.info("verifyResource() - Outcome {} of item:{} schema:{} version:{} ", status.name(), thisProxy.getName(), outcome.getSchema().getName(), version);

        if (status == NEW || status == UPDATED || status == OVERWRITTEN) {
            // validate it, but not for kernel objects (ns == null) because those are to validate the rest
            if (ns != null) outcome.validateAndCheck();

            PredefinedStep.storeOutcomeEventAndViews(thisProxy.getPath(), outcome, version, transactionKey);

            CollectionArrayList cols = getCollections(itemName, version, outcome);

            for (Collection<?> col : cols.list) {
                Gateway.getStorage().put(thisProxy.getPath(), col, transactionKey);
                Gateway.getStorage().clearCache(thisProxy.getPath(), ClusterType.COLLECTION+"/"+col.getName());
                col.setVersion(null);
                Gateway.getStorage().put(thisProxy.getPath(), col, transactionKey);
            }
        }
        else if (status == REMOVED) {
            // TODO implement
        }

        resourceChangeDetails = convertToResourceChangeDetails(itemName, version, outcome.getSchema(), status);

        if (log.isTraceEnabled()) {
            log.trace("verifyResource() - resourceChangeDetails:{}", resourceChangeDetails.replace("\n", "").replaceAll(">\\s*<", "><"));
        }

        return modDomPath;
    }

    /**
     * Verify module property and location
     */
    private ItemProxy verifyPathAndModuleProperty(String ns, String itemName, ItemPath itemPath, DomainPath modDomPath, DomainPath path, Object transactionKey)
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
     * TODO implement REMOVED
     */
    private Status checkToStoreOutcomeVersion(ItemProxy item, Outcome newOutcome, int version, boolean reset, Object transactionKey)
            throws PersistencyException, InvalidDataException, ObjectNotFoundException
    {
        Schema schema = newOutcome.getSchema();

        if (! item.checkViewpoint(schema.getName(), Integer.toString(version), transactionKey)) {
            return NEW;
        }

        Viewpoint currentData = item.getViewpoint(schema.getName(), Integer.toString(version), transactionKey);

        if (newOutcome.isIdentical(currentData.getOutcome())) {
            return IDENTICAL;
        }
        else {
            if (currentData.getEvent().getStepPath().equals("Bootstrap")) {
                return UPDATED;
            }
            else {
                // Use system property 'Module.reset' or 'Module.<namespace>.reset' to control if bootstrap should overwrite the resource
                if (reset) return OVERWRITTEN;
                else       return SKIPPED;
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
    private ItemProxy createResourceItem(String itemName, int version, String ns, ItemPath itemPath, Object transactionKey) throws Exception {
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

        CompositeActivity ca = null;

        try {
            ca = (CompositeActivity) ((CompositeActivityDef)LocalObjectLoader.getCompActDef(getWorkflowName(), version)).instantiate();
        }
        catch (ObjectNotFoundException ex) {
            // FIXME check if this could be a real error
        }

        Gateway.getCorbaServer().createItem(itemPath);
        lookupManager.add(itemPath);

        CreateItemFromDescription.storeItem((AgentPath)SYSTEM_AGENT.getPath(), itemPath, props, null, ca, null, null, transactionKey);

        DomainPath newDomPath = getPath(itemName, ns);
        newDomPath.setItemPath(itemPath);
        lookupManager.add(newDomPath);

        return Gateway.getProxyManager().getProxy(itemPath);
    }

    /**
     * Creates an XML fragment defined in Schema ModuleChanges struct ResourceChangeDetails
     * 
     * @param name of the resource item could be UUID
     * @param version of the resource Item
     * @param resourceChangesList the change list which was computed during verifyResource()
     * @return the xml fragment 
     * @throws IOException template file was not found
     */
    private String convertToResourceChangeDetails(String name, int version, Schema schema, Status status) throws IOException {
        String templ = FileStringUtility.url2String(ObjectProperties.class.getResource("resources/templates/ResourceChangeDetails_xsd.tmpl"));
        CompiledTemplate expr = TemplateCompiler.compileTemplate(templ);

        Map<Object, Object> vars = new HashMap<Object, Object>();

        vars.put("resourceName", StringEscapeUtils.escapeXml11(name));
        vars.put("resourceVersion", version);
        vars.put("schemaName", schema.getName());
        vars.put("changeType", status.name());

        return (String)TemplateRuntime.execute(expr, vars);
    }

    @Override
    public String getResourceChangeDetails() {
        return resourceChangeDetails;
    }
}
