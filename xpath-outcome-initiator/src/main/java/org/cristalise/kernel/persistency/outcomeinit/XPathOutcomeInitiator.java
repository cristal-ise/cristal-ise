/**
 * This file is part of the CRISTAL-iSE XPath Outcome Initiator module.
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
package org.cristalise.kernel.persistency.outcomeinit;

import static org.cristalise.kernel.persistency.outcomeinit.SystemProperties.XPathOutcomeInitiator_PropertyNamePrefix;

import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.entity.Job;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.mvel2.templates.TemplateRuntime;

import lombok.extern.slf4j.Slf4j;

/**
 * OutcomeInitiator implementation using on Activity Properties. It is based on the convention that the name
 * of the Activity Property is a XPath expression. All Activity properties in the Job shall be resolved already,
 * i.e. all the DataHelpers were executed while the Job was created.
 */
@Slf4j
public class XPathOutcomeInitiator extends EmptyOutcomeInitiator {

    private final String propNamePrefix;

    public XPathOutcomeInitiator() {
        propNamePrefix = XPathOutcomeInitiator_PropertyNamePrefix.getString();
    }

    public XPathOutcomeInitiator(String prefix) {
        propNamePrefix = prefix;
    }

    /**
     *
     */
    @Override
    public String initOutcome(Job job) throws InvalidDataException {
        log.debug("initOutcome() - stepName:"+job.getStepName());
        return initOutcomeInstance(job).getData();
    }

    /**
     *
     */
    @Override
    public Outcome initOutcomeInstance(Job job) throws InvalidDataException {
        //calls implementation of EmptyOutcomeInitiator
        Outcome xpathOutcome = super.initOutcomeInstance(job);

        Map<String, Object> matchedProps = job.matchActPropNames(propNamePrefix);

        for(Map.Entry<String, Object> entry: matchedProps.entrySet()) {
            try {
                String xpath;
                String value = (String)entry.getValue();

                if ("/".equals(propNamePrefix)) xpath = entry.getKey();
                else                            xpath = entry.getKey().substring(propNamePrefix.length());

                log.debug("initOutcomeInstance() - Using Property xpath:"+xpath+" value:"+value);

                if(StringUtils.isEmpty(value)) throw new InvalidDataException("Value is NULL/EMPTY for Property name:'"+xpath+"'");

                value = evaluate(value, job);

                if(value.startsWith("<") && value.endsWith(">")) {
                    log.debug("initOutcomeInstance() - Updating XML fregment with xpath:"+xpath);
                    xpathOutcome.appendXmlFragment(xpath, value);
                }
                else {
                    xpathOutcome.setFieldByXPath(xpath, value);
                }
            }
            catch (XPathExpressionException e) {
                log.error("Invalid XPath:"+entry.getKey(), e);
                throw new InvalidDataException(e.getMessage());
            }
        }

        return xpathOutcome;
    }

    /**
     * Evaluates the content of the provided value using an expression language.
     * 
     * The default implementation is using MVEL expression language. 
     * 
     * You could re-implement this method at upper classes to use your expression language.
     * 
     * @param value
     * @param job
     * @return
     */
	protected String evaluate(String value, Job job) {
		value = (String) TemplateRuntime.eval(value, job.getActProps());
		return value;
	}
}
