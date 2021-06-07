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

import java.util.Properties;

import org.cristalise.kernel.process.resource.ResourceLoader;

import lombok.extern.slf4j.Slf4j;

/**
 * Base class for all servers i.e. c2k processes that serve Entities
 */
@Slf4j
public class StandardServer extends AbstractMain {
    protected static StandardServer server;

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
        Gateway.runBoostrap();

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
