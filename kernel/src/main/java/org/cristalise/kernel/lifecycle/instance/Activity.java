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
package org.cristalise.kernel.lifecycle.instance;

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.AGENT_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.AGENT_ROLE;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.BREAKPOINT;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.DESCRIPTION;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.PAIRING_ID;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.PREDEFINED_STEP;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.VALIDATE_OUTCOME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.VIEW_POINT;
import static org.cristalise.kernel.property.BuiltInItemProperties.NAME;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Vector;
import javax.xml.xpath.XPathExpressionException;
import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.GTimeStamp;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.InvalidTransitionException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.Job;
import org.cristalise.kernel.events.History;
import org.cristalise.kernel.graph.model.GraphableVertex;
import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.graph.traversal.GraphTraversal;
import org.cristalise.kernel.lifecycle.WfCastorHashMap;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep;
import org.cristalise.kernel.lifecycle.instance.stateMachine.State;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.lifecycle.instance.stateMachine.Transition;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.OutcomeAttachment;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyUtility;
import org.cristalise.kernel.utils.DateUtility;
import org.cristalise.kernel.utils.LocalObjectLoader;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Activity extends WfVertex {
    protected static final String XPATH_TOKEN = "xpath:";

    /**
     * vector of errors (Strings) that is constructed each time verify() was launched
     */
    protected Vector<String> mErrors;
    /**
     * @associates a StateMachine engine
     */
    private StateMachine     machine;
    protected int            state  = -1;
    /**
     * true is available to be executed
     */
    public boolean           active = false;
    /**
     * used in verify()
     */
    private boolean          loopTested;
    private GTimeStamp       mStateDate;
    private String           mType;
    private String           mTypeName;

    public Activity() {
        super();
        setProperties(new WfCastorHashMap());

        mErrors = new Vector<String>(0, 1);
        mStateDate = new GTimeStamp();
        DateUtility.setToNow(mStateDate);
    }

    /**
     * add the activity which id is idNext as next of the current one
     */
    Next addNext(String idNext) {
        return addNext((WfVertex) getParent().search(idNext));
    }

    /**
     * adds a New link between the current Activity and the WfVertex passed in
     * param
     */
    @Override
    public Next addNext(WfVertex vertex) {
        return new Next(this, vertex);
    }

    public StateMachine getStateMachine() throws InvalidDataException {
        return getStateMachine(null);
    }

    public StateMachine getStateMachine(TransactionKey transKey) throws InvalidDataException {
        if (machine == null) {
            try {
                machine = LocalObjectLoader.getStateMachine(getProperties(), transKey);
            }
            catch (ObjectNotFoundException e) {
                throw new InvalidDataException(e.getMessage());
            }
        }
        return machine;
    }

    /**
     * @return The current State of the StateMachine (Used in Serialisation)
     * @throws InvalidDataException data was inconsistent
     */
    public int getState() throws InvalidDataException {
        if (state == -1) state = getStateMachine().getInitialStateCode();
        return state;
    }

    /**
     * Returns the id of the error Transition associated with the current state
     * @return the id of the error Transition associated with the current state or -1 of there is no error Transition
     */
    public int getErrorTransitionId() {
        try {
            return getStateMachine().getErrorTransitionIdForState(getState());
        }
        catch (InvalidDataException e) {
            return -1;
        }
    }

    public String getStateName() throws InvalidDataException {
        return getStateMachine().getState(getState()).getName();
    }

    /** Sets a new State */
    public void setState(int state) {
        this.state = state;
    }

    public boolean isFinished() throws InvalidDataException {
        return getStateMachine().getState(getState()).isFinished();
    }

    public String request(AgentPath agent,
                          ItemPath itemPath,
                          int transitionID,
                          String requestData,
                          String attachmentType,
                          byte[] attachment,
                          TransactionKey transactionKey
                          )
            throws AccessRightsException,
                   InvalidTransitionException,
                   InvalidDataException,
                   ObjectNotFoundException,
                   PersistencyException,
                   ObjectAlreadyExistsException,
                   ObjectCannotBeUpdated,
                   CannotManageException,
                   InvalidCollectionModification
    {
        boolean validateOutcomeDefault = Gateway.getProperties().getBoolean("Activity.validateOutcome", false);
        boolean validateOutcome = (boolean) getBuiltInProperty(VALIDATE_OUTCOME, validateOutcomeDefault);

        return request(agent, itemPath, transitionID, requestData, attachmentType, attachment, validateOutcome, transactionKey);
    }

    public String request(AgentPath agent,
                          ItemPath itemPath,
                          int transitionID,
                          String requestData,
                          String attachmentType,
                          byte[] attachment,
                          boolean validateOutcome,
                          TransactionKey transactionKey
                          )
            throws AccessRightsException,
                   InvalidTransitionException,
                   InvalidDataException,
                   ObjectNotFoundException,
                   PersistencyException,
                   ObjectAlreadyExistsException,
                   ObjectCannotBeUpdated,
                   CannotManageException,
                   InvalidCollectionModification
    {
        if (log.isTraceEnabled()) {
            StateMachine sm = getStateMachine();
            log.trace("request() - item:{} path:{} state:{} transition:{}", itemPath.getItemName(), getPath(), sm.getState(getState()), sm.getTransition(transitionID));
        }

        // Find requested transition
        Transition transition = getStateMachine().getTransition(transitionID);

        // Check if the transition is possible
        transition.checkPerformingRole(this, agent);

        // Verify outcome
        boolean storeOutcome = false;
        if (transition.hasOutcome(getProperties())) {
            if (StringUtils.isNotBlank(requestData)) {
                storeOutcome = true;
            }
            else if (transition.getOutcome().isRequired()) {
                throw new InvalidDataException("Transition requires outcome data, but none was given");
            }
        }

        // Get new state
        State oldState = getStateMachine().getState(this.state);
        State newState = getStateMachine().traverse(this, transition, agent);

        // Run extra logic in predefined steps here
        String outcome = runActivityLogic(agent, itemPath, transitionID, requestData, transactionKey);

        // set new state and reservation
        setState(newState.getId());
        setBuiltInProperty(AGENT_NAME, transition.getReservation(this, agent));

        History hist = new History(itemPath, transactionKey);

        if (storeOutcome) {
            Schema schema = transition.getSchema(getProperties());
            Outcome newOutcome = new Outcome(-1, outcome, schema);

            // This is used by PredefinedStep executed during bootstrap
            if (validateOutcome) newOutcome.validateAndCheck();

            executePredefinedSteps(agent, itemPath, newOutcome, transactionKey);

            String viewpoint = resolveViewpointName(newOutcome);
            boolean hasAttachment = attachment.length > 0;

            int eventID = hist.addEvent(agent, null/*usedRole*/, getName(), getPath(), getType(),
                    schema, getStateMachine(), transitionID, viewpoint, hasAttachment).getID();
            newOutcome.setID(eventID);

            Gateway.getStorage().put(itemPath, newOutcome, transactionKey);
            if (hasAttachment) {
                Gateway.getStorage().put(itemPath, new OutcomeAttachment(itemPath, newOutcome, attachmentType, attachment), transactionKey);
            }

            // update specific view if defined
            if (!viewpoint.equals("last")) {
                Gateway.getStorage().put(itemPath, new Viewpoint(itemPath, schema, viewpoint, eventID), transactionKey);
            }

            // update the default "last" view
            Gateway.getStorage().put(itemPath, new Viewpoint(itemPath, schema, "last", eventID), transactionKey);

            updateItemProperties(itemPath, newOutcome, transactionKey);
        }
        else {
            updateItemProperties(itemPath, null, transactionKey);
            hist.addEvent(agent, null/*usedRole*/, getName(), getPath(), getType(), getStateMachine(), transitionID);
        }

        boolean breakPoint = (Boolean) getBuiltInProperty(BREAKPOINT, Boolean.FALSE);

        if (newState.isFinished() && !oldState.isFinished() && !breakPoint) {
            runNext(agent, itemPath, transactionKey);
        }
        else {
            DateUtility.setToNow(mStateDate);
        }

        return outcome;
    }

    /**
     * Execute PredefiendSteps associated with the Activity using its properties and the Outcome
     * 
     * @param agent current Agent requesting the Activity
     * @param itemPath the current Item
     * @param newOutcome the Outcome submitted to the Activity
     * @param transactionKey the key of the current transaction 
     */
    private void executePredefinedSteps(AgentPath agent, ItemPath itemPath, Outcome newOutcome, TransactionKey transactionKey) 
            throws AccessRightsException, InvalidTransitionException, InvalidDataException, ObjectNotFoundException, 
            PersistencyException, ObjectAlreadyExistsException, ObjectCannotBeUpdated, CannotManageException, InvalidCollectionModification
    {
        String predefStepProperty = getBuiltInProperty(PREDEFINED_STEP, "").toString();

        if (StringUtils.isNotBlank(predefStepProperty)) {
            log.debug("executePredefinedStep() - predefStepProperty:{}", predefStepProperty);

            for (String predefStepName : StringUtils.split(predefStepProperty, ',')) {
                PredefinedStep predefStep = PredefinedStep.getStepInstance(predefStepName.trim());

                if (predefStep == null) {
                    log.warn("executePredefinedStep() - SKIPPING Invalid PredefinedStep name:'{}'", predefStepName);
                }
                else if (predefStep.outcomeHasValidData(newOutcome)) {
                    predefStep.mergeProperties(getProperties());
                    predefStep.computeUpdates(itemPath, this, newOutcome, transactionKey);

                    for (Entry<ItemPath, String> entry : predefStep.getAutoUpdates().entrySet()) {
                        predefStep.request(agent, entry.getKey(), entry.getValue(), transactionKey);
                    }
                }
                else {
                    log.debug("executePredefinedStep() - SKIPPING optional PredefinedStep:'{}'", predefStepName);
                }
            }
        }
    }

    private String resolveViewpointName(Outcome outcome) throws InvalidDataException {
        String viewpointString = (String)getBuiltInProperty(VIEW_POINT);

        log.debug("resolveViewpointName() - act:{} viewpointString:{}", getName(), viewpointString);

        if (StringUtils.isBlank(viewpointString)) {
            viewpointString = "last";
        }
        //FIXME: use DataHelper if possible, because it will make code more general
        else if(viewpointString.startsWith(XPATH_TOKEN)) {
            try {
                viewpointString = outcome.getFieldByXPath(viewpointString.substring(XPATH_TOKEN.length()));
            }
            catch (XPathExpressionException e) {
                throw new InvalidDataException(e.getMessage());
            }
        }

        if (StringUtils.isBlank(viewpointString))
            throw new InvalidDataException("Resolved viewpoint name cannot be blank for activity:" + getName());

        return viewpointString;
    }

    private void updateItemProperties(ItemPath itemPath, Outcome outcome, TransactionKey transactionKey)
            throws InvalidDataException, PersistencyException, ObjectCannotBeUpdated, ObjectNotFoundException
    {
        for(java.util.Map.Entry<String, Object> entry: getProperties().entrySet()) {
            if(entry.getKey().startsWith("ItemProperty.")) {
                String propName = entry.getKey().substring(13);

                if(StringUtils.isNotBlank(propName)) {
                    String propValue = entry.getValue().toString();

                    //FIXME: use DataHelper if possible, because it will make code more general
                    if (outcome != null && StringUtils.isNotBlank(propValue) && propValue.startsWith(XPATH_TOKEN)) {
                        try {
                            propValue = outcome.getFieldByXPath(propValue.substring(XPATH_TOKEN.length()));
                        }
                        catch (XPathExpressionException e) {
                            throw new InvalidDataException(e.getMessage());
                        }
                    }

                    if(StringUtils.isNotBlank(propValue)) {
                        PropertyUtility.writeProperty(itemPath, propName, propValue, transactionKey);
                    }
                }
                else {
                    throw new InvalidDataException("Incomplete vertex property name:" + entry.getKey());
                }
            }
        }
    }

    /**
     * Overridden in predefined steps
     *
     * @param agent
     * @param itemPath
     * @param transitionID
     * @param requestData
     * @param transactionKey
     * @return
     * @throws InvalidDataException
     * @throws InvalidCollectionModification
     * @throws ObjectAlreadyExistsException
     * @throws ObjectCannotBeUpdated
     * @throws ObjectNotFoundException
     * @throws PersistencyException
     * @throws CannotManageException
     * @throws AccessRightsException
     */
    protected String runActivityLogic(AgentPath agent, ItemPath itemPath, int transitionID, String requestData, TransactionKey transactionKey)
            throws InvalidDataException, InvalidCollectionModification, ObjectAlreadyExistsException, ObjectCannotBeUpdated,
            ObjectNotFoundException, PersistencyException, CannotManageException, AccessRightsException
    {
        return requestData;
    }

    @Override
    public boolean verify() {
        mErrors.removeAllElements();
        int nbInEdgres = getInEdges().length;
        int nbOutEdges = getOutEdges().length;
        if (nbInEdgres == 0 && this.getID() != getParent().getChildrenGraphModel().getStartVertexId()) {
            mErrors.add("Unreachable");
            return false;
        }
        else if (nbInEdgres > 1) {
            mErrors.add("Bad nb of previous");
            return false;
        }
        else if (nbOutEdges > 1) {
            mErrors.add("too many next");
            return false;
        }
        else if (nbOutEdges == 0) {
            if (!getParentCA().hasGoodNumberOfActivity()) {
                mErrors.add("too many endpoints");
                return false;
            }
        }
        return true;
    }

    /**
     * Used in verify()
     */
    @Override
    public boolean loop() {
        boolean loop2 = false;
        if (!loopTested) {
            loopTested = true;
            if (getOutGraphables().length != 0) loop2 = ((WfVertex) getOutGraphables()[0]).loop();
        }
        loopTested = false;
        return loop2;
    }

    private CompositeActivity getParentCA() {
        GraphableVertex theParent = getParent();

        if (theParent != null) return (CompositeActivity) theParent;
        else                   return null;
    }

    /**
     * sets the next activity available if possible
     */
    @Override
    public void runNext(AgentPath agent, ItemPath itemPath, TransactionKey transactionKey) throws InvalidDataException {
        setActive(false);

        Vertex[] outVertices = getOutGraphables();

        //run next vertex if any, so state/status of activities is updated
        if (outVertices.length > 0) ((WfVertex)outVertices[0]).run(agent, itemPath, transactionKey);

        //parent is never null, because we do not call runNext() for the top level workflow (see bellow)
        CompositeActivity parent = getParentCA();

        //check if the CA can be finished or not
        if (checkParentFinishable(parent)) {
            // do not call runNext() for the top level compAct (i.e. workflow is never finished)
            if (! parent.getName().equals("domain")) {
                parent.runNext(agent, itemPath, transactionKey);
            }
        }
    }

    /**
     * Calculate if the CompositeActivity (parent) can be finished automatically or not
     */
    private boolean checkParentFinishable(CompositeActivity parent) throws InvalidDataException {
        Vertex[] outVertices = getOutGraphables();
        boolean cont = outVertices.length > 0;
        WfVertex lastVertex = null;

        //Find the 'last' Vertex of the output of the Activity
        while (cont) {
            lastVertex = (WfVertex)outVertices[0];

            if (lastVertex instanceof Join) {
                outVertices = lastVertex.getOutGraphables();
                cont = outVertices.length > 0;
            }
            else if (lastVertex instanceof Loop) {
                String pairingId = (String) lastVertex.getBuiltInProperty(PAIRING_ID);
                if (StringUtils.isNotBlank(pairingId)) {
                    //Find the out Join which has not the same pairing id as the Loop
                    Join outJoin = (Join) Arrays.stream(lastVertex.getOutGraphables())
                            .filter(v -> !pairingId.equals(((WfVertex) v).getBuiltInProperty(PAIRING_ID)))
                            .findFirst().get();
                    outVertices = outJoin.getOutGraphables();
                    cont = outVertices.length > 0;
                    lastVertex = outJoin;
                }
                else {
                    cont = false;
                }
            }
            else if (lastVertex instanceof Split) {
                String pairingID = (String) lastVertex.getBuiltInProperty(PAIRING_ID);
                if (StringUtils.isNotBlank(pairingID)) {
                    // the pair of a Split (not Loop) is a Join
                    Join splitJoin = (Join)findPair(pairingID);
                    outVertices = splitJoin.getOutGraphables();
                    cont = outVertices.length > 0;
                    lastVertex = splitJoin;
                }
                else {
                    cont = false;
                }
            }
            else {
                cont = false;
            }
        }

        boolean finishable = false;

        //Calculate if there is still an Activity to be executed in the Parent
        if (lastVertex == null) {
            finishable = true;
        }
        else if (lastVertex instanceof Join) {
            finishable = parent.getPossibleActs(lastVertex, GraphTraversal.kUp).size() == 0;
        }
        else if (lastVertex instanceof Loop) {
            finishable = parent.getPossibleActs(lastVertex, GraphTraversal.kDown).size() == 0;
        }

        return finishable;
    }

    /**
     *
     * @return the only Next of the Activity
     */
    public Next getNext() {
        if (getOutEdges().length > 0) return (Next) getOutEdges()[0];
        else return null;
    }

    /**
     * reinitialises the Activity and propagate (for Loop)
     */
    @Override
    public void reinit(int idLoop) throws InvalidDataException {
        log.trace("reinit(id:{}, idLoop:{}) - parent:{} act:{}", getID(), idLoop, getParent().getName(), getPath());

        setState(getStateMachine().getInitialState().getId());

        Vertex[] outVertices = getOutGraphables();

        //NOTE: strange condition, activity can have zero or one outVertex and its id cannot be the loopId
        if (outVertices.length > 0 && idLoop != getID()) {
            ((WfVertex) outVertices[0]).reinit(idLoop);
        }
    }

    /**
     * return the String that identifies the errors found in th activity
     */
    @Override
    public String getErrors() {
        if (mErrors.size() == 0) return "No error";
        return mErrors.elementAt(0);
    }

    /**
     * called by precedent Activity runNext() for setting the activity able to be executed
     */
    @Override
    public void run(AgentPath agent, ItemPath itemPath, TransactionKey transactionKey) throws InvalidDataException {
        log.trace("run() path:" + getPath() + " state:" + getStateName());

        if (isFinished()) {
            runNext(agent, itemPath, transactionKey);
        }
        else {
            if (!getActive()) setActive(true);

            DateUtility.setToNow(mStateDate);
        }
    }

    /**
     * sets the activity available to be executed on start of Workflow or
     * composite activity (when it is the first one of the (sub)process
     */
    @Override
    public void runFirst(AgentPath agent, ItemPath itemPath, TransactionKey transactionKey) throws InvalidDataException {
        log.trace("runFirst() - path:" + getPath());
        run(agent, itemPath, transactionKey);
    }

    /**
     * @return the current ability to be executed
     */
    public boolean getActive() {
        return active;
    }

    /**
     * sets the ability to be executed
     */
    public void setActive(boolean acti) {
        active = acti;
    }

    /**
     * @return the Description field of properties
     */
    public String getDescription() {
        if (getProperties().containsKey("Description")) return (String) (getBuiltInProperty(DESCRIPTION));
        return "No description";
    }

    public String getCurrentAgentName() {
        return (String) getBuiltInProperty(AGENT_NAME);
    }

    public String getCurrentAgentRole() {
        return (String) getBuiltInProperty(AGENT_ROLE);
    }

    /**
     * Calculates the lists of jobs for the activity and its children (cf org.cristalise.kernel.entity.Job)
     */
    public List<Job> calculateJobs(AgentPath agent, ItemPath itemPath, boolean recurse)
            throws ObjectNotFoundException, InvalidDataException
    {
        return calculateJobsBase(agent, itemPath, false);
    }

    public List<Job> calculateAllJobs(AgentPath agent, ItemPath itemPath, boolean recurse)
            throws ObjectNotFoundException, InvalidDataException {
        return calculateJobsBase(agent, itemPath, true);
    }

    private List<Job> calculateJobsBase(AgentPath agent, ItemPath itemPath, boolean includeInactive)
            throws ObjectNotFoundException, InvalidDataException
    {
        log.trace("calculateJobsBase() - act:" + getPath());
        List<Job> jobs = new ArrayList<Job>();
        if ((includeInactive || getActive()) && !getName().equals("domain")) {
            List<Transition> transitions = getStateMachine().getPossibleTransitions(this, agent);
            log.trace("calculateJobsBase() - Got " + transitions.size() + " transitions.");
            for (Transition transition : transitions) {
                log.trace("calculateJobsBase() - Creating Job object for transition " + transition.getName());
                jobs.add(new Job(this, itemPath, transition.getName(), agent, transition.getRoleOverride()));
            }
        }
        return jobs;
    }

    /**
     * Returns the startDate.
     *
     * @return GTimeStamp startDate
     */
    public GTimeStamp getStateDate() {
        return mStateDate;
    }

    public void setStateDate(GTimeStamp startDate) {
        mStateDate = startDate;
    }

    @Deprecated
    public void setActiveDate(GTimeStamp date) {
    }

    @Deprecated
    public void setStartDate(GTimeStamp date) {
        setStateDate(date);
    }

    /**
     * Returns the type.
     *
     * @return String
     */
    public String getType() {
        return mType;
    }

    public String getTypeName() {
        if (mType == null) return null;
        if (mTypeName == null) {
            try {
                ItemPath actType = new ItemPath(mType);
                Property nameProp = (Property) Gateway.getStorage().get(actType, ClusterType.PROPERTY + "/" + NAME, null);
                mTypeName = nameProp.getValue();
            }
            catch (Exception e) {
                mTypeName = mType;
            }
        }
        return mTypeName;
    }

    /**
     * Sets the type.
     *
     * @param type
     *            The type to set
     */
    public void setType(String type) {
        mType = type;
        mTypeName = null;
    }

    @Override
    public void abort() {
        active = false;
    }
}
