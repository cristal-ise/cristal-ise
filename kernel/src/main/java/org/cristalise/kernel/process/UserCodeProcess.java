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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import org.cristalise.kernel.entity.imports.ImportAgent;
import org.cristalise.kernel.entity.imports.ImportRole;
import org.cristalise.kernel.lookup.RolePath;

import io.vertx.core.DeploymentOptions;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserCodeProcess extends StandardClient {

    /**
     * Defines the default role (value:{@value}). It also used as a prefix for every configuration property
     * eg: UserCode.StateMachine.startTransition
     */
    public static final String DEFAULT_ROLE = "UserCode";
    /**
     * Defines the default password (value:{@value}).
     */
    public static final String DEFAULT_PASSWORD = "uc";

    /**
     * 
     * @return
     */
    public static String getRoleName() {
        return Gateway.getProperties().getString("UserCode.roleOverride", DEFAULT_ROLE);
    }

    /**
     * 
     * @return
     */
    public static List<String> getRolePermissions() {
        String permissionString = Gateway.getProperties().getString(getRoleName() + ".permissions", "*");
        return Arrays.asList(permissionString.split(","));
    }

    /**
     * 
     * @return
     */
    public static ImportRole getImportRole() {
        RolePath rp = new RolePath(new RolePath(), getRoleName(), true, getRolePermissions());
        return ImportRole.getImportRole(rp);
    }

    /**
     * 
     * @return
     */
    public static String getAgentName() {
        try {
            return Gateway.getProperties().getString(getRoleName()+ ".agent", InetAddress.getLocalHost().getHostName());
        }
        catch (UnknownHostException e) {
            log.error("getRole(roelName={}) ", getRoleName(), e);
            return null;
        }
    }

    /**
     * 
     * @return
     */
    public static String getAgentPassword() {
        return Gateway.getProperties().getString(getRoleName() + ".password", DEFAULT_PASSWORD);
    }

    /**
     * 
     * @return
     */
    public static ImportAgent getImportAgent() {
        ImportAgent iAgent = new ImportAgent(getAgentName(), getAgentPassword());
        iAgent.addRole(getImportRole());
        return iAgent;
    }

    /**
     * 
     * @param args
     * @throws Exception
     */
    static public void main(String[] args) throws Exception {
        standardInitialisation(args);

        DeploymentOptions options = new DeploymentOptions().setWorker(true).setInstances(4);
        Gateway.getVertx().deployVerticle(UserCodeVerticle.class, options);
    }
}
