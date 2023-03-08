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

 @BaseScript(org.cristalise.dsl.module.ModuleScriptBase)
import groovy.transform.BaseScript
import groovy.transform.SourceURI

@SourceURI
URI scriptUri

setModuleDir scriptUri

Module(ns: 'dev', name: 'CristaliseDev', version: 0) {
    Info(description: 'CRISTAL-iSE Development Items to implement CRUD functionality.', version: '${version}') {}

    Config(name: 'OutcomeInit.Dev',                  value: 'org.cristalise.dev.DevObjectOutcomeInitiator')
    Config(name: 'OutcomeInit.dev',                  value: 'org.cristalise.dev.DevObjectOutcomeInitiator')
    Config(name: 'Script.EngineOverride.javascript', value: 'rhino')
    Config(name: 'Script.EngineOverride.JavaScript', value: 'rhino')
    Config(name: 'Script.EngineOverride.js',         value: 'rhino')
    Config(name: 'Script.EngineOverride.JS',         value: 'rhino')
    Config(name: 'Script.EngineOverride.ECMAScript', value: 'rhino')
    Config(name: 'Script.EngineOverride.ecmascript', value: 'rhino')

    Url('org/cristalise/dev/resources/')

    Contexts {
        DomainContext('/desc/dev', 0)
        DomainContext('/desc/ActivityDesc/dev', 0)
        DomainContext('/desc/PropertyDesc/dev', 0)
        DomainContext('/desc/Module/dev', 0)
        DomainContext('/desc/Schema/dev', 0)
        DomainContext('/desc/Script/dev', 0)
        DomainContext('/desc/Query/dev', 0)
        DomainContext('/desc/StateMachine/dev', 0)
        DomainContext('/desc/ItemDesc/dev', 0)
        DomainContext('/desc/AgentDesc/dev', 0)
        DomainContext('/desc/RoleDesc/dev', 0)
        DomainContext('/desc/DomainContext/dev', 0)
    }

    include(moduleDir+'/Property.groovy')
    include(moduleDir+'/CrudState.groovy')
    include(moduleDir+'/CrudDependency.groovy')
    include(moduleDir+'/CrudFactory.groovy')
    include(moduleDir+'/Schema.groovy')
    include(moduleDir+'/Script.groovy')
    include(moduleDir+'/Activity.groovy')
    include(moduleDir+'/Workflow.groovy')
    include(moduleDir+'/Item.groovy')
    include(moduleDir+'/Description.groovy')
}
