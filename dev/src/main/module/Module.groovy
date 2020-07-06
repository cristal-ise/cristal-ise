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
    Info(description: 'CRISTAL-iSE Development Items to implement CRUD functionality.', version: '${version}') {
    }
    Config(name: 'OutcomeInit.Dev',               value: 'org.cristalise.dev.DevObjectOutcomeInitiator')
    Config(name: 'OverrideScriptLang.javascript', value: 'rhino')
    Config(name: 'OverrideScriptLang.JavaScript', value: 'rhino')
    Config(name: 'OverrideScriptLang.js',         value: 'rhino')
    Config(name: 'OverrideScriptLang.JS',         value: 'rhino')
    Config(name: 'OverrideScriptLang.ECMAScript', value: 'rhino')
    Config(name: 'OverrideScriptLang.ecmascript', value: 'rhino')

    Url('org/cristalise/dev/resources/')

    include(moduleDir+'/Property.groovy')
    include(moduleDir+'/CrudFactory.groovy')
    include(moduleDir+'/Schema.groovy')
    include(moduleDir+'/Script.groovy')
    include(moduleDir+'/Activity.groovy')
    include(moduleDir+'/Workflow.groovy')
    include(moduleDir+'/Item.groovy')
}
