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
package org.cristalise.kernel.events;


import static org.cristalise.kernel.persistency.ClusterType.HISTORY;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.RemoteMap;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.persistency.outcome.Schema;

import lombok.extern.slf4j.Slf4j;

/**
 * The History is an instance of {@link org.cristalise.kernel.persistency.RemoteMap} 
 * which provides a live view onto the Events of an Item.
 */
@Slf4j
public class History extends RemoteMap<Event> {

    private static final long serialVersionUID = 3273324106002587993L;

    public History(ItemPath itemPath, TransactionKey transactionKey) {
        super(itemPath, HISTORY.getName(), transactionKey);
    }

    public Event getEvent(int id) {
        return get(String.valueOf(id));
    }

    public Event get(Integer id) {
        return get(String.valueOf(id));
    }

    @Override
    public Event remove(Object key) {
        throw new UnsupportedOperationException();
    }

    private Event storeNewEvent(Event newEvent) {
        synchronized (this) {
            newEvent.setID( getLastId()+1);
            put(newEvent.getName(), newEvent);
            return newEvent;
        }
    }

    public Event addEvent(AgentPath agentPath, AgentPath delegatePath, String agentRole,
                          String stepName, String stepPath, String stepType, 
                          StateMachine stateMachine, int transitionId)
    {
        return storeNewEvent(
                new Event(mItemPath, agentPath, delegatePath, agentRole, stepName, stepPath, stepType, stateMachine, transitionId));
    }

    public Event addEvent(AgentPath agentPath, AgentPath delegatePath, String agentRole,
                          String stepName, String stepPath, String stepType, Schema schema, 
                          StateMachine stateMachine, int transitionId, String viewName)
    {
        Event newEvent = new Event(mItemPath,agentPath, delegatePath, agentRole, stepName, stepPath, stepType, stateMachine, transitionId);
        newEvent.addOutcomeDetails(schema, viewName);
        return storeNewEvent(newEvent);
    }

    public Event addEvent(AgentPath agentPath, AgentPath delegatePath, String agentRole,
                          String stepName, String stepPath, String stepType, Schema schema, 
                          StateMachine stateMachine, int transitionId, String viewName, boolean hasAttachment)
    {
        Event newEvent = new Event(mItemPath,agentPath, delegatePath, agentRole, stepName, stepPath, stepType, stateMachine, transitionId, hasAttachment);
        newEvent.addOutcomeDetails(schema, viewName);
        return storeNewEvent(newEvent);
    }

    public Event addEvent(AgentPath agentPath, AgentPath delegatePath,	String agentRole,
                          String stepName, String stepPath, String stepType,
                          StateMachine stateMachine, int transitionId, String timeString) 
           throws InvalidDataException
    {
        Event newEvent = new Event(mItemPath, agentPath, delegatePath, agentRole, stepName, stepPath, stepType, stateMachine, transitionId);
        newEvent.setTimeString(timeString);
        return storeNewEvent(newEvent);
    }

    public Event addEvent(AgentPath agentPath, AgentPath delegatePath, String agentRole,
                          String stepName, String stepPath, String stepType, Schema schema, 
                          StateMachine stateMachine, int transitionId, String viewName, String timeString) 
           throws InvalidDataException
    {
        Event newEvent = new Event(mItemPath, agentPath, delegatePath, agentRole, stepName, stepPath, stepType, stateMachine, transitionId);
        newEvent.addOutcomeDetails(schema, viewName);
        newEvent.setTimeString(timeString);
        return storeNewEvent(newEvent);
    }

    /**
     * Events are never deleted and they are numbered incrementally, which means id >= 0 and id <= lastId
     */
    public boolean containsKey(Integer id) {
        return containsKey((Object)id);
    }

    /**
     * Events are never deleted and they are numbered incrementally, which means key >= 0 and key <= lastId
     */
    @Override
    public synchronized boolean containsKey(Object key) {
        try {
            int id = key instanceof String ? Integer.valueOf((String)key) : (int)key;
            return id >= 0 && id <= getLastId();
        }
        catch (Exception e) {
            log.error("containsKey() - Could not read key:{}", key, e);
            return false;
        }
    }
}
