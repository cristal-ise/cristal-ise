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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.C2KLocalObjectMap;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.persistency.outcome.Schema;

/**
 * The History is an instance of {@link org.cristalise.kernel.persistency.C2KLocalObjectMap} 
 * which provides a live view onto the Events of an Item.
 */
public class History extends C2KLocalObjectMap<Event> {

    public History(ItemPath itemPath) {
        super(itemPath, HISTORY);
    }

    public History(ItemPath itemPath, TransactionKey transactionKey) {
        super(itemPath, HISTORY, transactionKey);
    }

    public Event getEvent(int id) {
        return get(String.valueOf(id));
    }

    @Override
    public Event remove(Object key) {
        throw new UnsupportedOperationException("Event cannot be removed");
    }

    private synchronized Event storeNewEvent(Event newEvent) {
        newEvent.setID( getLastId()+1);
        put(newEvent.getName(), newEvent);
        return newEvent;
    }

    public Event addEvent(AgentPath agentPath, String agentRole,
                          String stepName, String stepPath, String stepType, 
                          StateMachine stateMachine, int transitionId)
    {
        return storeNewEvent(
                new Event(itemPath, agentPath, agentRole, stepName, stepPath, stepType, stateMachine, transitionId));
    }

    public Event addEvent(AgentPath agentPath, String agentRole,
                          String stepName, String stepPath, String stepType, Schema schema, 
                          StateMachine stateMachine, int transitionId, String viewName)
    {
        Event newEvent = new Event(itemPath,agentPath, agentRole, stepName, stepPath, stepType, stateMachine, transitionId);
        newEvent.addOutcomeDetails(schema, viewName);
        return storeNewEvent(newEvent);
    }

    public Event addEvent(AgentPath agentPath, String agentRole,
                          String stepName, String stepPath, String stepType, Schema schema, 
                          StateMachine stateMachine, int transitionId, String viewName, boolean hasAttachment)
    {
        Event newEvent = new Event(itemPath,agentPath, agentRole, stepName, stepPath, stepType, stateMachine, transitionId, hasAttachment);
        newEvent.addOutcomeDetails(schema, viewName);
        return storeNewEvent(newEvent);
    }

    public Event addEvent(AgentPath agentPath, String agentRole,
                          String stepName, String stepPath, String stepType,
                          StateMachine stateMachine, int transitionId, String timeString) 
           throws InvalidDataException
    {
        Event newEvent = new Event(itemPath, agentPath, agentRole, stepName, stepPath, stepType, stateMachine, transitionId);
        newEvent.setTimeString(timeString);
        return storeNewEvent(newEvent);
    }

    public Event addEvent(AgentPath agentPath, String agentRole,
                          String stepName, String stepPath, String stepType, Schema schema, 
                          StateMachine stateMachine, int transitionId, String viewName, String timeString) 
           throws InvalidDataException
    {
        Event newEvent = new Event(itemPath, agentPath, agentRole, stepName, stepPath, stepType, stateMachine, transitionId);
        newEvent.addOutcomeDetails(schema, viewName);
        newEvent.setTimeString(timeString);
        return storeNewEvent(newEvent);
    }

    /**
     * History contains the consecutive list of Event based in integer IDs, which means the list of Keys
     * can be computed from lastId;
     */
    @Override
    protected Set<String> loadKeys(String path) throws PersistencyException {
        Set<String> keys = new HashSet<>();

        for (int i = 0; i <= getLastId(); i++) keys.add(String.valueOf(i));

        return keys;
    }

    @SuppressWarnings("unlikely-arg-type")
    public LinkedHashMap<String, Event> list(int start, int batchSize, Boolean descending) {
        LinkedHashMap<String, Event> batch = new LinkedHashMap<>();

        int last = getLastId();

        if (descending) {
            int i = last - start;

            while (i >= 0 && batch.size() < batchSize) {
                batch.put(String.valueOf(i), get(i));
                i--;
            }

            // 'nextBatch' is not returned to the client, check ItemHistory.listEvents()
            // while (i >= 0 && map.get(i) == null) i--;
            // if (i >= 0) {
            // batch.put("nextBatch", uri.getAbsolutePathBuilder().replaceQueryParam("start", i).replaceQueryParam("batch",
            // batchSize).build());
            // }
        }
        else {
            int i = start;

            while (i <= last && batch.size() < batchSize) {
                batch.put(String.valueOf(i), get(i));
                i++;
            }

            // 'nextBatch' is not returned to the client, check ItemHistory.listEvents()
            // while (i <= last && map.get(i) == null) i++;
            // if (i <= last) {
            // batch.put("nextBatch", uri.getAbsolutePathBuilder().replaceQueryParam("start", i).replaceQueryParam("batch",
            // batchSize).build());
            // }
        }

        return batch;
    }

    @Override
    public Set<Entry<String, Event>> entrySet() {
        throw new UnsupportedOperationException("Cannot retrieve full content of History:"+itemPath);
    }

    @Override
    public Collection<Event> values() {
        throw new UnsupportedOperationException("Cannot retrieve full content of History:"+itemPath);
    }
    

    @Override
    public int size() {
        return getLastId()+1;
    }
}
