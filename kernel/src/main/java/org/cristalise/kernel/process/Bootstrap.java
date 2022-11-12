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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.imports.ImportAgent;
import org.cristalise.kernel.entity.imports.ImportRole;
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
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.resource.BuiltInResources;
import org.cristalise.kernel.process.resource.ResourceImportHandler;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.scripting.ScriptConsole;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;

import lombok.extern.slf4j.Slf4j;


/**
 * Bootstrap loads all Items defined in the kernel resource XMLs and the module XML
 */
@Slf4j
public class Bootstrap {

    static DomainPath thisServerPath;
    public static boolean shutdown = false;
    
    /**
     * Initialise Bootstrap
     * 
     * @throws Exception in case of any error
     */
    static void init() throws Exception {
        TransactionKey transactionKey = new TransactionKey("Bootstrap-Init");
        Gateway.getStorage().begin(transactionKey);

        try {
            //start console
            Logger.initConsole("ItemServer");

            // check for system agents
            checkAdminAgents(transactionKey);

            // create the server's mother item
            ItemPath serverItem = createServerItem(transactionKey);

            // store system properties in server item
            storeSystemProperties(serverItem, transactionKey);

            Gateway.getStorage().commit(transactionKey);
        }
        catch (Exception e) {
            Gateway.getStorage().abort(transactionKey);
            throw e;
        }
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

                    if (!shutdown) {
                        Gateway.getModuleManager().setUser(Gateway.getProxyManager().getAgentProxy((AgentPath)SYSTEM_AGENT.getPath()));
                        Gateway.getModuleManager().registerModules();
                    }

                    if (!shutdown) {
                        log.info("run() - RegisterModules complete");

                        Gateway.getModuleManager().runScripts("initialized");

                        if (Gateway.getLookupManager() != null) Gateway.getLookupManager().postBoostrap();
                        Gateway.getStorage().postBoostrap();

                        log.info("run() - Bootstrapper complete");
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

        TransactionKey transactionKey = new TransactionKey("Bootstrap-VerifyBootDataItems");
        Gateway.getStorage().begin(transactionKey);

        try {
            verifyBootDataItems(bootItems, null, true, transactionKey);

            Gateway.getStorage().commit(transactionKey);
        }
        catch (Exception e) {
            Gateway.getStorage().abort(transactionKey);
            throw e;
        }

        log.info("verifyBootDataItems() - DONE.");
    }

    /**
     *
     * @param bootList
     * @param ns
     * @param reset
     * @throws InvalidItemPathException
     */
    private static void verifyBootDataItems(String bootList, String ns, boolean reset, TransactionKey transactionKey) throws Exception {
        StringTokenizer str = new StringTokenizer(bootList, "\n\r");

        List<String> kernelChanges = new ArrayList<String>();

        while (str.hasMoreTokens() && !shutdown) {
            String thisItem = str.nextToken();
            String[] idFilename = thisItem.split(",");
            String id = idFilename[0], filename = idFilename[1];
            ItemPath itemPath = new ItemPath(id);
            String[] fileParts = filename.split("/");
            String itemType = fileParts[0], itemName = fileParts[1];

            String location = "boot/"+filename+(itemType.equals("OD")?".xsd":".xml");
            ResourceImportHandler importHandler = Gateway.getResourceImportHandler(BuiltInResources.getValue(itemType));
            importHandler.importResource(ns, itemName, 0, itemPath, location, reset, transactionKey);

            kernelChanges.add(importHandler.getResourceChangeDetails());
        }

        StringBuffer moduleChangesXML = new StringBuffer("<ModuleChanges>\n");
        moduleChangesXML.append("<ModuleName>kernel</ModuleName>");
        moduleChangesXML.append("<ModuleVersion>0</ModuleVersion>");
        for (String oneChange: kernelChanges) moduleChangesXML.append(oneChange).append("\n");
        moduleChangesXML.append("</ModuleChanges>");

        if (StringUtils.isNotBlank(moduleChangesXML)) {
            new UpdateImportReport().request((AgentPath)SYSTEM_AGENT.getPath(transactionKey), thisServerPath.getItemPath(), moduleChangesXML.toString(), transactionKey);
        }
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
    private static AgentProxy checkAgent(String name, String pass, RolePath rolePath, String uuid, TransactionKey transactionKey) throws Exception {
        log.info("checkAgent() - Checking for existence of '"+name+"' agent.");
        LookupManager lookup = Gateway.getLookupManager();

        try {
            AgentProxy agentProxy = Gateway.getProxyManager().getAgentProxy(lookup.getAgentPath(name, transactionKey), transactionKey);
            log.info("checkAgent() - Agent '"+name+"' found.");
            return agentProxy;
        }
        catch (ObjectNotFoundException ex) { }

        log.info("checkAgent() - Agent '"+name+"' not found. Creating.");

        try {
            AgentPath agentPath = new AgentPath(new ItemPath(uuid), name);

            Gateway.getCorbaServer().createAgent(agentPath, transactionKey);
            lookup.add(agentPath, transactionKey);

            if (StringUtils.isNotBlank(pass)) lookup.setAgentPassword(agentPath, pass, false, transactionKey);

            // assign role
            log.info("checkAgent() - Assigning role '"+rolePath.getName()+"'");
            Gateway.getLookupManager().addRole(agentPath, rolePath, transactionKey);
            Gateway.getStorage().put(agentPath, new Property(NAME, name, true), transactionKey);
            Gateway.getStorage().put(agentPath, new Property(TYPE, "Agent", false), transactionKey);
            AgentProxy agentProxy = Gateway.getProxyManager().getAgentProxy(agentPath, transactionKey);
            //TODO: properly init agent here with wf, props and colls -> use CreatItemFromDescription
            return agentProxy;
        }
        catch (Exception ex) {
            log.error("Unable to create '"+name+"' Agent.", ex);
            throw ex;
        }
    }

    /**
     * Checks for the existence of a agents and creates it if needed
     *
     * @param name the of the agent
     * @param pass of the agent
     * @param rolePath of the agent
     * @param uuid of the agent
     * @return the Proxy representing the Agent
     * @throws Exception any exception found
     */
    private static AgentPath checkOrCreateAgent(String name, String pass, ImportRole rolePath, UUID uuid, TransactionKey transactionKey) throws Exception {
      ImportAgent iAgent = new ImportAgent(name, pass);
      iAgent.addRole(rolePath);

      if (iAgent.exists(transactionKey)) {
        log.info("checkOrCreateAgent() - Agent '"+name+"' was found.");
      }
      else {
        log.info("checkOrCreateAgent() - Agent '"+name+"' NOT found. Creating.");

        AgentPath agentPath = new AgentPath(new ItemPath(uuid), name);
        iAgent.setItemPath(agentPath);
        iAgent.create(agentPath, false, transactionKey);
      }

      return iAgent.getAgentPath(transactionKey);
    }

    /**
     * 
     * @throws Exception
     */
    public static void checkAdminAgents(TransactionKey transactionKey) throws Exception {
      RolePath rootRole = new RolePath();
      if (!rootRole.exists(transactionKey)) Gateway.getLookupManager().createRole(rootRole, transactionKey);

      // check for 'Admin' role
      RolePath adminRole = new RolePath(rootRole, ADMIN_ROLE.getName(), false, Arrays.asList("*"));
      ImportRole importAdminRole = ImportRole.getImportRole(adminRole);

      if (adminRole.exists(transactionKey)) importAdminRole.update(null, transactionKey); // this will reset any changes done to the Admin role
      else                                  importAdminRole.create(null, false, transactionKey);

      // check for 'system' Agent
      checkOrCreateAgent(SYSTEM_AGENT.getName(), null, importAdminRole, new UUID(0, 1), transactionKey);

      // check for local usercode user & its role
      ImportRole importUCRole = UserCodeProcess.getImportRole();

      if (!importUCRole.exists(transactionKey)) importUCRole.create(null, false, transactionKey);

      String ucName = UserCodeProcess.getAgentName();
      String ucPwd = UserCodeProcess.getAgentPassword();

      checkOrCreateAgent(ucName, ucPwd, importUCRole, UUID.randomUUID(), transactionKey);
    }


  private static ItemPath createServerItem(TransactionKey transactionKey) throws Exception {
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
            Gateway.getCorbaServer().createItem(serverItem, transactionKey);
            lookupManager.add(serverItem, transactionKey);
            thisServerPath.setItemPath(serverItem);
            lookupManager.add(thisServerPath, transactionKey);
        }

        int proxyPort = Gateway.getProperties().getInt("ItemServer.Proxy.port", 1553);
        int consolePort = Logger.getConsolePort();

        Gateway.getStorage().put(serverItem, new Property(NAME,            serverName,                  false), transactionKey);
        Gateway.getStorage().put(serverItem, new Property(TYPE,            "Server",                    false), transactionKey);
        Gateway.getStorage().put(serverItem, new Property(KERNEL_VERSION,  Gateway.getKernelVersion(),  true),  transactionKey);
        Gateway.getStorage().put(serverItem, new Property("ProxyPort",     String.valueOf(proxyPort),   false), transactionKey);
        Gateway.getStorage().put(serverItem, new Property("ConsolePort",   String.valueOf(consolePort), true),  transactionKey);

        initServerItemWf(transactionKey);

        Gateway.getProxyManager().connectToProxyServer(serverName, proxyPort);

        return serverItem;
    }

    private static void storeSystemProperties(ItemPath serverItem, TransactionKey transactionKey) throws Exception {
        Outcome newOutcome = Gateway.getProperties().convertToOutcome("ItemServer");
        PredefinedStep.storeOutcomeEventAndViews(serverItem, newOutcome, transactionKey);
    }

    private static void initServerItemWf(TransactionKey transactionKey) throws Exception {
        CompositeActivityDef serverWfCa = (CompositeActivityDef)LocalObjectLoader.getCompActDef("ServerItemWorkflow", 0, transactionKey);
        Workflow wf = new Workflow((CompositeActivity)serverWfCa.instantiate(transactionKey), new ServerPredefinedStepContainer());
        wf.initialise(thisServerPath.getItemPath(), (AgentPath)SYSTEM_AGENT.getPath(transactionKey), transactionKey);
        Gateway.getStorage().put(thisServerPath.getItemPath(), wf, transactionKey);
    }
}
