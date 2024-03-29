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
package org.cristalise.kernel.lifecycle.instance.predefined.agent;

import static org.cristalise.kernel.SystemProperties.Lifecycle_Sign_agentNameField;
import static org.cristalise.kernel.SystemProperties.Lifecycle_Sign_passwordField;
import static org.cristalise.kernel.SystemProperties.Lifecycle_Sign_signedFlagField;

import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.Job;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.persistency.outcome.Outcome;

import lombok.extern.slf4j.Slf4j;

/**
 * {@value #description}
 */
@Slf4j
public class Sign extends Authenticate {

    public static final String description = "Authenticates the given user and records the Sign event in the system together with the execution context";

    public static final String agentNameField  = Lifecycle_Sign_agentNameField.getString();
    public static final String passwordField   = Lifecycle_Sign_passwordField.getString();
    public static final String signedFlagField = Lifecycle_Sign_signedFlagField.getString();

    public Sign() {
        super("SimpleElectonicSignature", description);
    }

    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath itemPath, int transitionID, String requestData, TransactionKey transactionKey)
            throws InvalidDataException, ObjectNotFoundException, ObjectCannotBeUpdated, CannotManageException, PersistencyException
    {
        log.debug("Called by {} on {}", agent.getAgentName(transactionKey), itemPath);

        Outcome req = new Outcome(requestData);
        authenticate(agent, itemPath, bundleData(req.getField("AgentName"), req.getField("Password")), transactionKey);
        req.setField("Password", REDACTED);

        return req.getData();
    }

    /**
     */
    public static String getSimpleElectonicSignature(Job job) throws InvalidDataException, ObjectNotFoundException {
        if (job.getOutcome().hasField(agentNameField) && job.getOutcome().hasField(passwordField)) {
            StringBuffer xml = new StringBuffer("<SimpleElectonicSignature>");

            xml.append("<AgentName>").append(job.getOutcome().getField(agentNameField)).append("</AgentName>");
            xml.append("<Password>") .append(job.getOutcome().getField(passwordField)) .append("</Password>");

            xml.append("<ExecutionContext>");
            xml.append("<ItemPath>")     .append(job.getItemUUID())     .append("</ItemPath>");
            xml.append("<SchemaName>")   .append(job.getSchemaName())   .append("</SchemaName>");
            xml.append("<SchemaVersion>").append(job.getSchemaVersion()).append("</SchemaVersion>");
            xml.append("<ActivityType>") .append(job.getStepType())     .append("</ActivityType>");
            xml.append("<ActivityName>") .append(job.getStepName())     .append("</ActivityName>");
            xml.append("<StepPath>")     .append(job.getStepPath())     .append("</StepPath>");
            xml.append("</ExecutionContext>");

            xml.append("</SimpleElectonicSignature>");

            job.getOutcome().setField(passwordField, REDACTED);
            if (job.getOutcome().hasField(signedFlagField)) job.getOutcome().setField(signedFlagField, "true");

            return xml.toString();
        }
        else {
            throw new InvalidDataException("Outcome does not contain AgentName or Password fields - job:"+job);
        }
    }
}
