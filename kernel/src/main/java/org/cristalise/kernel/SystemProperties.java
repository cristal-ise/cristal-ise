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
 */
public enum SystemProperties {

    /**
     * Enables OutcomeValidation during database transaction (aka server-side validation). Default
     * value is 'false', because Outcomes are validated in client code (e.g. during restapi call).
     * Such a validation can be enabled using {@link BuiltInVertexProperties#VALIDATE_OUTCOME}
     * Activity property as well.
     */
    Activity_validateOutcome("Activity.validateOutcome", false),
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
     * Create Role even if it is not fully specified in the ImportAgent. Default value is false.
     */
    Module_ImportAgent_enableRoleCreation("Module.ImportAgent.enableRoleCreation", false),
    /**
     * Generate error if the resource Item is referenced without version number, otherwise use
     * version 0. Default value is false.
     */
    Module_Versioning_strict("Module.Versioning.strict", false),
    /**
     * Use last Outcome instance instead of OutcomeInitiator. Default value is false.
     */
    OutcomeInit_jobUseViewpoint("OutcomeInit.jobUseViewpoint", false),
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
     * Host of the machine to connect through vert.x tcp-ip bridge, No default value
     */
    TcpBridge_host("TcpBridge.host"),
    /**
     * Port of the machine to connect through vert.x tcp-ip bridge, Default value is 7000
     */
    TcpBridge_port("TcpBridge.port", 7000),
    /**
     * Enable to use DOM instead of string during {@link Outcome#validate()}. Default value is 'true'.
     * It was added to investigate strange Apache Xerces xml corruption issue.
     */
    Outcome_Validation_useDOM("Outcome.Validation.useDOM", true),
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
     * The name of the Server Item. default is 'localhost', although kernel code overrides that 
     * with InetAddress.getLocalHost().getHostName().
     */
    ItemServer_name("ItemServer.name", "localhost"),
    /**
     * Forces Bootstrap to overwrite existing resources, even if it was updated by some else, 
     * i.e. using dynamic editing through UI. Default value is 'false'.
     */
    Module_reset("Module.reset", false),
    /**
     * Comma separated list of modules namespaces, that use file names with version. Default value is empty string.
     */
    Resource_moduleUseFileNameWithVersion("Resource.moduleUseFileNameWithVersion", ""),
    /**
     * Define the host for vert.x shall service using telnet. Default value is 'localhost'
     */
    ItemServer_Telnet_host("ItemServer.Telnet.host", "localhost"),
    /**
     * Define the port for vert.x shall service using telnet. Default value is 0, used to disable the service.
     */
    ItemServer_Telnet_port("ItemServer.Telnet.port", 0),
    /**
     * LocalChangeVerticle shall publish instead of send vert.x events the the local addresses. Default value is 'true'.
     */
    LocalChangeVerticle_publishLocalMessage("LocalChangeVerticle.publishLocalMessage", true),
    /**
     * Specify the Authenticator implementation to be used. The default value is 'Shiro', which will use the Shiro
     * integration with authentication realms, or it could be a java class implementing the {@link Authenticator} interface.
     */
    Authenticator("Authenticator", "Shiro"),
    /**
     * Configuration of the shiro.ini file, normally it is in the config directory. No default value.
     */
    Shiro_iniFile("Shiro.iniFile"),
    /**
     * Enforce LocalObjectLoader to use original (but slow) ItemProperty based search in lookup tree 
     * instead of subtree based search which is faster. Default value is 'false'.
     */
    LocalObjectLoader_lookupUseProperties("LocalObjectLoader.lookupUseProperties", false),
    /**
     * Specifies the comma separated list of field names in SystemProperties and Outcomes for which 
     * the value should be redacted when printed or stored for security reasons. Default is 'password,pwd'.
     */
    SystemProperties_keywordsToRedact("SystemProperties.keywordsToRedact", "password,pwd"),
    /**
     * Specifies the root directory of the XML file based Storage- No default value.
     */
    XMLStorage_root("XMLStorage.root");

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

    public Object get() {
        if (this.defaultValue == null) {
            return Gateway.getProperties().get(this.systemPropertyName);
        }
        else {
            Object actValue = Gateway.getProperties().get(this.systemPropertyName);

            if (actValue == null) return this.defaultValue;
            else                  return Gateway.getProperties().get(this.systemPropertyName);
        }
    }

    public boolean getBoolean() {
        if (this.defaultValue == null) {
            return Gateway.getProperties().getBoolean(this.systemPropertyName);
        }
        else {
            return Gateway.getProperties().getBoolean(this.systemPropertyName, (boolean) this.defaultValue);
        }
    }

    public Integer getInteger() {
        if (this.defaultValue == null) {
            return Gateway.getProperties().getInteger(this.systemPropertyName);
        }
        else {
            return Gateway.getProperties().getInteger(this.systemPropertyName, (Integer) this.defaultValue);
        }
    }

    public String getString() {
        return getString(null);
    }

    public String getString(String defaultOverwrite) {
        Object actualDefaultValue = defaultOverwrite == null ? this.defaultValue : defaultOverwrite;

        if (actualDefaultValue == null) {
            return Gateway.getProperties().getString(this.systemPropertyName);
        }
        else {
            return Gateway.getProperties().getString(this.systemPropertyName, (String) actualDefaultValue);
        }
    }

    public Object set(Object value) {
        return Gateway.getProperties().put(this.systemPropertyName, value);
    }

    @Override
    public String toString() {
        return systemPropertyName;
    }
}
