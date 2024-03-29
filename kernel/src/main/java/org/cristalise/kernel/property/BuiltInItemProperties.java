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

import org.cristalise.kernel.lifecycle.instance.predefined.CreateItemFromDescription;


/**
 * Helper enumeration to make built-in Property names easier to maintain and document
 */
public enum BuiltInItemProperties {
    AGGREGATE_SCRIPT_URN("AggregateScriptURN"),

    /**
     * Used in description Items to manage Elementary or Composite Activity Definition
     */
    COMPLEXITY("Complexity"),

    /**
     *  Used for generating Names: the prefix (e.g. PR) used as a beginning of the Name
     */
    ID_PREFIX("IDPrefix"),

    /**
     * 
     */
    KERNEL_VERSION("KernelVersion"),

    /**
     * Used for generating Names: the length of the number part left padded by zeros
     */
    LEFT_PAD_SIZE("LeftPadSize"),

    /**
     *  Used for generating Names: the last number used to generate a Name
     */
    LAST_COUNT("LastCount"),

    MASTER_SCHEMA_URN("MasterSchemaURN"),

    /**
     * The name or ID of the Item, specified by the Factory Item and sent as the first parameter in the
     * {@link CreateItemFromDescription} predefined Step or generated during the execution of {@link CreateItemFromDescription}.
     * It will be automatically added even if it was not defined.
     */
    NAME("Name"),

    NAMESPACE("Namespace"),

    MODULE("Module"),

    /**
     * Used in factory Items to specify the Root DomainPath.
     */
    ROOT("Root"),

    /**
     * The type of the Item, it is a good practice to set it as an immutable, fixed value Property. 
     * It is convenient to use in lookup.search() to find Items of the same type.
     * It is used in Dev module extensively, and it is also used when checking permission 
     * for the 'domain' part of Shiro WildcardPermission (use 'SecurityDomain' Property to overwrite it)
     */
    TYPE("Type"),

    /**
     * It is used during the permission checks based on Shiro WildcardPermission. If not blank it replaces the type of the Item 
     * used for the 'domain' section in the constructed permission string.
     * 
     * Can be null or undefined.
     */
    SECURITY_DOMAIN("SecurityDomain"),

    VERSION("Version"),

    /**
     * The UUID of the Schema Item and its Version number separated by colon ':'. It is created during 
     * instantiation of the Schema Dependency.
     */
    SCHEMA_URN("SchemaURN"),

    /**
     * The UUID of the Script Item and its Version number separated by colon ':'. It is created during 
     * instantiation of the Script Dependency.
     */
    SCRIPT_URN("ScriptURN"),

    /**
     * The UUID of the Query Item and its Version number separated by colon ':'. It is created during 
     * instantiation of the Query Dependency.
     */
    QUERY_URN("QueryURN"),

    /**
     * The UUID of the StateMachine Item and its Version number separated by colon ':'. It is created during 
     * instantiation of the StateMachine Dependency.
     */
    STATE_MACHINE_URN("StateMachineURN"),

    /**
     * The Name or the UUID of the Schema Item and its Version separated by colon ':'. It is specified in Factory Items
     * is used while creating new Items.
     */
    UPDATE_SCHEMA("UpdateSchema"),

    /**
     * The UUID of the Workflow Item and its Version number separated by colon ':'
     */
    WORKFLOW_URN("WorkflowURN");

    private String propName;

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
