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

package org.cristalise.kernel.property;

import org.cristalise.kernel.lifecycle.ActivityDef;
import org.cristalise.kernel.lifecycle.instance.predefined.CreateItemFromDescription;
import org.cristalise.kernel.process.Bootstrap;


/**
 * Enumeration to make built-in Properties easier to maintain and document.
 * In general, Properties are Optional and Mutable unless stated otherwise.
 */
public enum BuiltInItemProperties {
    /**
     * Used in {@link ActivityDef} Items to manage Elementary or Composite ActivityDesc
     * Immutable.
     */
    COMPLEXITY("Complexity"),

    /**
     *  Used for generating Item Names: the prefix (e.g. PR) used as a beginning of the Name. See
     *  {@link CreateItemFromDescription#getItemName}
     */
    ID_PREFIX("IDPrefix"),

    /**
     * The Propoerty is created and updated during {@link Bootstrap} in the so-called Server Item.
     * It is read from a file on the classpath created during the build of the kernel.
     */
    KERNEL_VERSION("KernelVersion"),

    /**
     * Used for generating Item Names: the length of the number part of the name left padded by zeros. See
     * {@link CreateItemFromDescription#getItemName}
     */
    LEFT_PAD_SIZE("LeftPadSize"),

    /**
     *  Used for generating Item Names the last number used to generate the Item Name. See 
     *  {@link CreateItemFromDescription#getItemName}
     */
    LAST_COUNT("LastCount"),

    /**
     * The Name or the generated ID of the Item, specified by the Description/Factory Item and sent as the
     * first parameter in the {@link CreateItemFromDescription} PredefinedStep or generated during the execution of 
     * {@link CreateItemFromDescription#getItemName}. It will be automatically added even if it was not defined.
     * Mandatory.
     */
    NAME("Name"),

    /**
     * It only used in Module Items to specify a segment/folder in the DomainPath for all Items in the Module. 
     * The value is copied into the {@link BuiltInItemProperties#MODULE} Property of these Items.
     * Immutable.
     */
    NAMESPACE("Namespace"),

    /**
     * Describes the Module to which an Item belongs to. Normally it is the 
     * {@link BuiltInItemProperties#NAMESPACE} of the Module.
     * Immutable.
     */
    MODULE("Module"),

    /**
     * Used in Description/Factory Items to specify the Root DomainPath.
     * Immutable.
     */
    ROOT("Root"),

    /**
     * The type of the Item, it is a good practice to set it as an immutable, fixed value Property. 
     * It is used in Dev module extensively, and it is also used when checking permission 
     * for the 'domain' part of Shiro WildcardPermission (use 'SecurityDomain' Property to overwrite it)
     * Mandatory, Immutable.
     */
    TYPE("Type"),

    /**
     * It is used during the permission checks based on Shiro WildcardPermission. If not blank, it replaces the Type of the Item 
     * used for the 'domain' section in the constructed permission string.
     */
    SECURITY_DOMAIN("SecurityDomain"),

    /**
     * The version identifier of the Item.
     * Mandatory.
     */
    VERSION("Version"),

    /**
     * The UUID of the Schema Item and its Version number separated by colon ':'.
     * It is created during instantiation of the Schema Dependency of an ActivityDef Item.
     */
    SCHEMA_URN("SchemaURN"),

    /**
     * The UUID of the Script Item and its Version number separated by colon ':'.
     * It is created during instantiation of the Script Dependency of an ActivityDef Item.
     */
    SCRIPT_URN("ScriptURN"),

    /**
     * The UUID of the Query Item and its Version number separated by colon ':'.
     * It is created during instantiation of the Query Dependency of an ActivityDef Item.
     */
    QUERY_URN("QueryURN"),

    /**
     * The UUID of the StateMachine Item and its Version number separated by colon ':'. 
     * It is created during instantiation of the StateMachine Dependency of an ActivityDef Item.
     */
    STATE_MACHINE_URN("StateMachineURN"),

    /**
     * The UUID of the Schema Item and its Version number separated by colon ':'. 
     * Represents the unique resource name (URN) of a Schema identifying and associating it with Item(s) as a Master Schema. 
     */
    MASTER_SCHEMA_URN("MasterSchemaURN"),

    /**
     * The Name or the UUID of the Script Item and its Version separated by colon ':'. 
     * Represents the unique resource name (URN) for Script identifying and  associating it with Item(s) as an Aggregate Script. 
     */
    AGGREGATE_SCRIPT_URN("AggregateScriptURN"),

    /**
     * The Name or the UUID of the Schema Item and its Version separated by colon ':'. 
     * Represents the unique resource name (URN) for Schema identifying and associating it with Item(s) as an Update Schema. 
     */
    UPDATE_SCHEMA_URN("UpdateSchemaURN"),

    /**
     * The UUID of the Workflow Item and its Version number separated by colon ':'
     * Represents the unique resource name (URN) for CompositeActivityDef identifying and associating it with Item(s) as Workflow. 
     */
    WORKFLOW_URN("WorkflowURN");

    private final String propName;

    private BuiltInItemProperties(final String n) {
        propName = n;
    }

    public String getName() {
        return propName;
    }

    public String toString() {
        return getName();
    }

    public static BuiltInItemProperties getValue(String propName) {
        for (BuiltInItemProperties prop : BuiltInItemProperties.values()) {
            if(prop.getName().equals(propName) || prop.name().equals(propName)) return prop;
        }
        return null;
    }
}
