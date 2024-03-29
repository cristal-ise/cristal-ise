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

import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.graph.model.GraphModel;
import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.graph.model.TypeNameAndConstructionInfo;
import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.CastorHashMap;

import lombok.extern.slf4j.Slf4j;

/**
 * A Collection with a graph layout
 */
@Slf4j
abstract public class Aggregation extends Collection<AggregationMember> {

    protected GraphModel mLayout = new GraphModel(new AggregationVertexOutlineCreator());

    private final TypeNameAndConstructionInfo[] mVertexTypeNameAndConstructionInfo = {
            new TypeNameAndConstructionInfo("Slot", "AggregationMember")
    };

    public Aggregation() {
        setName("Aggregation");
    }

    public Aggregation(String name) {
        setName(name);
    }

    public Aggregation(String name, Integer version) {
        setName(name);
        setVersion(version);
    }

    public GraphModel getLayout() {
        return mLayout;
    }

    public void setLayout(GraphModel layout) {
        mLayout = layout;
        layout.setVertexOutlineCreator(new AggregationVertexOutlineCreator());
    }

    public TypeNameAndConstructionInfo[] getVertexTypeNameAndConstructionInfo() {
        return mVertexTypeNameAndConstructionInfo;
    }

    public boolean exists(ItemPath itemPath) {
        for (int i = 0; i < size(); i++) {
            AggregationMember element = mMembers.list.get(i);
            if (element.getItemPath().equals(itemPath))
                return true;
        }
        return false;
    }

    public AggregationMember getMemberPair(int vertexID) {
        for (int i = 0; i < size(); i++) {
            AggregationMember element = mMembers.list.get(i);
            if (element.getID() == vertexID)
                return element;
        }
        return null;
    }

    public AggregationMember addSlot(CastorHashMap props, String classProps, GraphPoint location, int w, int h) {

        // Default geometry if not present
        if (location == null) location = new GraphPoint(100, 100 * getCounter());
        if (w < 0) w = 20;
        if (h < 0) h = 20;

        // Create new member object
        AggregationMember aggMem = new AggregationMember();
        aggMem.setProperties(props);
        aggMem.setClassProps(classProps);
        // create vertex
        Vertex vertex = new Vertex();
        vertex.setHeight(h);
        vertex.setWidth(w);
        mLayout.addVertexAndCreateId(vertex, location);
        aggMem.setCollection(this);
        aggMem.setID(vertex.getID());
        aggMem.setIsLayoutable(true);

        mMembers.list.add(aggMem);
        log.debug("addSlot() - new slot linked to vertexid " + vertex.getID());
        return aggMem;
    }

    public AggregationMember addMember(ItemPath itemPath, CastorHashMap props, String classProps, GraphPoint location, int w, int h, TransactionKey transactionKey)
            throws InvalidCollectionModification, ObjectAlreadyExistsException
    {
        AggregationMember aggMem = addSlot(props, classProps, location, w, h);
        if (itemPath != null) { // some clients use this method when not setting a member
            aggMem.assignItem(itemPath, transactionKey);
            aggMem.setIsComposite(getIsComposite(itemPath, getName(), transactionKey));
        }
        log.debug("addMember(" + itemPath + ") - assigned to new slot " + aggMem.getID());
        return aggMem;
    }

    @Override
    public AggregationMember addMember(ItemPath itemPath, CastorHashMap props, String classProps, TransactionKey transactionKey)
            throws InvalidCollectionModification, ObjectAlreadyExistsException
    {
        return addMember(itemPath, props, classProps, null, -1, -1, transactionKey);
    }

    public AggregationMember addMember(CastorHashMap props, String classProps, GraphPoint location, int w, int h, TransactionKey transactionKey)
            throws InvalidCollectionModification
    {
        try {
            return addMember(null, props, classProps, location, w, h, transactionKey);
        }
        catch (ObjectAlreadyExistsException e) { // not assigning an item so this won't happen
            return null;
        }
    }

    public AggregationMember addSlot(CastorHashMap props, String classProps) {
        return addSlot(props, classProps, null, -1, -1);
    }

    @Override
    public AggregationMember removeMember(int memberId) throws ObjectNotFoundException {
        for (AggregationMember element : mMembers.list) {
            if (element.getID() == memberId) {
                element.clearItem();
                mLayout.removeVertex(getLayout().getVertexById(memberId));
                return element;
            }
        }
        throw new ObjectNotFoundException("Member " + memberId + " not found");
    }

    static public boolean getIsComposite(ItemPath itemPath, String name, TransactionKey transactionKey) {
        if (itemPath == null) return false;
        try {
            for (String collName : Gateway.getStorage().getClusterContents(itemPath, ClusterType.COLLECTION, transactionKey)) {
                if (name == null || name.equals(collName)) return true;
            }
        }
        catch (PersistencyException e) {
            return false;
        }
        return false;
    }
}
