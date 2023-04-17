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

import org.apache.shiro.authz.permission.WildcardPermission;
import org.cristalise.kernel.entity.imports.ImportAgent;
import org.cristalise.kernel.graph.model.BuiltInVertexProperties;
import org.cristalise.kernel.lifecycle.CompositeActivityDef;
import org.cristalise.kernel.lifecycle.instance.predefined.BulkErase;
import org.cristalise.kernel.lifecycle.instance.predefined.CreateItemFromDescription;
import org.cristalise.kernel.lifecycle.instance.predefined.server.BulkImport;
import org.cristalise.kernel.lifecycle.routingHelpers.DataHelper;
import org.cristalise.kernel.lookup.Lookup;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.OutcomeInitiator;
import org.cristalise.kernel.process.Bootstrap;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.utils.DescriptionObject;
import org.cristalise.kernel.utils.SystemPropertyOperations;
import org.cristalise.storage.XMLClusterStorage;

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
 * @see #BulkErase_force
 * @see #BulkErase_limit
 * @see #BulkImport_fileExtension
 * @see #BulkImport_rootDirectory
 * @see #BulkImport_useDirectories
 * @see #ClusterStorage
 * @see #ClusterStorage_cacheSpec
 * @see #DataHelper
 * @see #Dependency_addStateMachineURN
 * @see #Dependency_addWorkflowURN
 * @see #Dependency_checkMemberUniqueness
 * @see #Erase_force
 * @see #Export_replaceActivitySlotDefUUIDWithName
 * @see #Gateway_clusteredVertx
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
 * @see #Lookup
 * @see #Module_ImportAgent_enableRoleCreation
 * @see #Module_reset
 * @see #Module_$Namespace_reset
 * @see #Module_Versioning_strict
 * @see #Outcome_Validation_useDOM
 * @see #OutcomeInit_$name
 * @see #OutcomeInit_jobUseViewpoint
 * @see #Resource_moduleUseFileNameWithVersion
 * @see #Resource_useOldImportFormat
 * @see #ResourceImportHandler_$typeCode
 * @see #RoutingScript_enforceStringReturnValue
 * @see #Script_EngineOverride_$lang
 * @see #Shiro_iniFile
 * @see #StateMachine_Composite_default
 * @see #StateMachine_Elementary_default
 * @see #StateMachine_enableErrorHandling
 * @see #StateMachine_Predefined_default
 * @see #SystemProperties_keywordsToRedact
 * @see #TcpBridge_host
 * @see #TcpBridge_port
 * @see #$UserCodeRole_agent
 * @see #$UserCodeRole_password
 * @see #$UserCodeRole_permissions
 * @see #UserCode_roleOverride
 * @see #$UserCodeRole_StateMachine_completeTransition
 * @see #$UserCodeRole_StateMachine_errorTransition
 * @see #$UserCodeRole_StateMachine_startTransition
 * @see #$UserCodeRole_StateMachine_bootfile
 * @see #$UserCodeRole_StateMachine_name
 * @see #$UserCodeRole_StateMachine_namespace
 * @see #$UserCodeRole_StateMachine_version
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
     * DEPRECATED - Shiro provides better mechanism. Specifies the Authenticator implementation to be used. 
     * The default value is 'Shiro', which will use the Shiro integration with authentication realms, 
     * or it could be a java class implementing the deprecated {@link Authenticator} interface.
     */
    Authenticator("Authenticator", "Shiro"),
    /**
     *  If true continue Erase even if an error. Default value is false - UNIMPLEMENTED
     */
    BulkErase_force("BulkErase.force", false),
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
     * Ordered list of implementation of the {@link ClusterStorage} interface that Cristal will use for storage 
     * of Item local objects. Precedence is left to right. If package name is not supplied, 
     * org.cristalise.storage is implied. No default value.
     */
    ClusterStorage("ClusterStorage"),
    /**
     * Specifies the Google Guava cache behavior used in ClusterStorageManager. 
     * Default is value is 'expireAfterAccess = 600s, recordStats'
     */
    ClusterStorage_cacheSpec("ClusterStorage.cacheSpec", "expireAfterAccess = 600s, recordStats"),
    /**
     * Define the java classname that implements the {@link DataHelper} interface. No default value.
     * 
     * @apiNote $Name means that it will be replaced with name of the {@link DataHelper} when 
     * {@link #getString(Object...)} is used.
     */
    DataHelper_$name("DataHelper.%s"),
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
     *  If true continue Erase even if an error during deleting BiDirectional references. Default value is false.
     */
    Erase_force("Erase.force", false),
    /**
     * Replace UUID with Activity name while exporting {@link CompositeActivityDef}. Default value is 'false'
     */
    Export_replaceActivitySlotDefUUIDWithName("Export.replaceActivitySlotDefUUIDWithName", false),
    /**
     * Initialise the clustered version of vertx. Default value is 'true'.
     */
    Gateway_clusteredVertx("Gateway.clusteredVertx", true),
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
     * Define the java classname that implements the {@link Lookup} interface. No default value.
     */
    Lookup("Lookup"),
    /**
     * Create Role(s) defined in {@link ImportAgent} even if it is not fully specified. Default value is false.
     */
    Module_ImportAgent_enableRoleCreation("Module.ImportAgent.enableRoleCreation", false),
    /**
     * Forces {@link Bootstrap} to overwrite existing resources imported from all modules. 
     * If true the resource is updated to the version stored in the jar to overwrite changes by someone 
     * other than {@link Bootstrap} (i.e. using dynamic editing through UI. Default value is 'false'.
     */
    Module_reset("Module.reset", false),
    /**
     * Forces {@link Bootstrap} to overwrite existing resources imported from the given module (see apiNote of $Namespace). 
     * If true the resource is updated to the version stored in the jar to overwrite changes by someone 
     * other than {@link Bootstrap} (i.e. using dynamic editing through UI. Default value is 'false'.
     * 
     * @apiNote $Namespace means that it will be replaced with name of the Namespace 
     * when {@link #getBoolean(Object...)} is used.
     */
    Module_$Namespace_reset("Module.%s.reset"),
    /**
     * If true generates error if the resource Item is referenced without version number, otherwise use
     * version 0. Default value is false.
     */
    Module_Versioning_strict("Module.Versioning.strict", false),
    /**
     * Enable to use DOM instead of string during {@link Outcome#validate()}. Default value is 'true'.
     * It was added to investigate strange Apache Xerces xml corruption issue.
     */
    Outcome_Validation_useDOM("Outcome.Validation.useDOM", true),
    /**
     * Specifies an {@link OutcomeInitiator} implementation to use to create new empty Outcomes. 
     * Will be invoked from Job.getOutcome() for Activities with an 'OutcomeInit' property set to the given name.
     * Default is null.
     * 
     * @apiNote $Name means that it will be replaced with name of the OutcomeInitiator when 
     * {@link #getString(Object...)} is used.
     */
    OutcomeInit_$name("OutcomeInit.%s"),
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
     * Specifies a custom ResourceImportHandler implementation, allowing modules to define their 
     * own resource types, or override the import of the core ones. The type code can be any string, 
     * but by convention a short upper-case string is used. The core types are EA (Elementary Activity), 
     * CA (Composite Activity), OD (Outcome Description - Schema), SC (Script) and SM (State Machine)
     */
    ResourceImportHandler_$typeCode("ResourceImportHandler.%s"),
    /**
     * Throws an exception when the RoutingScript does not return of the Script is not a String
     * type. Default value is 'false', which means the Object.toString() value is returned.
     */
    RoutingScript_enforceStringReturnValue("RoutingScript.enforceStringReturnValue", false),
    /**
     * Override the javax.script engine for the given scripting language. Used to override Javascript in Java8+ with Rhino
     */
    Script_EngineOverride_$lang("Script.EngineOverride.%s"),
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
     * Defines the default role to be used for UserCode. It also used as a prefix for every configuration property
     * eg: UserCode.StateMachine.startTransition. Default value is 'UserCode'.
     */
    UserCode_roleOverride("UserCode.roleOverride", "UserCode"),
    /**
     * Specifies the Agent name associated with the UserCode role. No default value.
     * 
     * @apiNote $UserCodeRole means that it will be replaced with Role name of the associated with the UserCode
     * when {@link #getString(Object...)} is used.
     */
    $UserCodeRole_agent("%s.agent"),
    /**
     * Specifies the password of the Agent associated with the UserCode role. Default value is 'uc'.
     * 
     * @apiNote $UserCodeRole means that it will be replaced with Role name of the associated with the UserCode
     * when {@link #getString(Object...)} is used.
     */
    $UserCodeRole_password("%s.password", "uc"),
    /**
     * Specifies the permissions, i.e. comma separated {@link WildcardPermission} strings, of the Agent 
     * associated with the UserCode role. Default value is 'uc'.
     * 
     * @apiNote $UserCodeRole means that it will be replaced with Role name of the associated with the UserCode
     * when {@link #getString(Object...)} is used.
     */
    $UserCodeRole_permissions("%s.permissions"),
    /**
     * Override the default mapping for Start transition of UserCode.
     * It is always prefixed like this: eg: UserCode.StateMachine.startTransition
     * 
     * @apiNote $UserCodeRole means that it will be replaced with Role name of the associated with the UserCode
     * when {@link #getString(Object...)} is used.
     */
    $UserCodeRole_StateMachine_startTransition("%s.StateMachine.startTransition", "Start"),
    /**
     * Override the default mapping for Complete transition of UserCode.
     * It is always prefixed like this: eg: UserCode.StateMachine.completeTransition
     * 
     * @apiNote $UserCodeRole means that it will be replaced with Role name of the associated with the UserCode
     * when {@link #getString(Object...)} is used.
     */
    $UserCodeRole_StateMachine_completeTransition("%s.StateMachine.completeTransition", "Complete"),
    /**
     * Override the default mapping for Error transition of UserCode.
     * It is always prefixed like this: eg: UserCode.StateMachine.errorTransition
     * 
     * @apiNote $UserCodeRole means that it will be replaced with Role name of the associated with the UserCode
     * when {@link #getString(Object...)} is used.
     */
    $UserCodeRole_StateMachine_errorTransition("%s.StateMachine.errorTransition", "Suspend"),
    /**
     * Specifies the StateMachine Name required for the UserCode implementation. Default value is 'Default'
     * because standard implementation is based in the built-in StateMachine.
     * 
     * @apiNote $UserCodeRole means that it will be replaced with Role name of the associated with the UserCode
     * when {@link #getString(Object...)} is used.
     */
    $UserCodeRole_StateMachine_name("%s.StateMachine.name", "Default"),
    /**
     * Specifies the StateMachine version required for the UserCode implementation. Default value is '0'.
     * 
     * @apiNote $UserCodeRole means that it will be replaced with Role name of the associated with the UserCode
     * when {@link #getString(Object...)} is used.
     */
    $UserCodeRole_StateMachine_version("%s.StateMachine.version"),
    /**
     * Specifies the namespace (i.e. the module) from which the StateMachine required for the UserCode implementation
     * can be loaded. No default value.
     * 
     * @apiNote $UserCodeRole means that it will be replaced with Role name of the associated with 
     * the UserCode when {@link #getString(Object...)} is used.
     * 
     * @apiNote It was added to enable testing, when LocalObjectLoader is not fully initialised, 
     * so the StateMachine is loaded from the jar directly.
     */
    $UserCodeRole_StateMachine_namespace("%s.StateMachine.namespace"),
    /**
     * Specifies the file path to of the StateMachine XML file used in the jar. This will be used to find 
     * the StateMachine required for the UserCode implementation can be loaded. No default value.
     * 
     * @apiNote $UserCodeRole means that it will be replaced with Role name of the associated with the UserCode
     * when {@link #getString(Object...)} is used.
     * 
     * @apiNote It was added to enable testing, when LocalObjectLoader is not fully initialised, 
     * so the StateMachine is loaded from the jar directly.
     */
    $UserCodeRole_StateMachine_bootfile("%s.StateMachine.bootfile"),
    /**
     * If using {@link XMLClusterStorage}, this defined the root directory of XML file storage No default value.
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
