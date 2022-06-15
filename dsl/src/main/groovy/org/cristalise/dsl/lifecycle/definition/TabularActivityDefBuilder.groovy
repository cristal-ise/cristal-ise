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
import org.cristalise.kernel.lifecycle.JoinDef
import org.cristalise.kernel.lifecycle.NextDef
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine
import org.cristalise.kernel.persistency.outcome.Schema
import org.cristalise.kernel.querying.Query
import org.cristalise.kernel.scripting.Script

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic @Slf4j
class TabularActivityDefBuilder {
    
    protected CompositeActivityDef caDef

    Map<String, ActivityDef> actDefMap = [:]

    /**
     * Contains the delegates of processed blocks
     */
    private List<BlockDefDelegate> blockLifo = []

    public TabularActivityDefBuilder(CompositeActivityDef ca) {
        caDef = ca
        blockLifo.add(new CompActDefLayoutDelegate(ca))
    }

    CompositeActivityDef build(TabularGroovyParser parser) {
        parser.eachRow() { Map<String, Map<String, Object>> record, int i ->
            switch (record['layout']['class']) {
                case 'Elementary': 
                    convertToElementary(record)
                    break

                case 'Loop': 
                    startLoop(record, false)
                    break

                case 'LoopInfinite': 
                    startLoop(record, true)
                    break

                case 'AndSplit': 
                    startAndSplit(record)
                    break

                case 'Block': 
                    startBlock(record)
                    break

                case 'BlockEnd': 
                case 'LoopEnd': 
                case 'End': 
                case '---': 
                    endBlock(record)
                    break

                default:
                    throw new InvalidDataException('Uncovered class value:' + record['layout']['class'])
            }
        }

        return caDef
    }

    private void initialiseDelegate(BlockDefDelegate blockD) {
        blockD.initialiseDelegate()
        blockLifo.add(blockD)
    }

    private void startLoop(Map<String, Map<String, Object>> record, boolean infinite) {
        log.info('startLoop() - {}', record)

        def currentBlockD = blockLifo.last()
        def loopD = infinite ? currentBlockD.LoopInfinite(record['property']) : currentBlockD.Loop(record['property'])
        initialiseDelegate(loopD)
        currentBlockD.lastSlotDef = loopD.joinDefFirst
    }

    private void startAndSplit(Map<String, Map<String, Object>> record) {
        log.info('startAndSplit() - {}', record)
        def currentBlockD = blockLifo.last()
        def andD = currentBlockD.AndSplit(record['property'])
        initialiseDelegate(andD)
        currentBlockD.lastSlotDef = andD.andSplitDef
    }

    private void startBlock(Map<String, Map<String, Object>> record) {
        log.info('startBlock() - {}', record)
        def currentBlockD = blockLifo.last()
        def blockD = currentBlockD.Block(record['property'])
        initialiseDelegate(blockD)
    }

    private void endBlock(Map<String, Map<String, Object>> record) {
        def closedBlockD = blockLifo.removeLast()

        log.info('endBlock({}) - {}', closedBlockD.class.simpleName, record)

        closedBlockD.finaliseDelegate()

        def currentBlockD = blockLifo.last()

        currentBlockD.finaliseBlock(closedBlockD.lastSlotDef, closedBlockD.firstEdge, record?.property?.Alias)
        //currentBlockD.lastSlotDef = closedBlockD.lastSlotDef
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
            nameAndVersion = ((String)reference.schema).split(':')
            actDef.schema = new Schema(nameAndVersion[0], nameAndVersion[1] as Integer, "")
        }

        if (reference?.script) {
            nameAndVersion = ((String)reference.script).split(':')
//            def fakeScript = "<cristalscript><script language='javascript' name='{$nameAndVersion[0]}'> </script></cristalscript>"
            actDef.script = new Script(nameAndVersion[0], nameAndVersion[1] as Integer, null, null)
        }

        if (reference?.query) {
            nameAndVersion = ((String)reference.query).split(':')
            actDef.query = new Query(nameAndVersion[0], nameAndVersion[1] as Integer, "")
        }

        if (reference?.stateMachine) {
            nameAndVersion = ((String)reference.stateMachine).split(':')
            actDef.stateMachine = new StateMachine(nameAndVersion[0], nameAndVersion[1] as Integer)
        }

        return actDef
    }

    private ActivityDef retrieveActivityDef(String actRef, Map<String, Map<String, Object>> record) {
        def actDef = actDefMap[actRef]

        if (!actDef) {
            actDef = createActivityDef(record)
            actDefMap[actRef] = actDef
        }

        return actDef
    }

    private void convertToElementary(Map<String, Map<String, Object>> record) {
        log.info('convertToElementary() - {}', record)

        def layout = record['layout']
        def actSlotName = layout['name'] as String
        def actRef = layout['activityReference'] as String

        def actDef = retrieveActivityDef(actRef, record)

        def currentBlockD = blockLifo.last()
        currentBlockD.Act(actSlotName, actDef)
    }
}
