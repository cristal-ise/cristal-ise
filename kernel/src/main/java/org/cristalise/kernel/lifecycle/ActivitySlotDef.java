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
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.VERSION;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.graph.traversal.GraphTraversal;
import org.cristalise.kernel.lifecycle.instance.Activity;
import org.cristalise.kernel.lifecycle.instance.WfVertex;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.utils.DescriptionObject;
import org.cristalise.kernel.utils.KeyValuePair;
import org.cristalise.kernel.utils.LocalObjectLoader;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ActivitySlotDef extends WfVertexDef {
    private String      activityDef;
    private ActivityDef theActivityDef;

    public ActivitySlotDef() {
    }

    /**
     * @see java.lang.Object#Object()
     */
    public ActivitySlotDef(String name, ActivityDef actDef) {
        setName(name);
        setTheActivityDef(actDef);
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        setBuiltInProperty(NAME, name);
    }

    /**
     * Method setActivityDef.
     *
     * @param oActivityDef
     *            the name or UUID of the ActivityDef Item
     */
    public void setActivityDef(String oActivityDef) {
        activityDef = oActivityDef;
        theActivityDef = null;
    }

    /**
     * Method getActivityDef.
     *
     * @return String
     */
    public String getActivityDef() {
        return activityDef;
    }

    public ActivityDef getTheActivityDef(TransactionKey transactionKey) throws ObjectNotFoundException, InvalidDataException {
        if (theActivityDef == null) {
            try {
                log.debug("getTheActivityDef() - try to load from item desc collection of actDef:" + getName());
                DescriptionObject[] parentActDefs = ((CompositeActivityDef) getParent()).getBuiltInCollectionResource(ACTIVITY, transactionKey);
                for (DescriptionObject thisActDef : parentActDefs) {
                    String childUUID = thisActDef.getItemID();
                    log.debug("getTheActivityDef() - Collection childUUID:" + childUUID + " of actDef:" + getName());
                    if (childUUID.equals(getActivityDef()) || thisActDef.getName().equals(getActivityDef())) {
                        ActivityDef currentActDef = (ActivityDef) thisActDef;
                        Integer requiredVersion = deriveVersionNumber(getBuiltInProperty(VERSION));

                        if (currentActDef.getVersion() != requiredVersion) // collection indicated a different version - get the right one
                            setTheActivityDef(LocalObjectLoader.getActDef(childUUID, requiredVersion, transactionKey));
                        else // use the existing one
                            setTheActivityDef(currentActDef);
                        break;
                    }
                }
            }
            catch (ObjectNotFoundException ex) {} // old def with no collection

            if (theActivityDef == null) { // try to load from property
                log.debug("getTheActivityDef() - try to load from property of actDef:" + getName());
                Integer version = deriveVersionNumber(getBuiltInProperty(VERSION));

                if (version == null) throw new InvalidDataException("No version defined in ActivityDefSlot " + getName());

                setTheActivityDef(LocalObjectLoader.getActDef(getActivityDef(), version, transactionKey));
            }
        }

        return theActivityDef;
    }

    public void setTheActivityDef(ActivityDef actDef) {
        theActivityDef = actDef;

        if (actDef != null) {
            activityDef = actDef.getItemID();
            if (StringUtils.isBlank(activityDef)) activityDef = actDef.getName();
            setBuiltInProperty(VERSION, actDef.getVersion());

            if (actDef instanceof CompositeActivityDef) mIsComposite = true;

            log.debug("setTheActivityDef() - name:" + getName() + " UUID:" + activityDef);
        }
    }

    /**
     * @see org.cristalise.kernel.lifecycle.WfVertexDef#verify()
     */
    /** launch the verification of the ActivityDef */
    @Override
    public boolean verify() {
        mErrors.removeAllElements();
        boolean err = true;
        int nbInEdgres = getInEdges().length;
        int nbOutEdges = getOutEdges().length;

        if (nbInEdgres == 0 && this.getID() != getParent().getChildrenGraphModel().getStartVertexId()) {
            mErrors.add("Unreachable");
            err = false;
        }
        if (nbInEdgres > 1) {
            mErrors.add("Bad nb of previous");
            err = false;
        }
        if (nbOutEdges > 1) {
            mErrors.add("too many next");
            err = false;
        }
        if (nbOutEdges == 0) {
            if (!((CompositeActivityDef) getParent()).hasGoodNumberOfActivity()) {
                mErrors.add("too many endpoints");
                err = false;
            }
        }

        Vertex[] allSiblings = getParent().getChildrenGraphModel().getVertices();
        String thisName = (String) getBuiltInProperty(NAME);

        if (thisName == null || thisName.length() == 0) {
            mErrors.add("Slot name is empty");
        }
        else {
            for (Vertex v : allSiblings) {
                if (v instanceof ActivitySlotDef && v.getID() != getID()) {
                    ActivitySlotDef otherSlot = (ActivitySlotDef) v;
                    String otherName = (String) otherSlot.getBuiltInProperty(NAME);
                    if (otherName != null && otherName.equals(thisName)) {
                        mErrors.add("Duplicate slot name");
                        err = false;
                    }
                }
            }
        }

        KeyValuePair[] props;
        try {
            props = getTheActivityDef(null).getProperties().getKeyValuePairs();
            for (KeyValuePair prop : props) {
                if (prop.isAbstract() && !getProperties().containsKey(prop.getKey())) {
                    mErrors.add("Abstract property '" + prop.getKey() + "' not defined in slot");
                    err = false;
                }
            }
        }
        catch (Exception ex) {}

        // Loop check
        Vertex[] outV = getOutGraphables();
        Vertex[] anteVertices = GraphTraversal.getTraversal(getParent().getChildrenGraphModel(), this, GraphTraversal.kUp, false);
        boolean errInLoop = false;
        for (Vertex element : outV) {
            for (Vertex anteVertice : anteVertices) {
                if (!loop() && element.getID() == anteVertice.getID()) errInLoop = true;
            }
        }
        if (errInLoop) {
            mErrors.add("Problem in Loop");
            err = false;
        }
        return err;
    }

    /**
     * Method getNextWfVertices.
     *
     * @return WfVertexDef[]
     */
    public WfVertexDef[] getNextWfVertices() {
        return (WfVertexDef[]) getOutGraphables();
    }

    /**
     * @see org.cristalise.kernel.graph.model.GraphableVertex#isLayoutable()
     */
    /**
     * @see org.cristalise.kernel.graph.model.GraphableVertex#getIsLayoutable()
     */
    public boolean isLayoutable() {
        return true;
    }

    private void configureInstance(Activity act) {
        KeyValuePair[] k = getProperties().getKeyValuePairs();
        for (KeyValuePair element : k)
            act.getProperties().put(element.getKey(), element.getValue(), element.isAbstract());
        act.setCentrePoint(getCentrePoint());
        act.setOutlinePoints(getOutlinePoints());
        act.setInEdgeIds(getInEdgeIds());
        act.setOutEdgeIds(getOutEdgeIds());
        act.setName(getActName());
        act.setID(getID());
    }

    public String getActName() {
        return (String) getBuiltInProperty(NAME);
    }

    @Override
    public WfVertex instantiate(TransactionKey transactionKey) throws ObjectNotFoundException, InvalidDataException {
        Activity newActivity = (Activity) getTheActivityDef(transactionKey).instantiate(transactionKey);
        configureInstance(newActivity);
        if (newActivity.getProperties().getAbstract().size() > 0) {
            throw new InvalidDataException("Abstract properties not overridden: " + newActivity.getProperties().getAbstract().toString());
        }
        return newActivity;
    }
}
