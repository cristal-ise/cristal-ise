package org.cristalise.dsl.lifecycle.definition

import org.cristalise.dsl.csv.TabularGroovyParser
import org.cristalise.dsl.persistency.outcome.Struct
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.lifecycle.CompositeActivityDef

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@Slf4j
class TabularWorkflowDefBuilder {
    
    Layout layout = new Layout()

    Layout build(TabularGroovyParser parser) {
        parser.eachRow() { Map<String, Object> record, int i ->
            switch (record['layout']['class']) {
                case 'activity': convertToActivity(record); break;
                case 'loop': convertToLoop(record); break;
                default:
                    throw new InvalidDataException('Uncovered class value:' + record['layout']['class'])
            }
        }

        return layout;
    }

    private void convertToActivity(Map<String, Object> record) {
        log.info('convertToActivity() - {}', record)

        def activityMap = record.layout.subMap(LayoutActivity.keys)
        if (record.property.Name) activityMap.name = record.property.Name

        def act = new LayoutActivity(activityMap)

        layout.children.add(act)
    }

    private void convertToLoop(Map<String, Object> record) {
        log.info('convertToLoop() - {}', record)
    }
}
