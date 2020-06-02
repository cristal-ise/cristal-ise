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

import static org.cristalise.kernel.property.BuiltInItemProperties.CREATOR;
import static org.cristalise.kernel.property.BuiltInItemProperties.NAME;
import static org.cristalise.kernel.security.BuiltInAuthc.ADMIN_ROLE;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
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
import org.cristalise.kernel.entity.TraceableEntity;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.events.History;
import org.cristalise.kernel.lifecycle.CompositeActivityDef;
import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.module.ModuleImport;
import org.cristalise.kernel.process.resource.BuiltInResources;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyArrayList;
import org.cristalise.kernel.utils.DescriptionObject;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.LocalObjectLoader;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Complete Structure for new Item created by different bootstrap uses cases including testing
 */
@Getter @Setter @Slf4j
public class ImportItem extends ModuleImport implements DescriptionObject {

    protected Integer version = 0;

    protected String  initialPath;
    protected String  workflow;
    protected Integer workflowVer;

    protected ArrayList<Property> properties = new ArrayList<Property>();

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
     * It is not marshallable by castor, therefore cannot be set in module.xml
     */
    protected Workflow wf;

    public ImportItem() {}

    public ImportItem(String ns, String name, String initialPath, ItemPath itemPath, String wf, int wfVer) {
        setNamespace(ns);
        setName(name);
        setItemPath(itemPath);
        setInitialPath(initialPath);
        setWorkflow(wf);
        setWorkflowVer(wfVer);

        compActDef = null;
        wf = null;
    }

    /**
     * Try to find ItemPath if already exists. If not create new one.
     */
    @Override
    public ItemPath getItemPath() {
        if (itemPath == null) {
            DomainPath existingItem = new DomainPath(initialPath + "/" + name);
            if (existingItem.exists()) {
                try {
                    itemPath = existingItem.getItemPath();
                }
                catch (ObjectNotFoundException ex) {}
            }
        }
        if (itemPath == null) itemPath = new ItemPath();

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
     *
     * @return
     * @throws ObjectNotFoundException
     * @throws CannotManageException
     * @throws ObjectAlreadyExistsException
     * @throws ObjectCannotBeUpdated
     */
    private TraceableEntity getTraceableEntitiy()
            throws ObjectNotFoundException, CannotManageException, ObjectAlreadyExistsException, ObjectCannotBeUpdated
    {
        TraceableEntity newItem;
        ItemPath ip = getItemPath();

        if (ip.exists()) {
            log.info("getTraceableEntitiy() - Verifying module item "+domainPath+" at "+ip);
            newItem = Gateway.getCorbaServer().getItem(getItemPath());
            isNewItem = false;
        }
        else {
            log.info("getTraceableEntitiy() - Creating module item "+ip+" at "+domainPath);
            newItem = Gateway.getCorbaServer().createItem(ip);
            Gateway.getLookupManager().add(ip);
        }
        return newItem;
    }

    /**
     *
     */
    @Override
    public Path create(AgentPath agentPath, boolean reset)
            throws InvalidDataException, ObjectCannotBeUpdated, ObjectNotFoundException,
            CannotManageException, ObjectAlreadyExistsException, InvalidCollectionModification, PersistencyException
    {
        domainPath = new DomainPath(new DomainPath(initialPath), name);

        log.info("create() - path:{}", domainPath);

        if (domainPath.exists()) {
            ItemPath domItem = domainPath.getItemPath();
            if (!getItemPath().equals(domItem)) {
                throw new CannotManageException("Item "+domainPath+" was found with the wrong itemPath ("+domainPath.getItemPath()+" vs "+getItemPath()+")");
            }
        }
        else isDOMPathExists = false;

        TraceableEntity newItem = getTraceableEntitiy();

        // (re)initialise the new item with properties, workflow and collections
        try {
            newItem.initialise( 
                    agentPath.getSystemKey(),
                    Gateway.getMarshaller().marshall(createItemProperties()),
                    Gateway.getMarshaller().marshall(createCompositeActivity()),
                    Gateway.getMarshaller().marshall(createCollections()),
                    "", ""
                    );
        }
        catch (Exception ex) {
            log.error("Error initialising new item " + ns + "/" + name, ex);

            if (isNewItem) Gateway.getLookupManager().delete(itemPath);

            throw new CannotManageException("Problem initialising new item. See server log:" + ex.getMessage());
        }

        History hist = new History(getItemPath(), null);

        // import outcomes
        for (ImportOutcome thisOutcome : outcomes) {
            String outcomeData = thisOutcome.getData(ns);

            // load schema and state machine
            Schema schema = LocalObjectLoader.getSchema(thisOutcome.schema, thisOutcome.version);

            // parse new outcome and validate
            Outcome newOutcome = new Outcome(-1, outcomeData, schema);
            newOutcome.validateAndCheck();

            Viewpoint impView;
            try {
                impView = (Viewpoint) Gateway.getStorage().get(getItemPath(), ClusterType.VIEWPOINT + "/" + thisOutcome.schema + "/" + thisOutcome.viewname, null);

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
                impView = new Viewpoint(getItemPath(), schema, thisOutcome.viewname, -1);
            }

            // write new view/outcome/event
            Event newEvent = hist.addEvent(
                    agentPath, null, ADMIN_ROLE.getName(), "Import", "Import", "Import", schema, 
                    LocalObjectLoader.getStateMachine("PredefinedStep", 0), PredefinedStep.DONE, thisOutcome.viewname);
            newOutcome.setID(newEvent.getID());
            impView.setEventId(newEvent.getID());

            Gateway.getStorage().put(getItemPath(), newOutcome, null);
            Gateway.getStorage().put(getItemPath(), impView, null);
        }

        // register domain path (before collections in case of recursive collections)
        if (!isDOMPathExists) {
            domainPath.setItemPath(getItemPath());
            Gateway.getLookupManager().add(domainPath);
        }

        return domainPath;
    }

    /**
     *
     * @return
     */
    protected PropertyArrayList createItemProperties() {
        properties.add(new Property(NAME, name, true));
        properties.add(new Property(CREATOR, "bootstrap", true));

        return new PropertyArrayList(properties);
    }

    /**
     * This method enables to use ImportItem in different bootstrap uses cases (e.g. testing)
     *
     * @return the the domain workflow of the Item
     * @throws ObjectNotFoundException
     * @throws InvalidDataException
     */
    protected CompositeActivity createCompositeActivity() throws ObjectNotFoundException, InvalidDataException {
        if (wf != null) {
            return (CompositeActivity) wf.search("workflow/domain");
        }
        else {
            if (compActDef == null) {
                // default workflow version is 0 if not given
                if (StringUtils.isNotBlank(workflow)) {
                    compActDef = (CompositeActivityDef) LocalObjectLoader.getActDef(workflow, workflowVer == null ? 0 : workflowVer);
                }
                else {
                    log.warn("createCompositeActivity() - NO Workflow was set for domainPath:"+domainPath);
                    compActDef = (CompositeActivityDef) LocalObjectLoader.getActDef("NoWorkflow", workflowVer == null ? 0 : workflowVer);
                }
            }
        }
        return (CompositeActivity) compActDef.instantiate();
    }

    /**
     *
     * @return
     * @throws InvalidCollectionModification
     * @throws ObjectNotFoundException
     * @throws ObjectAlreadyExistsException
     */
    protected CollectionArrayList createCollections()
            throws InvalidCollectionModification, ObjectNotFoundException, ObjectAlreadyExistsException
    {
        CollectionArrayList colls = new CollectionArrayList();

        for (ImportDependency element : dependencyList) {
            Dependency newDep = element.create();
            colls.put(newDep);
        }

        for (ImportAggregation element : aggregationList) {
            Aggregation newAgg = element.create();
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
    public CollectionArrayList makeDescCollections() throws InvalidDataException, ObjectNotFoundException {
        return new CollectionArrayList();
    }

    @Override
    public void export(Writer imports, File dir, boolean shallow) throws InvalidDataException, ObjectNotFoundException, IOException {
        String xml;
        String typeCode = BuiltInResources.ITEM_DESC_RESOURCE.getTypeCode();
        String fileName = getName() + (getVersion() == null ? "" : "_" + getVersion()) + ".xml";

        try {
            xml = Gateway.getMarshaller().marshall(this);
        }
        catch (Exception e) {
            log.error("Couldn't marshall name:" + getName(), e);
            throw new InvalidDataException("Couldn't marshall name:" + getName());
        }

        FileStringUtility.string2File(new File(new File(dir, typeCode), fileName), xml);

        if (imports == null) return;

        if (Gateway.getProperties().getBoolean("Resource.useOldImportFormat", false)) {
            imports.write("<Resource "
                    + "name='" + getName() + "' "
                    + (getItemPath() == null ? "" : "id='"      + getItemID()  + "' ")
                    + (getVersion()  == null ? "" : "version='" + getVersion() + "' ")
                    + "type='" + typeCode + "'>boot/" + typeCode + "/" + fileName
                    + "</Resource>\n");
        }
        else {
            imports.write("<ItemResource "
                    + "name='" + getName() + "' "
                    + (getItemPath() == null ? "" : "id='"      + getItemID()  + "' ")
                    + (getVersion()  == null ? "" : "version='" + getVersion() + "'")
                    + "/>\n");
        }
    }
}
