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

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.ABORTABLE;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.REPEAT_WHEN;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.STATE_MACHINE_NAME;

import java.util.ArrayList;
import java.util.List;

import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.InvalidTransitionException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.Job;
import org.cristalise.kernel.graph.model.GraphModel;
import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.graph.model.GraphableVertex;
import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.graph.traversal.GraphTraversal;
import org.cristalise.kernel.lifecycle.LifecycleVertexOutlineCreator;
import org.cristalise.kernel.lifecycle.instance.stateMachine.State;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.lifecycle.instance.stateMachine.Transition;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.security.BuiltInAuthc;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CompositeActivity extends Activity {

    public CompositeActivity() {
        super();
        setBuiltInProperty(ABORTABLE, false);
        setBuiltInProperty(REPEAT_WHEN, false);
        setBuiltInProperty(STATE_MACHINE_NAME, StateMachine.getDefaultStateMachine("Composite"));

        try {
            setChildrenGraphModel(new GraphModel(new LifecycleVertexOutlineCreator()));
        } catch (InvalidDataException e) { } // shouldn't happen with an empty one
        setIsComposite(true);
    }

    @Override
    public void setChildrenGraphModel(GraphModel childrenGraph) throws InvalidDataException {
        super.setChildrenGraphModel(childrenGraph);
        childrenGraph.setVertexOutlineCreator(new LifecycleVertexOutlineCreator());
    }

    /**
     * launch the verification of the subprocess()
     */
    @Override
    public boolean verify() {
        boolean err = super.verify();
        GraphableVertex[] vChildren = getChildren();

        for (int i = 0; i < vChildren.length; i++) {
            if (!((WfVertex) vChildren[i]).verify()) {
                mErrors.add("error in children");
                return false;
            }
        }
        return err;
    }

    /**
     * Initialise Vertex and attach to the current activity
     *
     * @param vertex the vertex to be initialised
     * @param first if true, the Waiting state will be one of the first launched by the parent activity
     * @param point the location of the vertex in the graph
     */
    public void initChild(WfVertex vertex, boolean first, GraphPoint point) {
        safeAddChild(vertex, point);
        if (first) setFirstVertex(vertex.getID());
    }

    public void setFirstVertex(int vertexID) {
        log.debug("setFirstVertex() vertexID:"+vertexID);

        getChildrenGraphModel().setStartVertexId(vertexID);
    }


    /**
     * Adds vertex to graph cloning GraphPoint first (NPE safe)
     *
     * @param v
     * @param g
     */
    private void safeAddChild(GraphableVertex v, GraphPoint g) {
        GraphPoint p = null;
        if(g != null) p = new GraphPoint(g.x, g.y);
        addChild(v, p);
    }

    public WfVertex newExistingChild(Activity child, String Name, GraphPoint point) {
        child.setName(Name);
        safeAddChild(child, point);
        return child;
    }

    public WfVertex newChild(String Name, String Type, GraphPoint point) {
        WfVertex v = newChild(Type, point);
        v.setName(Name);
        return v;
    }

    public WfVertex newChild(String vertexTypeId, GraphPoint point) {
        return newChild(Types.valueOf(vertexTypeId), "False id", false, point);
    }

    public WfVertex newChild(Types type, String name, boolean first, GraphPoint point) {
        switch (type) {
            case Atomic:    return newAtomChild(name, first, point);
            case Composite: return newCompChild(name, first, point);
            case OrSplit:   return newSplitChild(name, "Or",   first, point);
            case XOrSplit:  return newSplitChild(name, "XOr",  first, point);
            case AndSplit:  return newSplitChild(name, "And",  first, point);
            case LoopSplit: return newSplitChild(name, "Loop", first, point);
            case Join:      return newJoinChild(name, "Join",  first, point);
            case Route:     return newJoinChild(name, "Route", first, point);

            default:
                throw new IllegalArgumentException("Unhandled enum value of WfVertex.Type:" + type.name());
        }
    }

    public CompositeActivity newCompChild(String id, boolean first, GraphPoint point) {
        CompositeActivity act = new CompositeActivity();
        initChild(act, first, point);
        act.setName(id);
        return act;
    }

    public Activity newAtomChild(String id, boolean first, GraphPoint point) {
        Activity act = new Activity();
        initChild(act, first, point);
        act.setName(id);
        return act;
    }

    public Split newSplitChild(String name, String Type, boolean first, GraphPoint point) {
        Split split = null;

        if      (Type.equals("Or"))   { split = new OrSplit(); }
        else if (Type.equals("XOr"))  { split = new XOrSplit(); }
        else if (Type.equals("Loop")) { split = new Loop(); }
        else                          { split = new AndSplit(); }

        initChild(split, first, point);
        split.setName(name);

        return split;
    }

    public Join newJoinChild(String name, String type, boolean first, GraphPoint point) {
        Join join = new Join();
        join.getProperties().put("Type", type);
        initChild(join, first, point);
        join.setName(name);
        return join;
    }

    /*
    public Join newRouteChild(GraphPoint point)
    {
        Join join = new Join();
        join.getProperties().put("Type", "Route");
        safeAddChild(join, point);
        return join;
    }
     */

    /**
     * None recursive search by id
     *
     * @param id
     * @return WfVertex
     */
    WfVertex search(int id) {
        return (WfVertex)getChildrenGraphModel().resolveVertex(id);
    }

    /**
     * Returns the Transition that can start the current CompAct automatically.
     *
     * @param agent performing Agent
     * @param currentState he actual State of the activity
     * @return the Transition that can be started automatically
     * @throws InvalidDataException
     */
    private Transition getStartTransition(AgentPath agent) throws InvalidDataException {
        Transition startTrans = null;
        //see if there's only one that isn't terminating
        try {
            for (Transition possTrans : getStateMachine().getPossibleTransitions(this, agent)) {
                if (!possTrans.isFinishing()) {
                    if (startTrans == null) {
                        startTrans = possTrans;
                    }
                    else {
                        startTrans = null;
                        break;
                    }
                }
            }
        }
        catch (ObjectNotFoundException e) {
            throw new InvalidDataException("Problem calculating possible transitions for agent "+agent.getAgentName(), e);
        }

        log.trace("getStartTransition({}) - state:{} trans:{}", getPath(), getStateName(), startTrans);
        return startTrans;
    }

    /**
     * Find the next transition that could finish the CompositeActivity. A non-finishing and non-blocking transition 
     * will override a finishing one, but otherwise having more than one possible means we cannot proceed.
     * Transition enablement should filter before this point.
     * 
     * @param agent
     * @return
     * @throws InvalidDataException
     */
    private Transition getFinishTransition(AgentPath agent) throws InvalidDataException {
        Transition finishTrans = null;
        try {
            for (Transition possTran : getStateMachine().getPossibleTransitions(this, agent)) {
                if (finishTrans == null || (finishTrans.isFinishing() && !possTran.isFinishing() && !possTran.isBlocking())) {
                    finishTrans = possTran;
                }
                else if (finishTrans.isFinishing() == possTran.isFinishing()) {
                    log.warn("getFinishTransition() - Unclear choice of transition possible CompositeActivity:{} state:{}", getPath(), getStateName());
                    return null;
                }
            }
        }
        catch (ObjectNotFoundException e) {
            throw new InvalidDataException("Problem calculating possible transitions for agent "+agent.getAgentName(), e);
        }

        log.trace("getFinishTransition({}) - state:{} trans:{}", getPath(), getStateName(), finishTrans);
        return finishTrans;
    }

    private AgentPath getSystemAgent(TransactionKey transactionKey) throws InvalidDataException {
        try {
            return (AgentPath) BuiltInAuthc.SYSTEM_AGENT.getPath(transactionKey);
        }
        catch (ObjectNotFoundException e) {
            throw new InvalidDataException("CompAct:"+getName(), e);
        }
    }

    private void autoStart(TransactionKey transactionKey) throws InvalidDataException {
        ItemPath itemPath = getWf().getItemPath();
        AgentPath agent = getSystemAgent(transactionKey);

        Transition autoStart = getStartTransition(agent);

        if (autoStart != null) {
            try {
                request(agent, itemPath, autoStart.getId(), null, "", null, transactionKey);
            }
            catch (AccessRightsException e) {
                log.warn("autoStart() - Agent:{} didn't have permission to start composite activity:{}", agent, getPath());
                return;
            }
            catch (InvalidDataException e) {
                throw e;
            }
            catch (Exception e) {
                //log.error("autoStart()", e);
                throw new InvalidDataException("Problem auto starting composite activity:" + getPath(), e);
            }
        }
    }

    private void autoFinish(TransactionKey transactionKey) throws InvalidDataException {
        ItemPath itemPath = getWf().getItemPath();
        AgentPath agent = getSystemAgent(transactionKey);

        Transition trans = getFinishTransition(agent);

        if (trans != null) {
            // automatically execute the next transition if it doesn't require an script or outcome
            if (trans.hasOutcome(getProperties()) || trans.hasScript(getProperties())) {
                log.warn("autoFinish({}) - Cannot finish automatically, it has script or schema defined. ", getName());
                setActive(true);
                return;
            }

            try {
                request(agent, itemPath, trans.getId(), /*requestData*/null, "", /*attachment*/null, transactionKey);
                // don't run next if we didn't finish
/*????*/        if (!trans.isFinishing()) return;
            }
            catch (InvalidDataException e) {
                throw e;
            }
            catch (Exception e) {
                log.error("autoFinish() - Problem completing CompAct:{}", getName(), e);
                setActive(true);
                throw new InvalidDataException("Problem completing CompAct:"+getName(), e);
            }
        }
        else {
            log.trace("Not possible for the current agent to proceed with the Composite Activity '"+getName()+"'.");
            setActive(true);
        }
    }

    @Override
    public void run(TransactionKey transactionKey) throws InvalidDataException {
        log.trace("run() - {}", this);

        super.run(transactionKey);

        if (isStartable()) autoStart(transactionKey);
        if (isFinishable()) autoFinish(transactionKey);
    }

    /**
     * 
     * @return
     * @throws InvalidDataException
     */
    protected boolean isStartable() throws InvalidDataException {
        int  initialState = getStateMachine().getInitialStateCode();
        State currentState = getStateMachine().getState(state);
        return getChildrenGraphModel().getStartVertex() != null && !currentState.isFinished() && initialState == state;
    }
    

    /**
     * Checks if the CompositeActivity can be finished by checking if there is no
     * further Activities to be executed at its current state. 
     * 
     * @return whether the CompositeActivity can be finished or not
     * @throws InvalidDataException current data of CompositeActivity was inconsistent
     */
    protected boolean isFinishable() throws InvalidDataException {
        // do not finish 'workflow' and 'domain' CompActs
        if (getName().equals("workflow") || getName().equals("domain")) return false;

        WfVertex startVertex = (WfVertex) getChildrenGraphModel().getStartVertex();

        if (startVertex instanceof Activity) {
            Activity act = (Activity)startVertex;
            if (!act.isFinished() && act.active) return false;
        }

        return isFinishable(startVertex == null ? null : startVertex.findLastVertex());
    }

    /**
     * Checks if the CompositeActivity can be finished by checking if there is no
     * further Activities to be executed at its current state.
     * 
     * @param lastVertex of the current CompositeActivity is used as a starting point for the checking
     * @return whether the CompositeActivity can be finished or not
     * @throws InvalidDataException current data of CompositeActivity was inconsistent
     */
    protected boolean isFinishable(WfVertex lastVertex) throws InvalidDataException {
        if (getName().equals("workflow") || getName().equals("domain")) return false;

        //Calculate if there is still an Activity to be executed
        if (lastVertex == null) {
            return true;
        }
        else if (lastVertex instanceof Join) {
            return getPossibleActs(lastVertex, GraphTraversal.kUp).size() == 0;
        }
        else if (lastVertex instanceof Loop) {
            return getPossibleActs(lastVertex, GraphTraversal.kDown).size() == 0;
        }
        else if (lastVertex instanceof Activity) {
            Activity act = (Activity)lastVertex;
            return act.isFinished() && !act.active;
        }

        return false;
    }

    /**
     * 
     */
    @Override
    public void runNext(TransactionKey transactionKey) throws InvalidDataException  {
        log.trace("runNext() - {}", this);

        if (!isFinished()) autoFinish(transactionKey);
        super.runNext(transactionKey);
    }

    /**
     *
     */
    @Override
    public ArrayList<Job> calculateJobs(AgentPath agent, ItemPath itemPath, boolean recurse)
            throws ObjectNotFoundException, InvalidDataException
    {
        ArrayList<Job> jobs = new ArrayList<Job>();
        boolean childActive = false;
        if (recurse) {
            for (int i = 0; i < getChildren().length; i++) {
                if (getChildren()[i] instanceof Activity) {
                    Activity child = (Activity) getChildren()[i];
                    jobs.addAll(child.calculateJobs(agent, itemPath, recurse));
                    childActive |= child.active;
                }
            }
        }

        if (!childActive) jobs.addAll(super.calculateJobs(agent, itemPath, recurse));

        return jobs;
    }

    @Override
    public ArrayList<Job> calculateAllJobs(AgentPath agent, ItemPath itemPath, boolean recurse)
            throws ObjectNotFoundException, InvalidDataException
    {
        ArrayList<Job> jobs = new ArrayList<Job>();

        if (recurse) {
            for (int i = 0; i < getChildren().length; i++) {
                if (getChildren()[i] instanceof Activity) {
                    Activity child = (Activity) getChildren()[i];
                    jobs.addAll(child.calculateAllJobs(agent, itemPath, recurse));
                }
            }
        }

        jobs.addAll(super.calculateAllJobs(agent, itemPath, recurse));

        return jobs;
    }

    public Next addNext(WfVertex origin, WfVertex terminus) {
        return new Next(origin, terminus);
    }

    public Next addNext(int originID, int terminusID) {
        return addNext(search(originID), search(terminusID));
    }

    public boolean hasGoodNumberOfActivity() {
        int endingAct = 0;
        for (int i = 0; i < getChildren().length; i++) {
            WfVertex vertex = (WfVertex) getChildren()[i];

            if (getChildrenGraphModel().getOutEdges(vertex).length == 0) endingAct++;
        }

        if (endingAct > 1) return false;

        return true;
    }

    @Override
    public String getType() {
        return super.getType();
    }

    @Override
    public void reinit(int idLoop) throws InvalidDataException {
        super.reinit(idLoop);
        if (getChildrenGraphModel().getStartVertex() != null && !getStateMachine().getState(state).isFinished()) {
            ((WfVertex) getChildrenGraphModel().getStartVertex()).reinit(idLoop);
        }
    }

    @Override
    public void abort(AgentPath agentPath, ItemPath itemPath, TransactionKey transactionKey)
            throws AccessRightsException, InvalidTransitionException, InvalidDataException, ObjectNotFoundException, PersistencyException,
            ObjectAlreadyExistsException, ObjectCannotBeUpdated, CannotManageException, InvalidCollectionModification
    {
        StateMachine sm = getStateMachine();
        int abortId = sm.getTransition("Abort").getId();

        for (GraphableVertex childV : getChildren()) {
            if (childV instanceof CompositeActivity) {
                CompositeActivity ca = (CompositeActivity) childV;
                if (ca.active) {
                    ca.request(agentPath, itemPath, abortId, "", "", new byte[0], transactionKey);
                    ca.setActive(false);
                }
            }
            else {
                ((WfVertex) childV).abort(agentPath, itemPath, transactionKey);
            }
        }
    }

    public boolean hasActive() {
        GraphableVertex[] vChildren = getChildren();

        for (int i = 0; i < vChildren.length; i++) {
            if (!(vChildren[i] instanceof Activity)) continue;

            Activity childAct = (Activity)vChildren[i];

            if (childAct.getActive())
                return true; // if a child activity is active

            if (childAct instanceof CompositeActivity &&  ((CompositeActivity)vChildren[i]).hasActive())
                return true; // if a child composite has active children
        }
        return false; // don't include own active status
    }

    @Override
    public String request(AgentPath agent, ItemPath itemPath, int transitionID, String requestData, String attachmentType, byte[] attachment, TransactionKey transactionKey)
            throws AccessRightsException, InvalidTransitionException, InvalidDataException, ObjectNotFoundException, PersistencyException,
            ObjectAlreadyExistsException, ObjectCannotBeUpdated, CannotManageException, InvalidCollectionModification
    {
        StateMachine sm = getStateMachine();
        Transition trans = sm.getTransition(transitionID);

        log.trace("request() - {}/{} trans:{}", itemPath.getItemName(), this, trans);

        if ((trans.isFinishing() || trans.isBlocking()) && hasActive()) {
            if ((Boolean)getBuiltInProperty(ABORTABLE)) {
                abort(agent, itemPath, transactionKey);
            }
            else {
                throw new InvalidTransitionException("Attempted to finish '"+getPath()+"' it had active children but was not Abortable");
            }
        }

        if (trans.reinitializes()) {
            int preserveState = state;
            reinit(getID());
            setState(preserveState);
        }

        // request() changes the state of CA, so check now if the children has to be initialized
        boolean initChldren = sm.getState(state).equals(sm.getInitialState()) || trans.reinitializes();

        // execute request() first to create the correct order of events
        String  result = super.request(agent, itemPath, transitionID, requestData, attachmentType, attachment, transactionKey);

        // init children if needed. 
        if (initChldren) {
            Vertex startVertex = getChildrenGraphModel().getStartVertex();
            if ( startVertex != null) {
                ((WfVertex)startVertex).runFirst(transactionKey);
            }
            else {
                log.debug("request({}) has NO start vertex", getPath());
            }
        }

        return result;
    }

    public List<Activity> getPossibleActs(WfVertex fromVertex, int direction) throws InvalidDataException {
        List<Activity> nextActs = new ArrayList<>();

        for (Vertex v : GraphTraversal.getTraversal(getChildrenGraphModel(), fromVertex, direction, false)) {
            if (v instanceof Activity) {
                Activity act = (Activity) v;
                if (!act.isFinished() && act.active) nextActs.add(act);
            }
        }

        return nextActs;
    }
}
