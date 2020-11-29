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

import java.util.Iterator;
import java.util.Properties;

import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.process.resource.ResourceLoader;

import lombok.extern.slf4j.Slf4j;

/**
 * Base class for all servers i.e. c2k processes that serve Entities
 */
@Slf4j
public class StandardServer extends AbstractMain {
    protected static StandardServer server;

    public static void resetItemIORs(DomainPath root, Object transactionKey) throws ObjectNotFoundException, ObjectCannotBeUpdated, CannotManageException {
        log.info("resetItemIORs() - root:"+root);

        Iterator<Path> pathes = Gateway.getLookup().getChildren(root, transactionKey);

        while (pathes.hasNext()) {
            DomainPath domain = (DomainPath) pathes.next();

            if (domain.isContext()) {
                resetItemIORs(domain, transactionKey);
            }
            else {
                log.info("resetItemIORs() - setting IOR for domain:" + domain + " item:" + domain.getItemPath());

                Gateway.getLookupManager().setIOR(
                        domain.getItemPath(),
                        Gateway.getORB().object_to_string(Gateway.getCorbaServer().getItemIOR(domain.getItemPath())),
                        transactionKey);
            }
        }
    }

    public static void resetAgentIORs(RolePath root, Object transactionKey) throws ObjectNotFoundException, ObjectCannotBeUpdated, CannotManageException {
        log.info("resetAgentIORs() - root:"+root);

        Iterator<Path> roles = Gateway.getLookup().getChildren(root, transactionKey);

        while (roles.hasNext()) {
            RolePath role = (RolePath) roles.next();

            resetAgentIORs(role, transactionKey);

            for (AgentPath agent :  Gateway.getLookup().getAgents(role, transactionKey)) {
                log.info("resetAgentIORs() - setting IOR for role:" + role + " agent:" + agent.getAgentName() + " " + agent.getItemPath());

                Gateway.getLookupManager().setIOR(
                        agent.getItemPath(),
                        Gateway.getORB().object_to_string(Gateway.getCorbaServer().getAgentIOR(agent)),
                        transactionKey);
            }
        }
    }

    /**
     * Initialise the server
     * 
     * @param props initiliased Properties
     * @param res the instantiated ResourceLoader
     * @throws Exception throw whatever happens
     */
    public static void standardInitialisation(Properties props, ResourceLoader res) throws Exception {
        isServer = true;

        // read args and init Gateway
        Gateway.init(props, res);

        // connect to LDAP as root
        Gateway.connect();

        //initialize the server objects
        Gateway.startServer();

        if (Gateway.getProperties().containsKey(AbstractMain.MAIN_ARG_RESETIOR)) {
            log.info("standard initialisation RESETTING IORs");

            Object transactionKey = new Object();
            Gateway.getStorage().begin(transactionKey);

            try {
                resetItemIORs(new DomainPath(""), transactionKey);
                resetAgentIORs(new RolePath(), transactionKey);

                Gateway.getStorage().commit(transactionKey);
            }
            catch (Exception e) {
                log.error("Error reseting IORs", e);
                Gateway.getStorage().abort(transactionKey);
            }

            AbstractMain.shutdown(0);
        }
        else {
            Gateway.runBoostrap();
        }

        log.info("standardInitialisation() - complete.");
    }

    /**
     * Initialise the server
     * 
     * @param args command line parameters
     * @throws Exception throw whatever happens
     */
    public static void standardInitialisation(String[] args) throws Exception {
        standardInitialisation(readC2KArgs(args), null);
    }

    /**
     * Main to launch a Standard Server process
     * 
     * @param args command line parameters
     * @throws Exception  throw whatever happens
     */
    public static void main(String[] args) throws Exception {

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                AbstractMain.shutdown(0);
            }
        });

        //initialise everything
        standardInitialisation( args );
    }
}
