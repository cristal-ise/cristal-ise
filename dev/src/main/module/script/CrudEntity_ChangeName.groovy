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
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.transform.Field

//--------------------------------------------------
// item, agent and job are injected by the Script class
// automatically so these declaration are only needed
// to write the script with code completion.
// COMMENT OUT before you run the module generators
//--------------------------------------------------
// ItemProxy item
// AgentProxy agent
// Job job
//--------------------------------------------------

@Field final Logger log = LoggerFactory.getLogger('org.cristalise.dev.scripts.CrudEntity.ChangeName')

def outcome = job.getOutcome()
def currentName = item.getName()
def newName = outcome.getField('Name')

if (!newName) {
    throw new InvalidDataException('')
}
else if (newName != currentName) {
    def params = new String[2]

    params[0] = currentName
    params[1] = newName

    agent.execute(item, ChangeName.getClass(), params)
}
else
    log.warn 'New name is equal to the current one. Nothing done!'
