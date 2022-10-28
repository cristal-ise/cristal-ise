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
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.lifecycle.instance.predefined.ChangeName
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.transform.Field

@Field final Logger log = LoggerFactory.getLogger('org.cristalise.dev.scripts.CrudEntity.ChangeName')
@Field final PredefinedStepsOutcomeBuilder builder = new PredefinedStepsOutcomeBuilder(item, job.outcome, job.schema)

def outcome = job.outcome
def currentName = item.name
def newName = outcome.getField('Name')

if (!newName) {
    throw new InvalidDataException('')
}
else if (newName != currentName) {
    if (! outcome.getNodeByXPath('//ChangeName')) {
        OutcomeBuilder builder = new OutcomeBuilder(job.schema, job.outcome)
        builder.addField('ChangeName', '')
    }

    job.outcome.appendXmlFragment('//ChangeName', PredefinedStep.bundleData(currentName, newName))
}
else {
    log.warn 'New name is equal to the current one. Nothing done!'
}