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
package org.cristalise.kernel;

import org.cristalise.kernel.graph.model.BuiltInVertexProperties;
import org.cristalise.kernel.lifecycle.CompositeActivityDef;
import org.cristalise.kernel.lifecycle.instance.predefined.BulkErase;
import org.cristalise.kernel.lifecycle.instance.predefined.item.CreateItemFromDescription;
import org.cristalise.kernel.lifecycle.instance.predefined.server.BulkImport;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.utils.DescriptionObject;
import org.cristalise.kernel.utils.SystemPropertyOperations;

import io.vertx.serviceproxy.ServiceException;
import lombok.Getter;

/**
 * Defines all SystemProperties that are supported in the kernel to configure the behavior of the
 * application. Due to the limitation of javadoc, the actual usable string cannot be shown easily,
 * therefore replace underscores with dots to get the actual System Property:
 * 
 * <pre>
 *   Module_ImportAgent_enableRoleCreation => Module.ImportAgent.enableRoleCreation
 * </pre>
 * 
 * @see #Activity_validateOutcome
 * @see #Authenticator
 * @see #BulkErase_limit
 * @see #BulkImport_fileExtension
 * @see #BulkImport_rootDirectory
 * @see #BulkImport_useDirectories
 * @see #ClusterStorage
 * @see #ClusterStorage_cacheSpec
 * @see #Dependency_addStateMachineURN
 * @see #Dependency_addWorkflowURN
 * @see #Dependency_checkMemberUniqueness
 * @see #Export_replaceActivitySlotDefUUIDWithName
 * @see #ItemServer_name
 * @see #ItemServer_Telnet_host
 * @see #ItemServer_Telnet_port
 * @see #ItemVerticle_ebAddress
 * @see #ItemVerticle_includeDebugInfo
 * @see #ItemVerticle_instances
 * @see #ItemVerticle_isWorker
 * @see #ItemVerticle_requestTimeoutSeconds
 * @see #Lifecycle_Sign_agentNameField
 * @see #Lifecycle_Sign_passwordField
 * @see #Lifecycle_Sign_signedFlagField
 * @see #LocalChangeVerticle_publishLocalMessage
 * @see #LocalObjectLoader_lookupUseProperties
 * @see #Module_ImportAgent_enableRoleCreation
 * @see #Module_reset
 * @see #Module_Versioning_strict
 * @see #Outcome_Validation_useDOM
 * @see #OutcomeInit_jobUseViewpoint
 * @see #Resource_moduleUseFileNameWithVersion
 * @see #Resource_useOldImportFormat
 * @see #RoutingScript_enforceStringReturnValue
 * @see #Shiro_iniFile
 * @see #StateMachine_Composite_default
 * @see #StateMachine_Elementary_default
 * @see #StateMachine_enableErrorHandling
 * @see #StateMachine_Predefined_default
 * @see #SystemProperties_keywordsToRedact
 * @see #TcpBridge_host
 * @see #TcpBridge_port
 * @see #XMLStorage_root
 */
public enum SystemProperties implements SystemPropertyOperations {

    /**
     * Enables OutcomeValidation during database transaction (aka server-side validation). Default
     * value is 'false', because Outcomes are validated in client code (e.g. during restapi call).
     * Such a validation can be enabled using {@link BuiltInVertexProperties#VALIDATE_OUTCOME}
     * Activity property as well.
     */
    Activity_validateOutcome("Activity.validateOutcome", false),
    /**
     * Specify the Authenticator implementation to be used. The default value is 'Shiro', which will use the Shiro
     * integration with authentication realms, or it could be a java class implementing the {@link Authenticator} interface.
     */
    Authenticator("Authenticator", "Shiro"),
    /**
     * Defines the paging size used during {@link BulkErase} predefined step. Defaul is 0, which
     * means there is no limit in deleting Items
     */
    BulkErase_limit("BulkErase.limit", 0),
    /**
     * Defines file extension to be used by the {@link BulkImport} predefined step. Default value is
     * empty string
     */
    BulkImport_fileExtension("BulkImport.fileExtension", ""),
    /**
     * Defines root directory used by the {@link BulkImport} predefined step. No default value
     */
    BulkImport_rootDirectory("BulkImport.rootDirectory"),
    /**
     * Defines if {@link BulkImport} predefined step is based on directory structure or not. Default
     * value is 'false'
     */
    BulkImport_useDirectories("BulkImport.useDirectories", false),
    /**
     * Defines the class to be used as a {@link ClusterStorage} implementation. No default value.
     */
    ClusterStorage("ClusterStorage"),
    /**
     * Specifies the Google Guava cache behavior used in ClusterStorageManager. 
     * Default is value is 'expireAfterAccess = 600s, recordStats'
     */
    ClusterStorage_cacheSpec("ClusterStorage.cacheSpec", "expireAfterAccess = 600s, recordStats"),
    /**
     * Add or not a new Item Property 'StaeMachineURN' during {@link CreateItemFromDescription}.
     * Default value is false.
     */
    Dependency_addStateMachineURN("Dependency.addStateMachineURN", false),
    /**
     * Add or not a new Item Property 'WorkflowURN' during {@link CreateItemFromDescription}.
     * Default value is false.
     */
    Dependency_addWorkflowURN("Dependency.addWorkflowURN", false),
    /**
     * Enables or disables Dependency collection to contain the same Item many times. Default value
     * is true.
     */
    Dependency_checkMemberUniqueness("Dependency.checkMemberUniqueness", true),
    /**
     * Replace UUID with Activitz name while exporting {@link CompositeActivityDef}. Default value is "false"
     */
    Export_replaceActivitySlotDefUUIDWithName("Export.replaceActivitySlotDefUUIDWithName", false),
    /**
     * The name of the Server Item. default is 'localhost', although kernel code overrides that 
     * with InetAddress.getLocalHost().getHostName().
     */
    ItemServer_name("ItemServer.name", "localhost"),
    /**
     * Define the host for vert.x shall service using telnet. Default value is 'localhost'
     */
    ItemServer_Telnet_host("ItemServer.Telnet.host", "localhost"),
    /**
     * Define the port for vert.x shall service using telnet. Default value is 0, used to disable the service.
     */
    ItemServer_Telnet_port("ItemServer.Telnet.port", 0),
    /**
     * Specifies the address name of the vertx event bus. Default value is 'cristalise-items'
     */
    ItemVerticle_ebAddress("ItemVerticle.ebAddress", "cristalise-items"),
    /**
     * If the {@link ServiceException} thrown in ItemVerticle includes debug information or not.
     * Default value is 'true'
     */
    ItemVerticle_includeDebugInfo("ItemVerticle.includeDebugInfo", true),
    /**
     * The number of deployed ItemVerticle instances. Default value is 8
     */
    ItemVerticle_instances("ItemVerticle.instances", 8),
    /**
     * Specifies if the ItemVerticle is a worker verticle or not. Default value is 'true'
     */
    ItemVerticle_isWorker("ItemVerticle.isWorker", true),
    /**
     * The number of seconds before a request to an Item times out. Default value is 10
     */
    ItemVerticle_requestTimeoutSeconds("ItemVerticle.requestTimeoutSeconds", 10),
    /**
     * Defines the name of the field in the Outcome containing the name of the Agent. It is used in
     * the Sign predefined step. Default is 'AgentName'
     */
    Lifecycle_Sign_agentNameField("Lifecycle.Sign.agentNameField", "AgentName"),
    /**
     * Defines the name of the field in the Outcome containing the password of the Agent. It is used
     * in the Sign predefined step. Default is 'Password'
     */
    Lifecycle_Sign_passwordField("Lifecycle.Sign.passwordField", "Password"),
    /**
     * Defines the name of the field in the Outcome containing the password of the Agent. It is used
     * in the Sign predefined step. Default is 'Password'
     */
    Lifecycle_Sign_signedFlagField("Lifecycle.Sign.signedFlagField", "ElectronicallySigned"),
    /**
     * LocalChangeVerticle shall publish instead of send vert.x events the the local addresses. Default value is 'true'.
     */
    LocalChangeVerticle_publishLocalMessage("LocalChangeVerticle.publishLocalMessage", true),
    /**
     * Enforce LocalObjectLoader to use original (but slow) ItemProperty based search in lookup tree 
     * instead of subtree based search which is faster. Default value is 'false'.
     */
    LocalObjectLoader_lookupUseProperties("LocalObjectLoader.lookupUseProperties", false),
    /**
     * Create Role even if it is not fully specified in the ImportAgent. Default value is false.
     */
    Module_ImportAgent_enableRoleCreation("Module.ImportAgent.enableRoleCreation", false),
    /**
     * Forces Bootstrap to overwrite existing resources, even if it was updated by some else, 
     * i.e. using dynamic editing through UI. Default value is 'false'.
     */
    Module_reset("Module.reset", false),
    /**
     * Generate error if the resource Item is referenced without version number, otherwise use
     * version 0. Default value is false.
     */
    Module_Versioning_strict("Module.Versioning.strict", false),
    /**
     * Enable to use DOM instead of string during {@link Outcome#validate()}. Default value is 'true'.
     * It was added to investigate strange Apache Xerces xml corruption issue.
     */
    Outcome_Validation_useDOM("Outcome.Validation.useDOM", true),
    /**
     * Use last Outcome instance instead of OutcomeInitiator. Default value is false.
     */
    OutcomeInit_jobUseViewpoint("OutcomeInit.jobUseViewpoint", false),
    /**
     * Comma separated list of modules namespaces, that use file names with version. Default value is empty string.
     */
    Resource_moduleUseFileNameWithVersion("Resource.moduleUseFileNameWithVersion", ""),
    /**
     * Enables to use the deprecated module resource format when exporting the
     * {@link DescriptionObject}. Default value is 'false'.
     */
    Resource_useOldImportFormat("Resource.useOldImportFormat", false),
    /**
     * Throws an exception when the RoutingScript does not return of the Script is not a String
     * type. Default value is 'false', which means the Object.toString() value is returned.
     */
    RoutingScript_enforceStringReturnValue("RoutingScript.enforceStringReturnValue", false),
    /**
     * Configuration of the shiro.ini file, normally it is in the config directory. No default value.
     */
    Shiro_iniFile("Shiro.iniFile"),
    /**
     * Defines the default StateMachine for CompositeActivity. Default value is 'CompositeActivity'.
     */
    StateMachine_Composite_default("StateMachine.Elementary.default", "CompositeActivity"),
    /**
     * Defines the default StateMachine for ElementaryActivities. Default value is 'Default'.
     */
    StateMachine_Elementary_default("StateMachine.Elementary.default", "Default"),
    /**
     * Use error handling (error transition) defined in StateMachine. Default value is false.
     */
    StateMachine_enableErrorHandling("StateMachine.enableErrorHandling", false),
    /**
     * Defines the default StateMachine for PredefinedSteps. Default value is 'PredefinedStep'.
     */
    StateMachine_Predefined_default("StateMachine.Elementary.default", "PredefinedStep"),
    /**
     * Specifies the comma separated list of field names in SystemProperties and Outcomes for which 
     * the value should be redacted when printed or stored for security reasons. Default is 'password,pwd'.
     */
    SystemProperties_keywordsToRedact("SystemProperties.keywordsToRedact", "password,pwd"),
    /**
     * Host of the machine to connect through vert.x tcp-ip bridge, No default value
     */
    TcpBridge_host("TcpBridge.host"),
    /**
     * Port of the machine to connect through vert.x tcp-ip bridge, Default value is 7000
     */
    TcpBridge_port("TcpBridge.port", 7000),
    /**
     * Specifies the root directory of the XML file based Storage- No default value.
     */
    XMLStorage_root("XMLStorage.root");

    @Getter
    private final Object defaultValue;
    @Getter
    private final String systemPropertyName;

    private SystemProperties(String name) {
        this(name, null);
    }

    private SystemProperties(String name, Object value) {
        systemPropertyName = name;
        defaultValue = value;
    }

    @Override
    public String toString() {
        return systemPropertyName;
    }
}
