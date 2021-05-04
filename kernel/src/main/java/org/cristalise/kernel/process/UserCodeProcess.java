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

import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;

import io.vertx.core.DeploymentOptions;

public class UserCodeProcess extends StandardClient {

    /**
     * Defines the default role (value:{@value}). It also used as a prefix for every configuration property
     * eg: UserCode.StateMachine.startTransition
     */
    public static final String DEFAULT_ROLE = "UserCode";

    static public void main(String[] args) throws Exception {
        Gateway.init(readC2KArgs(args));

        String prefix = Gateway.getProperties().getString("UserCode.roleOverride", UserCodeProcess.DEFAULT_ROLE);

        UserCodeProcess proc = new UserCodeProcess();

        proc.login(
                Gateway.getProperties().getString(prefix + ".agent", InetAddress.getLocalHost().getHostName()),
                Gateway.getProperties().getString(prefix + ".password", "uc"),
                Gateway.getProperties().getString("AuthResource", "Cristal"));

        StateMachine sm = getRequiredStateMachine(prefix, null, "boot/SM/Default.xml");

        StandardClient.createClientVerticles();

        DeploymentOptions options = new DeploymentOptions().setWorker(true);
        Gateway.getVertx().deployVerticle(new UserCodeVerticle(prefix, proc.agent, sm), options);
    }
}
