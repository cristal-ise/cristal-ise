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
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.ABORTABLE;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.REPEAT_WHEN;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.STATE_MACHINE_NAME;
import static org.cristalise.kernel.process.resource.BuiltInResources.COMP_ACT_DESC_RESOURCE;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.collection.Dependency;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.graph.model.BuiltInVertexProperties;
import org.cristalise.kernel.graph.model.GraphModel;
import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.graph.model.GraphableVertex;
import org.cristalise.kernel.graph.model.TypeNameAndConstructionInfo;
import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lifecycle.instance.Next;
import org.cristalise.kernel.lifecycle.instance.WfVertex;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.w3c.dom.NodeList;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 */
@Slf4j
public class CompositeActivityDef extends ActivityDef {
    private ArrayList<ActivityDef> refChildActDef = new ArrayList<ActivityDef>();

    public CompositeActivityDef() {
        super();
        setBuiltInProperty(ABORTABLE, false);
        setBuiltInProperty(REPEAT_WHEN, false);
        setBuiltInProperty(STATE_MACHINE_NAME,  StateMachine.getDefaultStateMachine("Composite"));

        try {
            setChildrenGraphModel(new GraphModel(new LifecycleVertexOutlineCreator()));
        }
        catch (InvalidDataException e) {
        } // shouldn't happen with an empty one

        setIsComposite(true);
    }

    public ArrayList<ActivityDef> getRefChildActDef() {
        return refChildActDef;
    }

    public void setRefChildActDef(ArrayList<ActivityDef> refChildActDef) {
        this.refChildActDef = refChildActDef;
    }

    private final TypeNameAndConstructionInfo[] mVertexTypeNameAndConstructionInfo = {
            new TypeNameAndConstructionInfo("Activity",         "Atomic"),
            new TypeNameAndConstructionInfo("Local Activity",   "AtomicLocal"),
            new TypeNameAndConstructionInfo("Composite",        "Composite"),
            new TypeNameAndConstructionInfo("Sub-workflow",     "CompositeLocal"),
            new TypeNameAndConstructionInfo("AND Split",        "And"), 
            new TypeNameAndConstructionInfo("OR Split",         "Or"),
            new TypeNameAndConstructionInfo("XOR Split",        "XOr"),
            new TypeNameAndConstructionInfo("Join",             "Join"),
            new TypeNameAndConstructionInfo("Loop",             "Loop"),
    };

    private final TypeNameAndConstructionInfo[] mEdgeTypeNameAndConstructionInfo  = { new TypeNameAndConstructionInfo("Next Edge", "Next") };

    public TypeNameAndConstructionInfo[] getVertexTypeNameAndConstructionInfo() {
        return mVertexTypeNameAndConstructionInfo;
    }

    public TypeNameAndConstructionInfo[] getEdgeTypeNameAndConstructionInfo() {
        return mEdgeTypeNameAndConstructionInfo;
    }

    public NextDef addNextDef(WfVertexDef origin, WfVertexDef terminus) {
        NextDef returnNxt = new NextDef(origin, terminus);
        getChildrenGraphModel().addEdgeAndCreateId(returnNxt, origin, terminus);
        return returnNxt;
    }

    public ActivitySlotDef addExistingActivityDef(String name, ActivityDef actDef, GraphPoint point) throws InvalidDataException {
        changed = true;
        boolean newActDef = true;

        for (ActivityDef existingActDef : refChildActDef) {
            if (existingActDef.getName().equals(actDef.getName())) {
                if (existingActDef.getVersion().equals(actDef.getVersion())) {
                    actDef = existingActDef;
                    newActDef = false;
                    break;
                }
                else {
                    throw new InvalidDataException("Cannot use same activity def with different version in the same composite activity");
                }
            }
        }
        if (newActDef) refChildActDef.add(actDef);
        
        ActivitySlotDef child = new ActivitySlotDef(name, actDef);
        addChild(child, point);

        return child;
    }

    public ActivityDef addLocalActivityDef(String name, String type, GraphPoint point) {
        changed = true;
        ActivityDef child = type.startsWith("Composite") ? new CompositeActivityDef() : new ActivityDef();
        child.setName(name);
        child.setIsLayoutable(true);
        addChild(child, point);
        return child;
    }

    public WfVertexDef newChild(String Name, String Type, Integer version, GraphPoint location)
            throws ObjectNotFoundException, InvalidDataException
    {
        changed = true;
        boolean wasAdded = false;
        WfVertexDef child;
        
        if (Type.equals("Or"))        child = new OrSplitDef();
        else if (Type.equals("XOr"))  child = new XOrSplitDef();
        else if (Type.equals("And"))  child = new AndSplitDef();
        else if (Type.equals("Loop")) child = new LoopDef();
        else if (Type.equals("Join") || Type.equals("Route")) {
            child = new JoinDef();
            child.getProperties().put("Type", Type);
        }
        else if (Type.equals("Atomic") || Type.equals("Composite")) {
            ActivityDef act = Type.equals("Atomic") ? LocalObjectLoader.getElemActDef(Name, version) : LocalObjectLoader.getCompActDef(Name, version);
            child = addExistingActivityDef(act.getActName(), act, location);
            wasAdded = true;
        }
        else if (Type.equals("AtomicLocal") || Type.equals("CompositeLocal")) {
            child = addLocalActivityDef(Name, Type, location);
            wasAdded = true;
        }
        else {
            throw new InvalidDataException("Unknown child type: " + Type);
        }

        if(!wasAdded) addChild(child, location);

        log.debug("newChild() - Type:"+Type + " ID:" + child.getID() + " added to ID:" + this.getID());

        return child;
    }

    /**
     * 
     * @return CompositeActivity
     */
    @Override
    public WfVertex instantiate(TransactionKey transactionKey) throws ObjectNotFoundException, InvalidDataException {
        return instantiate(getName(), transactionKey);
    }

    @Override
    public WfVertex instantiate(String name, TransactionKey transactionKey) throws ObjectNotFoundException, InvalidDataException {
        CompositeActivity caInstance = new CompositeActivity();

        log.info("instantiate(name:"+name+") - Starting.");

        caInstance.setName(name);

        configureInstance(caInstance, transactionKey);

        if (getItemPath() != null) caInstance.setType(getItemID());

        caInstance.getChildrenGraphModel().setStartVertexId( getChildrenGraphModel().getStartVertexId()    );
        caInstance.getChildrenGraphModel().setVertices(      intantiateVertices(caInstance, transactionKey));
        caInstance.getChildrenGraphModel().setEdges(         instantiateEdges(caInstance)                  );
        caInstance.getChildrenGraphModel().setNextId(        getChildrenGraphModel().getNextId()           );

        caInstance.getChildrenGraphModel().resetVertexOutlines();

        propagateCollectionProperties(caInstance);

        return caInstance;
    }

    /**
     * Reading collections during configureInstance() the properties of CAInstance can be updated to
     * contain CastorHashMaps, which contain properties to be propagated to the Vertices of CAInstance.
     * 
     * @param caInstance the CompAct instance beeing instantiated
     * @throws InvalidDataException 
     */
    private void propagateCollectionProperties(CompositeActivity caInstance) throws InvalidDataException {
        //Propagate now properties to Vertices 
        CastorHashMap caProps = caInstance.getProperties();
        List<String> keysToDelete = new ArrayList<String>();

        for (Entry<String, Object> aCAProp: caProps.entrySet()) {
            if(aCAProp.getValue() instanceof CastorHashMap) {
                for (Vertex vertex : caInstance.getChildrenGraphModel().getVertices()) {
                    CastorHashMap propsToPropagate = (CastorHashMap)aCAProp.getValue();
                    BuiltInVertexProperties builtInProp = BuiltInVertexProperties.getValue(aCAProp.getKey());

                    if(builtInProp == null) {
                        ((GraphableVertex)vertex).updatePropertiesFromCollection(Integer.parseInt(aCAProp.getKey()), propsToPropagate);
                    }
                    else {
                        ((GraphableVertex)vertex).updatePropertiesFromCollection(builtInProp, propsToPropagate);
                    }
                }
                keysToDelete.add(aCAProp.getKey());
            }
        }

        for(String key : keysToDelete) caProps.remove(key);
    }

    /**
     * Loops through the edges of children graph model and calls their instantiate method
     * 
     * @param ca the parent CompositeActivity instance which will be set as a Parent
     * @return the instantiated array of edge instances called Next
     */
    public Next[] instantiateEdges(CompositeActivity ca) {
        Next[] nexts = new Next[getChildrenGraphModel().getEdges().length];
        
        for (int i = 0; i < getChildrenGraphModel().getEdges().length; i++) {
            NextDef nextDef = (NextDef) getChildrenGraphModel().getEdges()[i];

            nexts[i] = nextDef.instantiate();

            nexts[i].setParent(ca);
        }
        return nexts;
    }

    /**
     * Loops through the vertices of children graph model and calls their instantiate method
     * 
     * @param ca the parent CompositeActivity instance which will be set as a Parent
     * @return the instantiated array of WfVertex instances
     */
    public WfVertex[] intantiateVertices(CompositeActivity ca, TransactionKey transactionKey) throws ObjectNotFoundException, InvalidDataException {
        GraphableVertex[] vertexDefs = getLayoutableChildren();
        WfVertex[]        wfVertices = new WfVertex[vertexDefs.length];

        for (int i = 0; i < vertexDefs.length; i++) {
            WfVertexDef vertDef = (WfVertexDef) vertexDefs[i];

            wfVertices[i] = vertDef.instantiate(transactionKey);

            wfVertices[i].setParent(ca);
        }
        return wfVertices;
    }

    @Override
    public CollectionArrayList makeDescCollections(TransactionKey transactionKey) throws InvalidDataException, ObjectNotFoundException {
        CollectionArrayList retArr = super.makeDescCollections(transactionKey);
        retArr.put(makeActDefCollection(transactionKey));
        return retArr;
    }

    /**
     * Used in Script CompositeActivityDefCollSetter
     * 
     * @return the Dependency collection created from the list of child ActDefs of this class
     * @throws InvalidDataException there was a problem creating the collections
     */
    public Dependency makeActDefCollection(TransactionKey transactionKey) throws InvalidDataException {
        return makeDescCollection(ACTIVITY, transactionKey, refChildActDef.toArray(new ActivityDef[refChildActDef.size()]));
    }

    public ArrayList<ActivityDef> findRefActDefs(GraphModel graph, TransactionKey transactionKey) throws ObjectNotFoundException, InvalidDataException {
        ArrayList<ActivityDef> graphActDefs = new ArrayList<ActivityDef>();
        for (Vertex elem : graph.getVertices()) {
            if (elem instanceof ActivitySlotDef) {
                ActivitySlotDef actSlotDef = (ActivitySlotDef) elem;
                actSlotDef.setTheActivityDef(null); //enforce getTheActivityDef() to do the full search
                ActivityDef actDef = actSlotDef.getTheActivityDef(transactionKey);
                if (!graphActDefs.contains(actDef)) graphActDefs.add(actDef);
            }
        }
        return graphActDefs;
    }

    /**
     *
     * @return boolean
     */
    public boolean hasGoodNumberOfActivity() {
        int endingAct = 0;
        GraphableVertex[] graphableVertices = this.getLayoutableChildren();
        
        if (graphableVertices != null) for (GraphableVertex graphableVertice : graphableVertices) {
            WfVertexDef vertex = (WfVertexDef) graphableVertice;
            if (getChildrenGraphModel().getOutEdges(vertex).length == 0) endingAct++;
        }
        
        if (endingAct > 1) return false;
        
        return true;
    }

    /**
     * @see org.cristalise.kernel.graph.model.GraphableVertex#getPath()
     */
    @Override
    public String getPath() {
        if (getParent() == null) return getName();
        return super.getPath();
    }

    @Override
    public void setChildrenGraphModel(GraphModel childrenGraph) throws InvalidDataException {
        setChildrenGraphModel(childrenGraph, null);
    }

    //FIXME: castor should call this method with valid TransactionKey when doing unmarshall
    public void setChildrenGraphModel(GraphModel childrenGraph, TransactionKey transactionKey) throws InvalidDataException {
        super.setChildrenGraphModel(childrenGraph);
        childrenGraph.setVertexOutlineCreator(new LifecycleVertexOutlineCreator());
        
        try {
            setRefChildActDef(findRefActDefs(childrenGraph, transactionKey));
        }
        catch (ObjectNotFoundException e) {
            throw new InvalidDataException(e.getMessage());
        }
    }

    @Deprecated
    public String[] getCastorNonLayoutableChildren() {
        return new String[0];
    }

    @Deprecated
    public void setCastorNonLayoutableChildren(String[] dummy) {
    }

    @Override
    public boolean verify() {
        boolean err = super.verify();
        GraphableVertex[] vChildren = getChildren();
        
        for (int i = 0; i < vChildren.length; i++) {
            WfVertexDef wfvChild = (WfVertexDef) vChildren[i];
            if (!(wfvChild.verify())) {
                mErrors.add(wfvChild.getName() + ": " + wfvChild.getErrors());
                err = false;
            }
        }
        return err;
    }

    @Override
    public void export(Writer imports, File dir, boolean shallow) throws InvalidDataException, ObjectNotFoundException, IOException {
        // rebuild the child refs in case any slots have been removed
        setRefChildActDef(findRefActDefs(getChildrenGraphModel(), null));

        // TODO: property include routing scripts in another dependency collection

        if (!shallow) {
            //export child activitz defs, routing scripts and schemas
            for (GraphableVertex vert: getChildren()) {
                if (vert instanceof AndSplitDef) {
                    try {
                        ((AndSplitDef) vert).getRoutingScript(null).export(imports, dir, shallow);
                    }
                    catch (ObjectNotFoundException ex) {}
                }
                else if (vert instanceof ActivitySlotDef) {
                    ActivityDef refAct = ((ActivitySlotDef) vert).getTheActivityDef(null);
                    refAct.export(imports, dir, shallow);
                }
            }
        }

        String tc = COMP_ACT_DESC_RESOURCE.getTypeCode();

        try {
            NodeList nodeList = null;
            Outcome outcome = null;
            // export marshalled compAct
            String compactXML = Gateway.getMarshaller().marshall(this);
            if (Gateway.getProperties().getBoolean("Export.replaceActivitySlotDefUUIDWithName", false)) {
                outcome = new Outcome(compactXML);
                nodeList = outcome.getNodesByXPath("/CompositeActivityDef/childrenGraphModel/GraphModelCastorData/ActivitySlotDef/activityDef/text()");
                for (int i = 0; i < nodeList.getLength(); i++) {
                    try {
                        String syskey = nodeList.item(i).getNodeValue();
                        if (StringUtils.isNotBlank(syskey) && ItemPath.isUUID(syskey)) {
                            ItemPath itemPath = Gateway.getLookup().getItemPath(syskey);
                            ItemProxy itemProxy = Gateway.getProxyManager().getProxy(itemPath);
                            nodeList.item(i).setNodeValue(itemProxy.getName());
                            break;
                        }
                        else if (StringUtils.isNotBlank(syskey)) {
                            log.debug("export(name:{}) - syskey:{} was not replaced - not null & not UUID", getActName(), syskey);
                            break;
                        }
                    }
                    catch(Exception e) {
                        log.error("Cannot find item with UIID: "+nodeList.item(i).getNodeValue(), e);
                        throw new ObjectNotFoundException("Cannot find item with UIID: "+nodeList.item(i).getNodeValue());
                    }
                }
                compactXML = outcome.getData(true);
            }            
            FileStringUtility.string2File(new File(new File(dir, tc), getActName() + (getVersion() == null ? "" : "_" + getVersion()) + ".xml"), compactXML);
        }
        catch (Exception e) {
            log.error("Couldn't marshall composite activity def " + getActName(), e);
            throw new InvalidDataException("Couldn't marshall composite activity def " + getActName());
        }

        if (imports != null) {
            imports.write("<Workflow " + getExportAttributes(tc) + ">" + getExportCollections());

            for (ActivityDef childActDef : refChildActDef) {
                imports.write("<Activity name=\"" + childActDef.getActName() + "\" id=\"" + childActDef.getItemID() + "\" version=\"" + childActDef.getVersion() + "\"/>");
            }
            imports.write("</Workflow>\n");
        }
    }
}
