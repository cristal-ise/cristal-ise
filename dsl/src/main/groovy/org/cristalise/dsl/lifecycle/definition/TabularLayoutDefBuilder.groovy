package org.cristalise.dsl.lifecycle.definition

import org.cristalise.dsl.csv.TabularGroovyParser
import org.cristalise.dsl.persistency.outcome.Struct
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.lifecycle.CompositeActivityDef

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@Slf4j @CompileStatic
class TabularLayoutDefBuilder {
    
    void build(TabularGroovyParser parser) {
        parser.eachRow() { Map<String, Object> record, int i ->
            switch (record['layout']['class']) {
                case 'activity': convertToActivitySlotDef(record); break;
                case 'loop': startLoopBlock(record); break;
                case 'end': startLoopBlock(record); break;
                default:
                    throw new InvalidDataException('Uncovered class value:' + record['layout']['class'])
            }
        }
    }

    private void convertToActivitySlotDef(Map<String, Object> record) {
        log.info('convertToActivitySlotDef() - {}', record)

        def activityMap = ((Map)record['layout'])//.subMap(LayoutActivity.keys)
        if (record['property']['Name']) activityMap.name = (String)record['property']['Name']

        //def act = new LayoutActivity(activityMap)
        //layout.children.add(act)
    }

    private void startLoopBlock(Map<String, Object> record) {
        log.info('startLoopBlock() - {}', record)
    }

    private void endCurrentBlock(Map<String, Object> record) {
        log.info('endCurrentBlock() - {}', record)
    }
}
