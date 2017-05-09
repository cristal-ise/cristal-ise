/**
 * This file is part of the CRISTAL-iSE Development Module.
 * Copyright (c) 2001-2017 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.dev;

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCHEMA_NAME;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lifecycle.ActivityDef;
import org.cristalise.kernel.lifecycle.CompositeActivityDef;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.OutcomeInitiator;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.DescriptionObject;
import org.cristalise.kernel.utils.Logger;


/**
 * Creates empty object for Activities and StateMachine or loads empty one from Factory
 *
 */
public class DevObjectOutcomeInitiator implements OutcomeInitiator {

    @Override
    public Outcome initOutcomeInstance(Job job) throws InvalidDataException {
    	return new Outcome(initOutcome(job));
    }

    @Override
    public String initOutcome(Job job) throws InvalidDataException {
        String type = job.getActPropString(SCHEMA_NAME);

        DescriptionObject emptyObj = null;

        if      (type.equals("CompositeActivityDef"))  emptyObj = new CompositeActivityDef();
        else if (type.equals("ElementaryActivityDef")) emptyObj = new ActivityDef();
        else if (type.equals("StateMachine"))          emptyObj = new StateMachine();

        if (emptyObj != null) {
            try {
                emptyObj.setName(job.getItemProxy().getName());
                return Gateway.getMarshaller().marshall(emptyObj);
            }
            catch (Exception e) {
                Logger.error("Error creating empty "+type);
                Logger.error(e);
                return null;
            }
        }
        else {
            DomainPath factoryPath;
            String schema;

            if (type.equals("Schema")) {
                factoryPath = new DomainPath("/desc/dev/SchemaFactory");
                schema = "Schema";
            }
            else if (type.equals("Script")) {
                factoryPath = new DomainPath("/desc/dev/ScriptFactory");
                schema = "Script";
            }
            else 
                throw new InvalidDataException("Unknown dev object type: "+type);

            ItemProxy factory;
            Viewpoint newInstance;

            try {
                factory = Gateway.getProxyManager().getProxy(factoryPath);
                newInstance = factory.getViewpoint(schema, "last");
                return newInstance.getOutcome().getData();
            }
            catch (Exception e) {
                Logger.error(e);
                throw new InvalidDataException("Error loading new "+schema);
            }
        }
    }
}
