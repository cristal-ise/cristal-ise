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

@Slf4j @CompileStatic
class TabularActivityDefBuilder {

    List<ActivityDef> actDefs = []

    List<ActivityDef> build(TabularGroovyParser parser) {
        parser.eachRow() { Map<String, Object> record, int i ->
            def type = (String)record['basic']['type']

            switch (type) {
                case 'Elementary': convertToElementary(record); break;
                case 'Composite':  convertToComposite(record);  break;
                default:
                    throw new InvalidDataException('Uncovered type value:' + type)
            }
        }

        return actDefs
    }

    private void convertToElementary(Map<String, Object> record) {
        log.info('convertToElementary() - {}', record)
        def actDef = new ActivityDef()

        def nameAndVersion = ((String)record['basic']['name']).split(':')

        actDef.name = nameAndVersion[0]
        if (nameAndVersion.size() > 1) actDef.version = nameAndVersion[1] as Integer

        if (((Map)record?.reference)?.schema) {
            nameAndVersion = ((String)((Map)record.reference).schema).split(':')
            actDef.schema = new Schema(nameAndVersion[0], nameAndVersion[1] as Integer, "")
        }

        if (((Map)record?.reference)?.script) {
            nameAndVersion = ((String)((Map)record.reference).script).split(':')
            def fakeScript = "<cristalscript><script language='javascript' name='{$nameAndVersion[0]}'> </script></cristalscript>"
            actDef.script = new Script(nameAndVersion[0], nameAndVersion[1] as Integer, null, fakeScript)
        }

        if (((Map)record?.reference)?.query) {
            nameAndVersion = ((String)((Map)record.reference).query).split(':')
            actDef.query = new Query(nameAndVersion[0], nameAndVersion[1] as Integer, "")
        }

        if (((Map)record?.reference)?.stateMachine) {
            nameAndVersion = ((String)((Map)record.reference).stateMachine).split(':')
            actDef.stateMachine = new StateMachine(nameAndVersion[0], nameAndVersion[1] as Integer)
        }

        actDefs.add(actDef)
    }

    private void convertToComposite(Map<String, Object> record) {
        log.info('convertToComposite() - {}', record)
        def actDef = new CompositeActivityDef()

        actDefs.add(actDef)
    }
}
