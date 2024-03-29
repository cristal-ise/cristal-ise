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
package org.cristalise.kernel.graph.model;

import org.cristalise.kernel.SystemProperties;
import org.cristalise.kernel.collection.Collection;

/**
 * Enumeration to define all Vertex properties which are used by collection and lifecycle packages
 *
 */
public enum BuiltInVertexProperties {
    /**
     * Boolean property. Makes CompositeActitivy abortable, i.e. it can be finished even if it has active children.
     */
    ABORTABLE("Abortable"),

    ACTIVITY_DEF_NAME("ActivityDefName"),
    ACTIVITY_DEF_VERSION("ActivityDefVersion"),

    ACTIVITY_DEF_URN("ActivityDefURN"),

    /**
     * String property. The name of the Agent associated with Activities. Can be null or undefined.
     */
    AGENT_NAME("Agent Name"),

    /**
     * String property. Contains the comma separated list of mine types. Defined in ActivityDef. Can be null or undefined.
     */
    ATTACHMENT_MIME_TYPES("AttachmentMimeTypes"),

    /**
     * String property. The role of the Agent associated with Activities. It should only be used to
     * for UserCode like functionality. Can be null or undefined.
     */
    AGENT_ROLE("Agent Role"),

    /**
     * Boolean property ...
     */
    BREAKPOINT("Breakpoint"),

    /**
     * String property of Collection specifying the cardinality. It contains one of the values of {@link Collection.Cardinality} 
     */
    DEPENDENCY_CARDINALITY("DependencyCardinality"),

    /**
     * String property of Activity specifying the name(s) of the Dependency updated by the Activity. 
     * May contain a comma separated list. In DSL it is used like this:
     *
     * <pre>
     * Activity('User_AddAddress', 0) {
     *   Property(PredefinedStep: 'AddMemberToCollection')
     *   Property(DependencyName: 'Addresses')
     *   Schema('Address_Details', 0)
     * }</pre>
     * 
     * Can be null or undefined.
     */
    DEPENDENCY_NAME("DependencyName"),

    /**
     * String property of Collection specifying the Name of the Collection in the other Item. 
     * It is used in case of Bidirectional Dependency.
     */
    DEPENDENCY_TO("DependencyTo"),

    /**
     * String property of Collection specifying the type. It contains one of the values of {@link Collection.Type}
     */
    DEPENDENCY_TYPE("DependencyType"),

    /**
     * default: false
     */
    DEPENDENCY_ALLOW_DUPLICATE_ITEMS("DependencyAllowDuplicateItems"),

    /**
     * default: false
     */
    DEPENDENCY_DISABLE_TYPE_CHECK("DependencyDisableTypeCheck"),

    /**
     * String property used in ActivityDef to store the description text
     */
    DESCRIPTION("Description"),

    /**
     * Used in Splits as a counter to provide unique ID for instances of {@link DirectedEdge}
     */
    LAST_NUM("LastNum"),

    /**
     * String property used in CollectionDefinition to specify the Script to be executed during AddMembersToCollection. 
     * Its primary purpose is to  ensure referential integrity. It shall contain the Name or UUID of the Script and the version number 
     * separated with colon.
     * 
     * <pre>{@code
     * <Dependency name="Employees" isDescription="true">
     *   <DependencyMember itemPath="/domain/path/EmployeeFactory" />
     *   <CollectionProperties>
     *     <KeyValuePair Key="MemberAddScript" String="Department_CheckEmployees:0"/>
     *   </CollectionProperties>
     * </Dependency>
     * }</pre>
     */
    MEMBER_ADD_SCRIPT("MemberAddScript"),

    /**
     * String property used in CollectionDefinition to specify the Script to be executed during RemoveSlotFromCollection. 
     * Its primary purpose is to  ensure referential integrity. It shall contain the Name or UUID of the Script and the version number 
     * separated with colon.
     * 
     * <pre>{@code
     * <Dependency name="Employees" isDescription="true">
     *   <DependencyMember itemPath="/domain/path/EmployeeFactory" />
     *   <CollectionProperties>
     *     <KeyValuePair Key="MemberRemoveScript" String="Department_CheckEmployees:0"/>
     *   </CollectionProperties>
     * </Dependency>
     * }</pre>
     */
    MEMBER_REMOVE_SCRIPT("MemberRemoveScript"),

    /**
     * String property. Used in CollectionDefinition to specify the Script to be executed during UpdateDependencyMamber. 
     * Its primary purpose is to  ensure referential integrity. It shall contain the Name or UUID of the Script and the version number 
     * separated with colon.
     * 
     * <pre>{@code
     * <Dependency name="Employees" isDescription="true">
     *   <DependencyMember itemPath="/domain/path/EmployeeFactory" />
     *   <CollectionProperties>
     *     <KeyValuePair Key="MemberUpdateScript" String="Department_CheckEmployees:0"/>
     *   </CollectionProperties>
     * </Dependency>
     * }</pre>
     */
    MEMBER_UPDATE_SCRIPT("MemberUpdateScript"),

    /**
     * String property. It is used in ActivityDef and ActivitySlotDef to override the name of the ActivityDef
     */
    NAME("Name"),

    /**
     * String property. It is used in ActivityDef or ActivitySlotDef
     */
    NAMESPACE("Namespace"),

    /**
     * String property. The type of object the Activity is going to create. Values are Schema, Script, StateMachine and Query.
     * Used in script DescriptionCollectionSetter. Can be null or undefined.
     */
    OBJECT_TYPE("ObjectType"),

    /**
     * String property to hold the name of the OutcomeInititator to be used by the Job associated with Activities.
     * The name is used to find the class name defined in the Config section of the module.xml. For example, the
     * OutcomeInitiator named <b>Empty</b> is defined like this:
     *
     * <pre>
     * {@code<Config name="OutcomeInit.Empty">org.cristalise.kernel.persistency.outcome.EmptyOutcomeInitiator</Config>}
     * </pre>
     *
     * Can be null or undefined.
     */
    OUTCOME_INIT("OutcomeInit"),

    /**
     * String property. It is used to find the Split-Join pairs to calculate if all the branches were finished.
     *
     * Can be null or undefined.
     */
    PAIRING_ID("PairingID"),

    /**
     * String property. Declares the Activity to be associated with a PredefinedStep(s), therefore the Outcome
     * shall contain the data required to execute automatically the predefined step(s). 
     * May contain a comma separated list.
     * 
     * In DSL it is used like this:
     *
     * <pre>
     * Activity('Equipment_AddDevice', 0) {
     *   Property(PredefinedStep: 'AddMembersToCollection')
     *   Schema('Equipment_Device', 0)
     * }</pre>
     * 
     * Can be null or undefined.
     */
    PREDEFINED_STEP("PredefinedStep"),

    /**
     * String property. It contains either the name or the UUID of the PropertyDescription Item.
     *
     * Can be null or undefined.
     */
    PROPERTY_DEF_NAME("PropertyDefName"),
    
    /**
     * Integer property. It contains the version number of PropertyDescription Item.
     *
     * Can be null or undefined.
     */
    PROPERTY_DEF_VERSION("PropertyDefVersion"),

    /**
     * Boolean property. Enables the Loop Transition of the CompositeActivity StateMachine
     */
    REPEAT_WHEN("RepeatWhen"),

    /**
     * String property. Overrides the Root Item Property used in factory Items to specify the Root DomainPath.
     */
    ROOT("Root"),

    /**
     * String property. Routing expression associated with Splits. It is interpreted by the Script class. The content should
     * start with the script engine name followed by the expression separated by a column like this:
     *
     * <pre>
     * javascript: new java.lang.Integer(counter % 2);
     * </pre>
     */
    ROUTING_EXPR("RoutingExpr"),

    /**
     * String property to hold either the name of the Script or the UUID of the Schema Item associated with Splits.
     * If RoutingScriptVersion is null the content if any is interpreted like {@link #ROUTING_EXPR}.
     *
     * Can be null or undefined.
     */
    ROUTING_SCRIPT_NAME("RoutingScriptName"),

    /**
     * Integer property to hold the version of the Schema associated with Splits.
     *
     * Can be null or undefined.
     */
    ROUTING_SCRIPT_VERSION("RoutingScriptVersion"),

    /**
     * String property. Either the name of the Schema or the UUID of the Schema Item associated with Activities.
     *
     * Can be null or undefined.
     */
    SCHEMA_NAME("SchemaType"),

    /**
     * Integer property to hold the version of the Schema associated with Activities.
     *
     * Can be null or undefined.
     */
    SCHEMA_VERSION("SchemaVersion"),

    /**
     * String property. Either the name of the Schema or the UUID of the Schema Item associated with Activities.
     *
     * Can be null or undefined.
     */
    SCRIPT_NAME("ScriptName"),

    /**
     * Integer property to hold the version of the Script associated with Activities.
     *
     * Can be null or undefined.
     */
    SCRIPT_VERSION("ScriptVersion"),

    /**
     * It is used during the permission checks based on Shiro WildcardPermission. If not blank it replaces the name of the Activity 
     * used for the 'actions' section in the constructed permission string.
     * 
     * Can be null or undefined.
     */
    SECURITY_ACTION("SecurityAction"),

    /**
     * 
     */
    SIMPLE_ELECTRONIC_SIGNATURE("SimpleElectonicSignature"),

    /**
     * String property. Either the name of the Schema or the UUID of the Schema Item associated with Activities.
     *
     * Can be null or undefined.
     */
    QUERY_NAME("QueryName"),

    /**
     * Integer property to hold the version of the Script associated with Activities.
     *
     * Can be null or undefined.
     */
    QUERY_VERSION("QueryVersion"),

    /**
     * String property to hold either the name of the StateMachine or the UUID of the StateMachine
     * Item associated with Activities. Can be null or undefined. The default StateMachine is called Default
     *
     * Can be null or undefined.
     */
    STATE_MACHINE_NAME("StateMachineName"),

    /**
     * Integer property to hold the version of the StateMachine associated with Activities.
     *
     * Can be null or undefined.
     */
    STATE_MACHINE_VERSION("StateMachineVersion"),

    /**
     * String property. It is used in Join and JoinDef to to specify if the join is a route or not
     *
     * Can be null or undefined.
     */
    TYPE("Type"),

    /**
     * Boolean property to trigger Outcome validation before creating the entry in the ClusterStore. 
     * Default value is false, which can be overwritten by the {@link SystemProperties#Activity_validateOutcome}.
     */
    VALIDATE_OUTCOME("ValidateOutcome"),

    /**
     * Integer property. It is used in CollectionMember to store the version of DescriptionDependency
     */
    VERSION("Version"),

    /**
     * String property used in Script CreateNewNumberedVersionFromLast and SetLastNumberedVersionFromLast
     * to specify {@link org.cristalise.kernel.persistency.outcome.Viewpoint#setSchemaName(String)}.
     */
    VIEW_NAME("ViewName"),

    /**
     * String property used to specify {@link org.cristalise.kernel.persistency.outcome.Viewpoint#setName(String)}.
     */
    VIEW_POINT("Viewpoint");

    /**
     * Provides the actual string name of the Property used in the kernel
     */
    private String propertyName;

    private BuiltInVertexProperties(final String n) {
        propertyName = n;
    }

    /**
     * @return kernel defined string value
     */
    public String getName() {
        return propertyName;
    }

    @Override
    public String toString() {
        return getName();
    }

    public static BuiltInVertexProperties getValue(String propName) {
        for (BuiltInVertexProperties prop : BuiltInVertexProperties.values()) {
            if(prop.getName().equals(propName) || prop.name().equals(propName)) return prop;
        }
        return null;
    }
}
