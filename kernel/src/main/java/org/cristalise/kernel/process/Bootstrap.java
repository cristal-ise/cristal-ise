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
package org.cristalise.kernel.process;

import static org.cristalise.kernel.property.BuiltInItemProperties.KERNEL_VERSION;
import static org.cristalise.kernel.property.BuiltInItemProperties.NAME;
import static org.cristalise.kernel.property.BuiltInItemProperties.TYPE;
import static org.cristalise.kernel.security.BuiltInAuthc.ADMIN_ROLE;
import static org.cristalise.kernel.security.BuiltInAuthc.SYSTEM_AGENT;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.lifecycle.CompositeActivityDef;
import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep;
import org.cristalise.kernel.lifecycle.instance.predefined.UpdateImportReport;
import org.cristalise.kernel.lifecycle.instance.predefined.server.ServerPredefinedStepContainer;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.LookupManager;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.resource.BuiltInResources;
import org.cristalise.kernel.process.resource.ResourceImportHandler;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.scripting.ScriptConsole;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.LocalObjectLoader;

import lombok.extern.slf4j.Slf4j;


/**
 * Bootstrap loads all Items defined in the kernel resource XMLs and the module XML
 */
@Slf4j
public class Bootstrap {

    static DomainPath thisServerPath;
    static HashMap<String, AgentProxy> systemAgents = new HashMap<String, AgentProxy>();
    public static boolean shutdown = false;
    
    /**
     * Initialise Bootstrap
     * 
     * @throws Exception in case of any error
     */
    static void init() throws Exception {
        // check for system agents
        checkAdminAgents();

        // create the server's mother item
        ItemPath serverItem = createServerItem();

        // store system properties in server item
        storeSystemProperties(serverItem);
    }

    /**
     * Run everything without timing-out the service wrapper
     */
    static void run() throws Exception {
        init();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.currentThread().setName("Bootstrapper");

                    log.info("run() - Bootstrapper started");

                    ClassLoader wClassLoader = Bootstrap.class.getClassLoader();
                    log.info("run() setContextClassLoader=[{}]", wClassLoader);
                    Thread.currentThread().setContextClassLoader(wClassLoader);

                    // make sure all of the boot items are up-to-date
                    if (!shutdown) {
                        log.info("run() - Verifying kernel boot data items");
                        verifyBootDataItems();
                    }

                    // verify the server item's wf
                    if (!shutdown) {
                        log.info("run() - Initialising Server Item Workflow");
                        initServerItemWf();
                    }

                    if (!shutdown) {
                        Gateway.getModuleManager().setUser(systemAgents.get(SYSTEM_AGENT.getName()));
                        Gateway.getModuleManager().registerModules();
                    }

                    if (!shutdown) {
                        log.info("run() - Bootstrapper complete");
                        Gateway.getModuleManager().runScripts("initialized");

                        if (Gateway.getLookupManager() != null) Gateway.getLookupManager().postBoostrap();
                        Gateway.getStorage().postBoostrap();
                    }
                }
                catch (Throwable e) {
                    log.error("Exception performing bootstrap. Check that everything is OK.", e);
                    AbstractMain.shutdown(1);
                }
            }
        }).start();
    }

    /**
     * Set flag for the thread to abort gracefully
     */
    public static void abort() {
        shutdown = true;
    }

    /**
     * Checks all kernel descriptions stored in resources and create or update them if they were changed
     */
    public static void verifyBootDataItems() throws Exception {
        String bootItems;
        log.info("verifyBootDataItems() - Start checking kernel descriptions ...");

        bootItems = FileStringUtility.url2String(Gateway.getResource().getKernelResourceURL("boot/allbootitems.txt"));

        verifyBootDataItems(bootItems, null, true);

        log.info("verifyBootDataItems() - DONE.");
    }

    /**
     *
     * @param bootList
     * @param ns
     * @param reset
     * @throws InvalidItemPathException
     */
    private static void verifyBootDataItems(String bootList, String ns, boolean reset) throws Exception {
        StringTokenizer str = new StringTokenizer(bootList, "\n\r");

        List<String> kernelChanges = new ArrayList<String>();

        while (str.hasMoreTokens() && !shutdown) {
            String thisItem = str.nextToken();
            String[] idFilename = thisItem.split(",");
            String id = idFilename[0], filename = idFilename[1];
            ItemPath itemPath = new ItemPath(id);
            String[] fileParts = filename.split("/");
            String itemType = fileParts[0], itemName = fileParts[1];

            try {
                String location = "boot/"+filename+(itemType.equals("OD")?".xsd":".xml");
                ResourceImportHandler importHandler = Gateway.getResourceImportHandler(BuiltInResources.getValue(itemType));
                importHandler.importResource(ns, itemName, 0, itemPath, location, reset);

                kernelChanges.add(importHandler.getResourceChangeDetails());
            }
            catch (Exception e) {
                log.error("Error importing bootstrap items. Unsafe to continue.", e);
                AbstractMain.shutdown(1);
            }
        }

        StringBuffer moduleChangesXML = new StringBuffer("<ModuleChanges>\n");
        for (String oneChange: kernelChanges) moduleChangesXML.append(oneChange).append("\n");
        moduleChangesXML.append("</ModuleChanges>");

        new UpdateImportReport().request((AgentPath)SYSTEM_AGENT.getPath(), thisServerPath.getItemPath(), moduleChangesXML.toString());
    }

    /**
     * Checks for the existence of a agents and creates it if needed so it can be used
     *
     * @param name the name of the agent
     * @param pass the password of the agent
     * @param rolePath the role of the agent
     * @param uuid the UUID os the agent
     * @return the Proxy representing the Agent
     * @throws Exception any exception found
     */
    private static AgentProxy checkAgent(String name, String pass, RolePath rolePath, String uuid) throws Exception {
        log.info("checkAgent() - Checking for existence of '"+name+"' agent.");
        LookupManager lookup = Gateway.getLookupManager();

        try {
            AgentProxy agentProxy = Gateway.getProxyManager().getAgentProxy(lookup.getAgentPath(name));
            systemAgents.put(name, agentProxy);
            log.info("checkAgent() - Agent '"+name+"' found.");
            return agentProxy;
        }
        catch (ObjectNotFoundException ex) { }

        log.info("checkAgent() - Agent '"+name+"' not found. Creating.");

        try {
            AgentPath agentPath = new AgentPath(new ItemPath(uuid), name);

            Gateway.getCorbaServer().createAgent(agentPath);
            lookup.add(agentPath);

            if (StringUtils.isNotBlank(pass)) lookup.setAgentPassword(agentPath, pass);

            // assign role
            log.info("checkAgent() - Assigning role '"+rolePath.getName()+"'");
            Gateway.getLookupManager().addRole(agentPath, rolePath);
            Gateway.getStorage().put(agentPath, new Property(NAME, name, true), null);
            Gateway.getStorage().put(agentPath, new Property(TYPE, "Agent", false), null);
            AgentProxy agentProxy = Gateway.getProxyManager().getAgentProxy(agentPath);
            //TODO: properly init agent here with wf, props and colls -> use CreatItemFromDescription
            systemAgents.put(name, agentProxy);
            return agentProxy;
        }
        catch (Exception ex) {
            log.error("Unable to create '"+name+"' Agent.", ex);
            throw ex;
        }
    }

    /**
     * 
     * @throws Exception
     */
    public static void checkAdminAgents() throws Exception {
        RolePath rootRole = new RolePath();
        if (!rootRole.exists()) Gateway.getLookupManager().createRole(rootRole);

        // check for 'Admin' role
        RolePath adminRole = new RolePath(rootRole, ADMIN_ROLE.getName(), false);
        adminRole.getPermissions().add("*");
        if (!adminRole.exists()) Gateway.getLookupManager().createRole(adminRole);

        // check for 'system' Agent
        AgentProxy system = checkAgent(SYSTEM_AGENT.getName(), null, adminRole, new UUID(0, 1).toString());
        ScriptConsole.setUser(system);

        String ucRole = Gateway.getProperties().getString("UserCode.roleOverride", UserCodeProcess.DEFAULT_ROLE);

        // check for local usercode user & role
        RolePath usercodeRole = new RolePath(rootRole, ucRole, true);
        if (!usercodeRole.exists()) Gateway.getLookupManager().createRole(usercodeRole);
        checkAgent(
                Gateway.getProperties().getString(ucRole + ".agent",     InetAddress.getLocalHost().getHostName()),
                Gateway.getProperties().getString(ucRole + ".password", "uc"),
                usercodeRole,
                UUID.randomUUID().toString());
    }

    private static ItemPath createServerItem() throws Exception {
        LookupManager lookupManager = Gateway.getLookupManager();
        String serverName = Gateway.getProperties().getString("ItemServer.name", InetAddress.getLocalHost().getHostName());
        thisServerPath = new DomainPath("/servers/"+serverName);
        ItemPath serverItem;
        try {
            serverItem = thisServerPath.getItemPath();
        }
        catch (ObjectNotFoundException ex) {
            log.info("Creating server item "+thisServerPath);
            serverItem = new ItemPath();
            Gateway.getCorbaServer().createItem(serverItem);
            lookupManager.add(serverItem);
            thisServerPath.setItemPath(serverItem);
            lookupManager.add(thisServerPath);
        }

        int proxyPort = Gateway.getProperties().getInt("ItemServer.Proxy.port", 1553);

        Gateway.getStorage().put(serverItem, new Property(NAME,            serverName,                              false), null);
        Gateway.getStorage().put(serverItem, new Property(TYPE,            "Server",                                false), null);
        Gateway.getStorage().put(serverItem, new Property(KERNEL_VERSION,  Gateway.getKernelVersion(),              true),  null);
        Gateway.getStorage().put(serverItem, new Property("ProxyPort",     String.valueOf(proxyPort),               false), null);
//        Gateway.getStorage().put(serverItem, new Property("ConsolePort",   String.valueOf(Logger.getConsolePort()), true),  null);

        Gateway.getProxyManager().connectToProxyServer(serverName, proxyPort);

        return serverItem;
    }

    private static void storeSystemProperties(ItemPath serverItem) throws Exception {
        Outcome newOutcome = Gateway.getProperties().convertToOutcome("ItemServer");
        PredefinedStep.storeOutcomeEventAndViews(serverItem, newOutcome);
    }

    public static void initServerItemWf() throws Exception {
        CompositeActivityDef serverWfCa = (CompositeActivityDef)LocalObjectLoader.getActDef("ServerItemWorkflow", 0);
        Workflow wf = new Workflow((CompositeActivity)serverWfCa.instantiate(), new ServerPredefinedStepContainer());
        wf.initialise(thisServerPath.getItemPath(), systemAgents.get(SYSTEM_AGENT.getName()).getPath(), null);
        Gateway.getStorage().put(thisServerPath.getItemPath(), wf, null);
    }
}
