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
package org.cristalise.kernel.utils;

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.PROPERTY_DEF_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.PROPERTY_DEF_VERSION;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.QUERY_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.QUERY_VERSION;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCHEMA_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCHEMA_VERSION;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCRIPT_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCRIPT_VERSION;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.STATE_MACHINE_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.STATE_MACHINE_VERSION;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.imports.ImportAgent;
import org.cristalise.kernel.entity.imports.ImportItem;
import org.cristalise.kernel.entity.imports.ImportRole;
import org.cristalise.kernel.graph.model.BuiltInVertexProperties;
import org.cristalise.kernel.lifecycle.ActivityDef;
import org.cristalise.kernel.lifecycle.CompositeActivityDef;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.property.PropertyDescriptionList;
import org.cristalise.kernel.querying.Query;
import org.cristalise.kernel.scripting.Script;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocalObjectLoader {
    private static ActDefCache              actCache      = new ActDefCache(null);
    private static ActDefCache              compActCache  = new ActDefCache(true);
    private static ActDefCache              elemActCache  = new ActDefCache(false);
    private static StateMachineCache        smCache       = new StateMachineCache();
    private static SchemaCache              schCache      = new SchemaCache();
    private static ScriptCache              scrCache      = new ScriptCache();
    private static QueryCache               queryCache    = new QueryCache();
    private static PropertyDescriptionCache propDescCache = new PropertyDescriptionCache();
    private static AgentDescCache           agentDescCache = new AgentDescCache();
    private static ItemDescCache            itemDescCache = new ItemDescCache();
    private static RoleDescCache            roleDescCache = new RoleDescCache();

    /**
     * Retrieves a named version of a script from the database
     *
     * @param scriptName - script name
     * @param scriptVersion - integer script version
     * @return Script
     * @throws ObjectNotFoundException - When script or version does not exist
     * @throws InvalidDataException - When the stored script data was invalid
     * 
     */
    static public Script getScript(String scriptName, int scriptVersion) throws ObjectNotFoundException, InvalidDataException {
        return getScript(scriptName, scriptVersion, null);
    }

    static public Script getScript(String scriptName, int scriptVersion, TransactionKey transactionKey) throws ObjectNotFoundException, InvalidDataException {
        log.trace("getScript({} v{}) - transactionKey:{}", scriptName, scriptVersion, transactionKey);
        return scrCache.get(scriptName, scriptVersion, transactionKey);
    }

    /**
     * Retrieves a script from the database finding data in the Vertex properties
     * 
     * @param properties vertex properties
     * @return Script
     */
    /**
     * Retrieves a script from the database finding data in the Vertex properties
     * 
     * @param properties vertex properties
     * @return Script
     */
    static public Script getScript(CastorHashMap properties) throws InvalidDataException, ObjectNotFoundException {
        return getScript(properties, null);
    }

    static public Script getScript(CastorHashMap properties, TransactionKey transactionKey) throws InvalidDataException, ObjectNotFoundException {
        return (Script)getDescObjectByProperty(properties, SCRIPT_NAME, SCRIPT_VERSION, transactionKey);
    }

    /**
     * Retrieves a named version of a query from the database
     *
     * @param queryName - query name
     * @param queryVersion - integer query version
     * @return Query
     * @throws ObjectNotFoundException - When query or version does not exist
     * @throws InvalidDataException - When the stored query data was invalid
     * 
     */
    static public Query getQuery(String queryName, int queryVersion) throws ObjectNotFoundException, InvalidDataException {
        return getQuery(queryName, queryVersion, null);
    }

    static public Query getQuery(String queryName, int queryVersion, TransactionKey transactionKey) throws ObjectNotFoundException, InvalidDataException {
        log.trace("getQuery({} v{}) - transactionKey:{}", queryName, queryVersion, transactionKey);
        return queryCache.get(queryName, queryVersion, transactionKey);
    }

    /**
     * Retrieves a query from the database finding data in the Vertex properties
     * 
     * @param properties vertex properties
     * @return Query
     */
    static public Query getQuery(CastorHashMap properties) throws InvalidDataException, ObjectNotFoundException {
        return getQuery(properties, null);
    }

    static public Query getQuery(CastorHashMap properties, TransactionKey transactionKey) throws InvalidDataException, ObjectNotFoundException {
        return (Query)getDescObjectByProperty(properties, QUERY_NAME, QUERY_VERSION, transactionKey);
    }

    /**
     * Retrieves a named version of a schema from the database
     *
     * @param schemaName - schema name
     * @param schemaVersion - integer schema version
     * @return Schema
     * @throws ObjectNotFoundException - When schema or version does not exist
     * @throws InvalidDataException - When the stored schema data was invalid
     */
    static public Schema getSchema(String schemaName, int schemaVersion) throws ObjectNotFoundException, InvalidDataException {
        return getSchema(schemaName, schemaVersion, null);
    }

    static public Schema getSchema(String schemaName, int schemaVersion, TransactionKey transactionKey) throws ObjectNotFoundException, InvalidDataException {
        log.trace("getSchema({} v{}) - transactionKey:{}", schemaName, schemaVersion, transactionKey);

        // don't bother if this is the Schema schema - for bootstrap especially
        if (schemaName.equals("Schema") && schemaVersion == 0) {
            return new Schema(schemaName, schemaVersion, new ItemPath(new UUID(0, 5)), "");
        }

        return schCache.get(schemaName, schemaVersion, transactionKey);
    }

    /**
     * Retrieves a schema from the database finding data in the Vertex properties
     * 
     * @param properties vertex properties
     * @return Schema
     */
    static public Schema getSchema(CastorHashMap properties) throws InvalidDataException, ObjectNotFoundException {
        return getSchema(properties, null);
    }

    static public Schema getSchema(CastorHashMap properties, TransactionKey transactionKey) throws InvalidDataException, ObjectNotFoundException {
        return (Schema)getDescObjectByProperty(properties, SCHEMA_NAME, SCHEMA_VERSION, transactionKey);
    }

    /**
     * Retrieves a named version of ActivityDef from the database
     *
     * @param actName - activity name
     * @param actVersion - integer activity version
     * @return ActivityDef
     * @throws ObjectNotFoundException - When activity or version does not exist
     * @throws InvalidDataException - When the stored script data was invalid
     */
    static public ActivityDef getActDef(String actName, int actVersion) throws ObjectNotFoundException, InvalidDataException {
        return getActDef(actName, actVersion, null);
    }

    static public ActivityDef getActDef(String actName, int actVersion, TransactionKey transactionKey) throws ObjectNotFoundException, InvalidDataException {
        log.trace("getActDef({} v{}) - transactionKey:{}", actName, actVersion, transactionKey);
        return actCache.get(actName, actVersion, transactionKey);
    }

    /**
     * Retrieves a named version of CompositeActivityDef from the database
     *
     * @param actName - activity name
     * @param actVersion - integer activity version
     * @return ActivityDef
     * @throws ObjectNotFoundException - When activity or version does not exist
     * @throws InvalidDataException - When the stored script data was invalid
     */
    static public CompositeActivityDef getCompActDef(String actName, int actVersion) throws ObjectNotFoundException, InvalidDataException {
        return getCompActDef(actName, actVersion, null);
    }

    static public CompositeActivityDef getCompActDef(String actName, int actVersion, TransactionKey transactionKey) throws ObjectNotFoundException, InvalidDataException {
        log.trace("getCompActDef({} v{}) - transactionKey:{}", actName, actVersion, transactionKey);
        return (CompositeActivityDef)compActCache.get(actName, actVersion, transactionKey);
    }

    /**
     * Retrieves a named version of ActivityDef from the database
     *
     * @param actName - activity name
     * @param actVersion - integer activity version
     * @return ActivityDef
     * @throws ObjectNotFoundException - When activity or version does not exist
     * @throws InvalidDataException - When the stored script data was invalid
     */
    static public ActivityDef getElemActDef(String actName, int actVersion) throws ObjectNotFoundException, InvalidDataException {
        return getElemActDef(actName, actVersion, null);
    }

    static public ActivityDef getElemActDef(String actName, int actVersion, TransactionKey transactionKey) throws ObjectNotFoundException, InvalidDataException {
        log.trace("getElemActDef({} v{}) - transactionKey:{}", actName, actVersion, transactionKey);
        return elemActCache.get(actName, actVersion, transactionKey);
    }

    /**
     * Retrieves a named version of a StateMachine from the database
     *
     * @param smName - state machine name
     * @param smVersion - integer state machine version
     * @return StateMachine
     * @throws ObjectNotFoundException - When state machine or version does not exist
     * @throws InvalidDataException - When the stored state machine data was invalid
     */	
    static public StateMachine getStateMachine(String smName, int smVersion) throws ObjectNotFoundException, InvalidDataException {
        return getStateMachine(smName, smVersion, null);
    }

    static public StateMachine getStateMachine(String smName, int smVersion, TransactionKey transactionKey) throws ObjectNotFoundException, InvalidDataException {
        log.trace("getStateMachine({} v{}) - transactionKey:{}", smName, smVersion, transactionKey);
        return smCache.get(smName, smVersion, transactionKey);
    }

    /**
     * 
     * @param properties
     * @return
     * @throws InvalidDataException
     * @throws ObjectNotFoundException
     */
    static public StateMachine getStateMachine(CastorHashMap properties) throws InvalidDataException, ObjectNotFoundException {
        return getStateMachine(properties, null);
    }

    static public StateMachine getStateMachine(CastorHashMap properties, TransactionKey transactionKey) throws InvalidDataException, ObjectNotFoundException {
        return (StateMachine)getDescObjectByProperty(properties, STATE_MACHINE_NAME, STATE_MACHINE_VERSION, transactionKey);
    }

    /**
     * 
     * @param name
     * @param version
     * @return
     * @throws ObjectNotFoundException
     * @throws InvalidDataException
     */
    static public PropertyDescriptionList getPropertyDescriptionList(String name, int version) throws ObjectNotFoundException, InvalidDataException {
        return getPropertyDescriptionList(name, version, null);
    }

    static public PropertyDescriptionList getPropertyDescriptionList(String name, int version, TransactionKey transactionKey) throws ObjectNotFoundException, InvalidDataException {
        log.trace("getPropertyDescriptionList({} v{}) - transactionKey:{}", name, version, transactionKey);
        return propDescCache.get(name, version, transactionKey);
    }

    /**
     * 
     * @param properties
     * @return
     * @throws InvalidDataException
     * @throws ObjectNotFoundException
     */
    static public PropertyDescriptionList getPropertyDescriptionList(CastorHashMap properties) throws InvalidDataException, ObjectNotFoundException {
        return getPropertyDescriptionList(properties, null);
    }

    static public PropertyDescriptionList getPropertyDescriptionList(CastorHashMap properties, TransactionKey transactionKey) throws InvalidDataException, ObjectNotFoundException {
        return (PropertyDescriptionList)getDescObjectByProperty(properties, PROPERTY_DEF_NAME, PROPERTY_DEF_VERSION, transactionKey);
    }

    static public ImportAgent getAgentDesc(String name, int version) throws ObjectNotFoundException, InvalidDataException {
        return getAgentDesc(name, version, null);
    }

    static public ImportAgent getAgentDesc(String name, int version, TransactionKey transactionKey) throws ObjectNotFoundException, InvalidDataException {
        log.trace("getAgentDesc(({} v{}) - transactionKey:{}", name, version, transactionKey);
        return agentDescCache.get(name, version, transactionKey);
    }

    static public ImportItem getItemDesc(String name, int version) throws ObjectNotFoundException, InvalidDataException {
        return getItemDesc(name, version, null);
    }

    static public ImportItem getItemDesc(String name, int version, TransactionKey transactionKey) throws ObjectNotFoundException, InvalidDataException {
        log.trace("getItemDesc(({} v{}) - transactionKey:{}", name, version, transactionKey);
        return itemDescCache.get(name, version, transactionKey);
    }

    static public ImportRole getRoleDesc(String name, int version) throws ObjectNotFoundException, InvalidDataException {
        return getRoleDesc(name, version, null);
    }

    static public ImportRole getRoleDesc(String name, int version, TransactionKey transactionKey) throws ObjectNotFoundException, InvalidDataException {
        log.trace("getRoleDesc(({} v{}) - transactionKey:{}", name, version, transactionKey);
        return roleDescCache.get(name, version, transactionKey);
    }

    /**
     * 
     * @param properties
     * @param nameProp
     * @param verProp
     * @return DescriptionObject
     * @throws InvalidDataException
     * @throws ObjectNotFoundException
     */
    private static DescriptionObject getDescObjectByProperty(CastorHashMap properties, BuiltInVertexProperties nameProp, BuiltInVertexProperties verProp, TransactionKey transactionKey)
            throws InvalidDataException, ObjectNotFoundException
    {
        log.trace("getDescObjectByProperty(nameProp:{} verProp:{}) - transactionKey:{}", nameProp, verProp, transactionKey);

        String resName = (String) properties.getBuiltInProperty(nameProp);

        if (!(properties.isAbstract(nameProp)) && StringUtils.isNotBlank(resName)) {
            Integer resVer = deriveVersionNumber(properties.getBuiltInProperty(verProp));

            if (resVer == null && !(properties.isAbstract(verProp))) {
                throw new InvalidDataException("Invalid version property '" + resVer + "' in " + verProp);
            }

            switch (nameProp) {
                case SCHEMA_NAME:        return getSchema(resName, resVer, transactionKey);
                case SCRIPT_NAME:        return getScript(resName, resVer, transactionKey);
                case QUERY_NAME :        return getQuery(resName, resVer, transactionKey);
                case STATE_MACHINE_NAME: return getStateMachine(resName, resVer, transactionKey);
                case PROPERTY_DEF_NAME:  return getPropertyDescriptionList(resName, resVer, transactionKey);
                default:
                    throw new InvalidDataException(" CANNOT handle BuiltInVertexProperties:"+nameProp);
            }
        }
        return null;
    }

    /**
     * Converts Object to an Integer representing a version number.
     * 
     * @param value the Object containing a version number
     * @return the converted Version number. Set to null if value was null or-1
     */
    public static Integer deriveVersionNumber(Object value) throws InvalidDataException {
        if (value == null || "".equals(value)) return null;

        try {
            Integer version = Integer.valueOf(value.toString());

            if(version == -1) return null;
            return            version;
        }
        catch (NumberFormatException ex) {
            throw new InvalidDataException("Invalid version number : "+value.toString());
        }
    }
}
