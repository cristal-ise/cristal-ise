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

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import org.cristalise.kernel.process.resource.BadArgumentsException;
import org.cristalise.kernel.utils.FileStringUtility;

import lombok.extern.slf4j.Slf4j;

/**
 * Abstract base class for all CristalSpecific applications
 */
@Slf4j
abstract public class AbstractMain {

    public static boolean          isServer = false;
    private static ShutdownHandler shutdownHandler;

    public static final String MAIN_ARG_CONFIG         = "config";
    public static final String MAIN_ARG_CONNECT        = "connect";
    public static final String MAIN_ARG_SKIPBOOTSTRAP  = "skipBootstrap";

    /**
     * Reading and setting input paramaters
     *
     * Known arguments :
     * <ul>
     * <li>config</li> specifies the connect file
     * <li>connect</li> specifies the clc file
     * <li>LocalCentre</li> sets the local centre id
     * <li>resetIOR</li> simple argument with no value to trigger the reset ior feature
     * </ul>
     *
     * @param args arguments normally passed by the main()
     * @return the initialised Properties
     */
    public static Properties readC2KArgs( String[] args ) throws BadArgumentsException {
        Properties argProps = new Properties();

        int i = 0;
        while( i < args.length ) {
            if (args[i].startsWith("-") && args[i].length()>1) {
                String key = args[i].substring(1);

                if (argProps.containsKey(key)) throw new BadArgumentsException("Argument "+args[i]+" given twice");

                String value = "";

                if (args.length > i+1 && !args[i+1].startsWith("-")) value = args[++i];

                argProps.put(key, value);
                i++;
            }
            else
                throw new BadArgumentsException("Bad argument: "+args[i]);
        }

        // Dump params if log high enough
        if (log.isInfoEnabled()) {
            for (Enumeration<?> e = argProps.propertyNames(); e.hasMoreElements();) {
                String next = (String)e.nextElement();
                log.info("args param {}:{}", next, argProps.getProperty(next));
            }
        }

        String configPath = argProps.getProperty(MAIN_ARG_CONFIG);
        if (configPath == null) throw new BadArgumentsException("Config file not specified");
        
        String connectFile = argProps.getProperty(MAIN_ARG_CONNECT);
        if (connectFile == null) throw new BadArgumentsException("Connect file not specified");

        Properties c2kProps = readPropertyFiles(configPath, connectFile, argProps);

        log.info("readC2KArgs() DONE.");

        return c2kProps;
    }

    /**
     * Loads config & connect files into c2kprops, and merges them with existing properties 
     * 
     * @param configFile path to the config file
     * @param connectFile path to the connect (clc) file
     * @param argProps existing properties
     * @return fully initialized and merged list of properties
     * @throws BadArgumentsException
     */
    public static Properties readPropertyFiles(String configFile, String connectFile, Properties argProps) throws BadArgumentsException {
        log.info("readPropertyFiles() - config:{} connect:{}", configFile, connectFile);
        try {
            Properties c2kProps = FileStringUtility.loadConfigFile(configFile);

            if (argProps != null) c2kProps.putAll(argProps); // put args overlap config

            FileStringUtility.appendConfigFile(c2kProps, connectFile);

            if (!c2kProps.containsKey("LocalCentre")) {
                String connectFileName = new File(connectFile).getName();
                String centreId = connectFileName.substring(0, connectFileName.lastIndexOf(".clc"));
                c2kProps.setProperty("LocalCentre", centreId);
            }

            if (argProps != null) c2kProps.putAll(argProps); // put args override connect file too

            return c2kProps;
        }
        catch (IOException e) {
            log.error("readPropertyFiles() - Error reading config files", e);
            throw new BadArgumentsException(e);
        }
    }

    /**
     * Register application specific shutdown handler
     * 
     * @param handler the ShutdownHandler
     */
    public static void setShutdownHandler(ShutdownHandler handler) {
        shutdownHandler = handler;
    }

    /**
     * The actual shotdown each subclass should be calling to release resource properly
     * 
     * @param errCode unix error code to pass to the ShutdownHandler
     */
    public static void shutdown(int errCode) {
        Bootstrap.abort();

        if (shutdownHandler!= null) shutdownHandler.shutdown(errCode, isServer);

        try {
            Gateway.close();
        }
        catch (Exception ex) {
            log.error("", ex);
        }
        throw new ThreadDeath(); // if we get here, we get out
    }
}
