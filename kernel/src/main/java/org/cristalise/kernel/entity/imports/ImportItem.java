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
package org.cristalise.kernel.entity.imports;

import static org.cristalise.kernel.persistency.ClusterType.VIEWPOINT;
import static org.cristalise.kernel.property.BuiltInItemProperties.NAME;
import static org.cristalise.kernel.security.BuiltInAuthc.ADMIN_ROLE;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.collection.Aggregation;
import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.collection.Dependency;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.entity.proxy.ProxyManager;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.events.History;
import org.cristalise.kernel.lifecycle.CompositeActivityDef;
import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.lifecycle.instance.predefined.CreateItemFromDescription;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.module.ModuleImport;
import org.cristalise.kernel.process.resource.BuiltInResources;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyArrayList;
import org.cristalise.kernel.utils.DescriptionObject;
import org.cristalise.kernel.utils.LocalObjectLoader;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Complete Structure for new Item created by different bootstrap uses cases including testing
 */
@Getter @Setter @Slf4j
public class ImportItem extends ModuleImport implements DescriptionObject {

    protected Integer version;

    protected String  initialPath;
    protected String  workflow;
    protected Integer workflowVer;

    protected PropertyArrayList properties = new PropertyArrayList();

    protected ArrayList<ImportAggregation> aggregationList = new ArrayList<ImportAggregation>();
    protected ArrayList<ImportDependency>  dependencyList  = new ArrayList<ImportDependency>();
    protected ArrayList<ImportOutcome>     outcomes        = new ArrayList<ImportOutcome>();

    /**
     * Alternative way to provide workflow definition.
     * It is not marshallable by castor, therefore cannot be set in module.xml
     */
    protected CompositeActivityDef compActDef;

    /**
     * Alternative way to provide workflow.
     * It is not marshalable by castor, therefore cannot be set in module.xml
     */
    protected Workflow wf;

    public ImportItem() {}

    public ImportItem(String ns, String name, Integer version, String initialPath, ItemPath itemPath, String wf, Integer wfVer) {
        setNamespace(ns);
        setName(name);
        setVersion(version);
        setItemPath(itemPath);
        setInitialPath(initialPath);
        setWorkflow(wf);
        setWorkflowVer(wfVer);

        compActDef = null;
        wf = null;
    }

    public ImportItem(String ns, String name, String initialPath, ItemPath itemPath, String wf, Integer wfVer) {
        this(ns, name, null, initialPath, itemPath, wf, wfVer);
    }

    /**
     * Constructor with mandatory fields
     * 
     * @param name
     * @param initialPath
     * @param itemPath
     * @param wf
     */
    public ImportItem(String name, String initialPath, ItemPath itemPath, String wf) {
        this(null, name, null, initialPath, itemPath, wf, null);
    }

    /**
     * 
     */
    @Override
    public DomainPath getDomainPath() {
        if (domainPath == null) domainPath = new DomainPath(new DomainPath(initialPath), name);
        return domainPath;
    }

    public boolean exists(TransactionKey transactionKey) {
        return getDomainPath().exists(transactionKey);
    }

    @Override
    public ItemPath getItemPath() {
        return getItemPath(null);
    }

    public ItemProxy getProxy() {
        try {
            return ProxyManager.getProxy(getItemPath());
        } catch (ObjectNotFoundException e) {
            return null;
        }
    }

    /**
     * Get the workflow version. Default workflow version is 0 if not given
     * @return workflow version (default 0)
     */
    public int getWfVersion() {
        return workflowVer == null ? 0 : workflowVer;
    }

    /**
     * Returns ItemPath if it was already set. If it was not set yet, it checks 
     * DomianPath.target as well.
     */
    @Override
    public ItemPath getItemPath(TransactionKey transactionKey) {
        if (itemPath == null) {
            getDomainPath();
            if (domainPath.exists(transactionKey)) {
                try {
                    itemPath = domainPath.getItemPath(transactionKey);
                }
                catch (ObjectNotFoundException ex) {}
            }
        }

        return itemPath;
    }

    @Override
    public void setNamespace(String ns) {
        super.setNamespace(ns);
        if (initialPath == null) initialPath = "/desc/" + ns;
    }

    @Override
    public void setName(String name) {
        super.setName(name);
    }

    /**
     * Returns or creates the itemPath 
     * 
     * @param transactionKey
     * @throws ObjectNotFoundException
     * @throws CannotManageException
     * @throws ObjectAlreadyExistsException
     * @throws ObjectCannotBeUpdated
     */
    private void setOrCreateItemPath(TransactionKey transactionKey)
            throws ObjectNotFoundException, CannotManageException, ObjectAlreadyExistsException, ObjectCannotBeUpdated
    {
        getItemPath(transactionKey);

        if (itemPath != null && itemPath.exists(transactionKey)) {
            log.info("getOrCreateItemPath() - Verifying module item {} at {}", itemPath, domainPath);
            isNewItem = false;
        }
        else {
            if (itemPath == null) itemPath = new ItemPath();

            log.info("getOrCreateItemPath() - Creating module item {} at {}", itemPath, domainPath);
            Gateway.getLookupManager().add(itemPath, transactionKey);
        }
    }

    /**
     *
     */
    @Override
    public Path create(AgentPath agentPath, boolean reset, TransactionKey transactionKey)
            throws InvalidDataException, ObjectCannotBeUpdated, ObjectNotFoundException,
            CannotManageException, ObjectAlreadyExistsException, InvalidCollectionModification, PersistencyException
    {
        setOrCreateItemPath(transactionKey);

        log.info("create() - path:{}", domainPath);

        if (domainPath.exists(transactionKey)) {
            ItemPath domIp = domainPath.getItemPath(transactionKey);
            if (! domIp.equals(itemPath)) {
                throw new CannotManageException("Item "+domainPath+" was found with the wrong itemPath ("+domIp+" vs "+itemPath+")");
            }
        }
        else
            isDOMPathExists = false;

        // (re)initialise the new item with properties, workflow and collections
        try {
            CreateItemFromDescription.storeItem(
                    agentPath, 
                    itemPath,
                    createItemProperties(),
                    createCollections(transactionKey),
                    createCompositeActivity(transactionKey),
                    null, //initViewpoint
                    null, //initOutcomeString
                    transactionKey);
        }
        catch (Exception ex) {
            log.error("Error initialising new item:{}", domainPath, ex);

            if (isNewItem) Gateway.getLookupManager().delete(itemPath, transactionKey);

            throw new CannotManageException("Problem initialising new item:"+domainPath+". See server log:" + ex.getMessage());
        }

        History hist = new History(itemPath, transactionKey);

        // import outcomes
        for (ImportOutcome thisOutcome : outcomes) {
            String outcomeData = thisOutcome.getData(ns);

            // load schema and state machine
            Schema schema = LocalObjectLoader.getSchema(thisOutcome.schema, thisOutcome.version, transactionKey);

            // parse new outcome and validate
            Outcome newOutcome = new Outcome(-1, outcomeData, schema);
            newOutcome.validateAndCheck();

            Viewpoint impView;
            try {
                impView = (Viewpoint) Gateway.getStorage().get(itemPath, VIEWPOINT + "/" + thisOutcome.schema + "/" + thisOutcome.viewname, transactionKey);

                if (newOutcome.isIdentical(impView.getOutcome())) {
                    log.debug("create() - View "+thisOutcome.schema+"/"+thisOutcome.viewname+" in "+ns+"/"+name+" identical, no update required");
                    continue;
                }
                else {
                    log.info("create() - Difference found in view "+thisOutcome.schema+"/"+thisOutcome.viewname+" in "+ns+"/"+name);

                    if (!reset && !impView.getEvent().getStepPath().equals("Import")) {
                        log.info("create() - Last edit was not done by import, and reset not requested. Not overwriting.");
                        continue;
                    }
                }
            }
            catch (ObjectNotFoundException ex) {
                log.info("create() - View "+thisOutcome.schema+"/"+thisOutcome.viewname+" not found in "+ns+"/"+name+". Creating.");
                impView = new Viewpoint(itemPath, schema, thisOutcome.viewname, -1);
            }

            // write new view/outcome/event
            Event newEvent = hist.addEvent(
                    agentPath, ADMIN_ROLE.getName(), "Import", "Import", "Import", schema, 
                    LocalObjectLoader.getStateMachine("PredefinedStep", 0, transactionKey), PredefinedStep.DONE, thisOutcome.viewname);
            newOutcome.setID(newEvent.getID());
            impView.setEventId(newEvent.getID());

            Gateway.getStorage().put(itemPath, newOutcome, transactionKey);
            Gateway.getStorage().put(itemPath, impView, transactionKey);
        }

        if (!isDOMPathExists) {
            domainPath.setItemPath(itemPath);
            Gateway.getLookupManager().add(domainPath, transactionKey);
        }

        return domainPath;
    }

    /**
     *
     * @return
     */
    protected PropertyArrayList createItemProperties() {
        if (!properties.contains(NAME.getName()))    properties.put(new Property(NAME, name, true));

        return properties;
    }

    /**
     * This method enables to use ImportItem in different bootstrap uses cases (e.g. testing)
     *
     * @return the the domain workflow of the Item
     * @throws ObjectNotFoundException
     * @throws InvalidDataException
     */
    protected CompositeActivity createCompositeActivity(TransactionKey transactionKey) throws ObjectNotFoundException, InvalidDataException {
        if (wf != null) {
            log.info("createCompositeActivity() - use Workflow instance");
            return (CompositeActivity) wf.search("workflow/domain");
        }
        else {
            if (compActDef == null) {
                if (StringUtils.isNotBlank(workflow)) {
                    compActDef = LocalObjectLoader.getCompActDef(workflow, getWfVersion(), transactionKey);
                }
                else {
                    log.warn("createCompositeActivity() - NO Workflow was set for domainPath:"+domainPath);
                    compActDef = LocalObjectLoader.getCompActDef("NoWorkflow", getWfVersion(), transactionKey);
                }
            }
            return (CompositeActivity) compActDef.instantiate(transactionKey);
        }
    }

    /**
     *
     * @param transactionKey 
     * @return
     * @throws InvalidCollectionModification
     * @throws ObjectNotFoundException
     * @throws ObjectAlreadyExistsException
     */
    protected CollectionArrayList createCollections(TransactionKey transactionKey)
            throws InvalidCollectionModification, ObjectNotFoundException, ObjectAlreadyExistsException
    {
        CollectionArrayList colls = new CollectionArrayList();

        for (ImportDependency element : dependencyList) {
            Dependency newDep = element.create(transactionKey);
            colls.put(newDep);
        }

        for (ImportAggregation element : aggregationList) {
            Aggregation newAgg = element.create(transactionKey);
            colls.put(newAgg);
        }

        log.info("createCollections() - name:{} number of colls:{}", name, colls.list.size());

        return colls;
    }

    @Override
    public String getItemID() {
        return getID();
    }

    @Override
    public CollectionArrayList makeDescCollections(TransactionKey transactionKey) throws InvalidDataException, ObjectNotFoundException {
        return new CollectionArrayList();
    }

    @Override
    public BuiltInResources getResourceType() {
        return BuiltInResources.ITEM_DESC_RESOURCE;
    }

    @Override
    public String toString() {
        return "ImportItem(name:"+name+" version:"+version+" status:"+resourceChangeStatus+")";
    }

    /**
     * Method required to be backward compatible with castor marshalling. check issue #518
     * @return
     */
    public ArrayList<Property> getProperties() {
        return properties.list;
    }
}
