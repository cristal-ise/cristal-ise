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
package org.cristalise.kernel.lifecycle.instance;

import java.io.IOException;
import java.util.List;

import org.cristalise.kernel.common.CriseVertxException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.entity.agent.JobArrayList;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lifecycle.instance.predefined.agent.RefreshJobList;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.CastorHashMap;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JobPusherVerticle extends AbstractVerticle {
    public static final String ebAddress = "cristalise-jobPusher";

    @Override
    public void start() {
        vertx.eventBus().consumer(ebAddress, message -> {
            JsonObject msg = (JsonObject) message.body();
            log.trace("handler() - message.body:{}", msg);

            try {
                final AgentPath agentPath = new AgentPath(msg.getString("agentPath"));
                final ItemPath itemPath  = new ItemPath(msg.getString("itemPath"));
                final String stepPath = msg.getString("stepPath");

                ItemProxy item = Gateway.getProxy(itemPath);
                AgentProxy agent = Gateway.getAgentProxy(agentPath);

                vertx.executeBlocking((toto) -> {
                    try {
                        List<Job> jobs = item.getJobs(agentPath, stepPath);

                        if (jobs.isEmpty()) {
                            // FIXME: this is a hack to send the itemPath and stepPath to RefreshJobList so it can cleanup the actual list
                            jobs.add(new Job(-1, itemPath, "stepName", stepPath, "stepType", 
                                    null, ""/*originStateName*/, ""/*targetStateName*/, agentPath.getRoles()[0].getName(),
                                    agentPath, new CastorHashMap(), null/*creationDate*/));
                        }

                        String stringJobs = Gateway.getMarshaller().marshall(new JobArrayList(jobs));

                        agent.execute(agent, RefreshJobList.class, stringJobs);
                    }
                    catch (CriseVertxException e) {
                        log.error("handler()", e);
                    }
                    catch (MarshalException | ValidationException | IOException |  MappingException e) {
                        log.error("handler()", e);
                    }
                    catch (Throwable e) {
                        log.error("handler()", e);
                    }
                });
            }
            catch (InvalidItemPathException e) {
                log.error("handler()", e);
            }
            catch (ObjectNotFoundException e) {
                log.error("handler()", e);
            }
        });

        log.info("start() - '{}' consumer configured", ebAddress);
    }
}
