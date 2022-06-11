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
package org.cristalise.dsl.lifecycle.definition

import org.cristalise.dsl.csv.TabularGroovyParser
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine
import org.cristalise.kernel.persistency.outcome.Schema
import org.cristalise.kernel.querying.Query
import org.cristalise.kernel.scripting.Script

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic @Slf4j
class TabularActivityDefBuilder {
    
    protected CompositeActivityDef caDef
    protected CompActDefLayoutDelegate caDefLayoutDelegate
    
    Map<String, ActivityDef> actDefMap = [:]

    public TabularActivityDefBuilder(CompositeActivityDef ca) {
        caDef = ca
        caDefLayoutDelegate = new CompActDefLayoutDelegate(ca)
    }

    CompositeActivityDef build(TabularGroovyParser parser) {
        parser.eachRow() { Map<String, Map<String, Object>> record, int i ->
            switch (record['layout']['class']) {
                case 'Elementary': convertToElementary(record); break;
                case 'Loop': startLoopBlock(record); break;
                case 'LoopInfinite': startInfiniteLoopBlock(record); break;
                case 'LoopEnd': endCurrentBlock(record); break;
                default:
                    throw new InvalidDataException('Uncovered class value:' + record['layout']['class'])
            }
        }

        return caDef
    }

    private void startLoopBlock(Map<String, Map<String, Object>> record) {
        log.info('startLoopBlock() - {}', record)
    }

    private void startInfiniteLoopBlock(Map<String, Map<String, Object>> record) {
        log.info('startInfiniteLoopBlock() - {}', record)
    }

    private void endCurrentBlock(Map<String, Map<String, Object>> record) {
        log.info('endCurrentBlock() - {}', record)
    }

    private ActivityDef createActivityDef(Map<String, Map<String, Object>> record) {
        def actDef = new ActivityDef()
        def layout = record['layout']
        def actRef = layout['activityReference'] as String

        def nameAndVersion = actRef.split(':')
        actDef.name = nameAndVersion[0]
        if (nameAndVersion.size() > 1) actDef.version = nameAndVersion[1] as Integer

        def reference = record.reference

        if (reference?.schema) {
            nameAndVersion = ((String)((Map)record.reference).schema).split(':')
            actDef.schema = new Schema(nameAndVersion[0], nameAndVersion[1] as Integer, "")
        }

        if (reference?.script) {
            nameAndVersion = ((String)((Map)record.reference).script).split(':')
//            def fakeScript = "<cristalscript><script language='javascript' name='{$nameAndVersion[0]}'> </script></cristalscript>"
            actDef.script = new Script(nameAndVersion[0], nameAndVersion[1] as Integer, null, null)
        }

        if (reference?.query) {
            nameAndVersion = ((String)((Map)record.reference).query).split(':')
            actDef.query = new Query(nameAndVersion[0], nameAndVersion[1] as Integer, "")
        }

        if (reference?.stateMachine) {
            nameAndVersion = ((String)((Map)record.reference).stateMachine).split(':')
            actDef.stateMachine = new StateMachine(nameAndVersion[0], nameAndVersion[1] as Integer)
        }
        
        return actDef
    }

    private void convertToElementary(Map<String, Map<String, Object>> record) {
        log.info('convertToElementary() - {}', record)

        def layout = record['layout']
        def actRef = layout['activityReference'] as String
        def actDef = actDefMap[actRef]

        if (! actDef) {
            actDef = createActivityDef(record)
            actDefMap[actRef] = actDef
        }
        
        def actSlotName = layout['name'] as String
        caDefLayoutDelegate.Act(actSlotName, actDef)
    }
}
