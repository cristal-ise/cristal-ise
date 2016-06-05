/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2016 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.kernel.persistency.outcome;

import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;

/**
 * OutcomeInitiator implementation using on Activity Properties. It is based on the convention that the name 
 * of the Activity Property is a XPath expression. All Activity properties in the Job shall be resolved already, 
 * i.e. all the ActivityHelpers were executed when the Job was retrieved.
 */
public class XPathOutcomeInitiator extends EmptyOutcomeInitiator {

    private String propNamePattern = null;

    public XPathOutcomeInitiator() {
        propNamePattern = Gateway.getProperties().getString("XPathOutcomeInitiator.PropertyNamePattern", "^/");
    }

    /**
     * 
     */
    @Override
    public String initOutcome(Job job) throws InvalidDataException {
        return initOutcomeInstance(job).getData();
    }

    /**
     * 
     */
    public Outcome initOutcomeInstance(Job job) throws InvalidDataException {
        Map<String, Object> actProps = job.matchhActPropNames(propNamePattern);

        Outcome emptyOutcome = super.initOutcomeInstance(job);

        for(Map.Entry<String, Object> entry: actProps.entrySet()) {
            try {
                emptyOutcome.setFieldByXPath(entry.getKey(), (String) entry.getValue());
            }
            catch (XPathExpressionException e) {
                Logger.msg(5,"XPathOutcomeInitiator - Invalid XPath:"+entry.getKey());
                Logger.error(e);
            }
        }

        return emptyOutcome;
    }
}
