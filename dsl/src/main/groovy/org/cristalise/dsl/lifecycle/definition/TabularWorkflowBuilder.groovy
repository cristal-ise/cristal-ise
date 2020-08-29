package org.cristalise.dsl.lifecycle.definition

import org.cristalise.dsl.csv.TabularGroovyParser
import org.cristalise.dsl.persistency.outcome.Struct
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.lifecycle.CompositeActivityDef

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic @Slf4j
class TabularWorkflowBuilder {

    Layout build(TabularGroovyParser parser) {
        parser.eachRow() { Map<String, Object> record, int i ->
            switch (record['layout']['class']) {
                case 'activity': convertToActivity(record); break;
                default:
                    throw new InvalidDataException('Uncovered class value:' + record['layout']['class'])
            }
        }
        
        return null;
    }
    
    private void convertToActivity(Map<String, Object> record) {
        log.info('convertToActivity() - {}', record)
    }
}
