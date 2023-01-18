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

package org.cristalise.kernel.process.resource;

import org.cristalise.kernel.entity.DomainContext;
import org.cristalise.kernel.entity.imports.ImportAgent;
import org.cristalise.kernel.entity.imports.ImportItem;
import org.cristalise.kernel.entity.imports.ImportRole;
import org.cristalise.kernel.lifecycle.ActivityDef;
import org.cristalise.kernel.lifecycle.CompositeActivityDef;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.process.module.Module;
import org.cristalise.kernel.property.PropertyDescriptionList;
import org.cristalise.kernel.querying.Query;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.utils.DescriptionObject;
import lombok.Getter;

/**
 *
 */
@Getter
public enum BuiltInResources {
    //                     typeCode,    schemaName,              typeRoot,              workflowDef
    ACTIVITY_DESC_RESOURCE( "AC",       "ActivityDef",           "/desc/ActivityDesc",  null), //'abstract' resource - does not have an Item
    PROPERTY_DESC_RESOURCE( "property", "PropertyDescription",   "/desc/PropertyDesc",  "ManagePropertyDesc"),
    MODULE_RESOURCE(        "module",   "Module",                "/desc/Module",        "ManageModule"),
    SCHEMA_RESOURCE(        "OD",       "Schema",                "/desc/Schema",        "ManageSchema"),
    SCRIPT_RESOURCE(        "SC",       "Script",                "/desc/Script",        "ManageScript"),
    QUERY_RESOURCE(         "query",    "Query",                 "/desc/Query",         "ManageQuery"),
    STATE_MACHINE_RESOURCE( "SM",       "StateMachine",          "/desc/StateMachine",  "ManageStateMachine"),
    COMP_ACT_DESC_RESOURCE( "CA",       "CompositeActivityDef",  "/desc/ActivityDesc",  "ManageCompositeActDef"),
    ELEM_ACT_DESC_RESOURCE( "EA",       "ElementaryActivityDef", "/desc/ActivityDesc",  "ManageElementaryActDef"),
    ITEM_DESC_RESOURCE(     "item",     "Item",                  "/desc/ItemDesc",      "ManageItemDesc"),
    AGENT_DESC_RESOURCE(    "agent",    "Agent",                 "/desc/AgentDesc",     "ManageAgentDesc"),
    ROLE_DESC_RESOURCE(     "role" ,    "Role",                  "/desc/RoleDesc",      "ManageRoleDesc"),
    DOMAIN_CONTEXT_RESOURCE("context" , "DomainContext",         "/desc/DomainContext", "ManageDomainContext");

    private String  typeCode;
    private String  schemaName;
    private String  typeRoot;
    private String  workflowDef;

    private BuiltInResources(final String code, final String schema, final String root, final String wf) {
        typeCode = code;
        schemaName = schema;
        typeRoot = root;
        workflowDef = wf;
    }

    @Override
    public String toString() {
        return getTypeCode();
    }

    public static BuiltInResources getValue(String value) {
        for (BuiltInResources res : BuiltInResources.values()) {
            if(res.getTypeCode().equals(value) || 
               res.getSchemaName().equals(value) || 
               res.name().equals(value))
            {
                return res;
            }
        }
        return null;
    }

    public static BuiltInResources getValue(DescriptionObject descObject) {
        switch (descObject.getClass().getSimpleName()) {
            case "PropertyDescriptionList": return PROPERTY_DESC_RESOURCE;
            case "Module":                  return MODULE_RESOURCE;
            case "Schema":                  return SCHEMA_RESOURCE;
            case "Script":                  return SCRIPT_RESOURCE;
            case "Query":                   return QUERY_RESOURCE;
            case "StateMachine":            return STATE_MACHINE_RESOURCE;
            case "CompositeActivityDef":    return COMP_ACT_DESC_RESOURCE;
            case "ActivityDef":             return ELEM_ACT_DESC_RESOURCE;
            case "ImportItem":              return ITEM_DESC_RESOURCE;
            case "ImportAgent":             return AGENT_DESC_RESOURCE;
            case "ImportRole":              return ROLE_DESC_RESOURCE;
            case "DomainContext":           return DOMAIN_CONTEXT_RESOURCE;
            default:
                return null;
        }
    }

    public DescriptionObject getDescriptionObject(String name) {
        DescriptionObject descObj;

        switch(this) {
            case ACTIVITY_DESC_RESOURCE: descObj = null; break; //abstract resource
            case MODULE_RESOURCE:        descObj = new Module(); break; 
            case SCHEMA_RESOURCE:        descObj = new Schema(null); break; 
            case SCRIPT_RESOURCE:        descObj = new Script(); break; 
            case QUERY_RESOURCE:         descObj = new Query(); break; 
            case PROPERTY_DESC_RESOURCE: descObj = new PropertyDescriptionList(); break;
            case COMP_ACT_DESC_RESOURCE: descObj = new CompositeActivityDef(); break;
            case ELEM_ACT_DESC_RESOURCE: descObj = new ActivityDef(); break;
            case STATE_MACHINE_RESOURCE: descObj = new StateMachine(); break;
            case ITEM_DESC_RESOURCE:     descObj = new ImportItem(); break;
            case AGENT_DESC_RESOURCE:    descObj = new ImportAgent(); break;
            case ROLE_DESC_RESOURCE:     descObj = new ImportRole(); break;
            case DOMAIN_CONTEXT_RESOURCE:descObj = new DomainContext(); break;

            default:
                return null;
        }

        if (descObj != null) descObj.setName(name);

        return descObj;
    }

    private String getActivityTypeText() {
        switch (this) {
            case COMP_ACT_DESC_RESOURCE:
            case ELEM_ACT_DESC_RESOURCE:
                return "Activity";

            case ITEM_DESC_RESOURCE:
            case AGENT_DESC_RESOURCE:
            case ROLE_DESC_RESOURCE:
                return getSchemaName() + "Desc";

            default:
                return getSchemaName();
        }
    }

    public String getEditActivityName() {
        return "EditDefinition";
    }

    public String getMoveVersionActivityName() {
        return "MoveLatest" + getActivityTypeText() + "VersionToLast";
    }

    public String getAssignVersionActivityName() {
        return "AssignNew" + getActivityTypeText() + "VersionFromLast";
    }
}
