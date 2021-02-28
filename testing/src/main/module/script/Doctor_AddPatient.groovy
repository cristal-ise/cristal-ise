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
import org.cristalise.kernel.collection.Dependency
import org.cristalise.kernel.persistency.outcome.Outcome
import org.cristalise.kernel.utils.CastorHashMap

//import org.slf4j.Logger
//import org.slf4j.LoggerFactory
//import groovy.transform.Field
//@Field final Logger log = LoggerFactory.getLogger('org.cristalise.testing.scripts.Doctor.AddPatient')

Outcome outcome = job.getOutcome()
def memberName = outcome.getField('MemberName')

def dep = new Dependency('Patients')
dep.addMember(agent.getItem("/integTest/Patients/$memberName").getPath(), new CastorHashMap(), '', null);

outcome.appendXmlFragment('/Doctor_Patient/AddMembersToCollection', agent.marshall(dep))
