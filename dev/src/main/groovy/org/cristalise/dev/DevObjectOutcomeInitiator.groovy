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
import static org.cristalise.kernel.process.resource.BuiltInResources.QUERY_RESOURCE;
import static org.cristalise.kernel.process.resource.BuiltInResources.SCHEMA_RESOURCE;
import static org.cristalise.kernel.process.resource.BuiltInResources.SCRIPT_RESOURCE;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.OutcomeInitiator;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.resource.BuiltInResources;
import org.cristalise.kernel.utils.DescriptionObject;

import groovy.util.logging.Slf4j


/**
 * Creates empty DescriptionObject and marshals them or loads the 'new' XML from Factory
 */
@Slf4j
public class DevObjectOutcomeInitiator implements OutcomeInitiator {

    @Override
    public Outcome initOutcomeInstance(Job job) throws InvalidDataException {
        return new Outcome(initOutcome(job));
    }

    @Override
    public String initOutcome(Job job) throws InvalidDataException {
        String type = job.getActPropString(SCHEMA_NAME);
        BuiltInResources res = BuiltInResources.getValue(type);
        String itemName = null;

        try {
            itemName = job.getItemProxy().getName();
        }
        catch (ObjectNotFoundException | InvalidItemPathException e) {
            throw new InvalidDataException(e.getMessage());
        }

        DescriptionObject emptyObj = res.getDescriptionObject(itemName);

        // these DescObject cannot be marshalled by castor due to the use of CDATA
        if (res == SCHEMA_RESOURCE || res == SCRIPT_RESOURCE || res == QUERY_RESOURCE) {
            DomainPath factoryPath = new DomainPath("/desc/dev/" + res.getSchemaName() + "Factory");

            try {
                ItemProxy factory = Gateway.getProxyManager().getProxy(factoryPath);
                Viewpoint newInstance = factory.getViewpoint(res.getSchemaName(), "last");
                return newInstance.getOutcome().getData();
            }
            catch (Exception e) {
                log.error("Error creating new schema:'"+res.getSchemaName()+"'", e);
                throw new InvalidDataException("Error loading new schema:'"+res.getSchemaName()+"' exception:"+e.getMessage());
            }
        }
        else {
            try {
                return Gateway.getMarshaller().marshall(emptyObj);
            }
            catch (Exception e) {
                log.error("Error creating empty type:'"+type+"'", e);
                throw new InvalidDataException("Error creating empty type:'"+type+"' exception:"+e.getMessage());
            }
        }
    }
}
