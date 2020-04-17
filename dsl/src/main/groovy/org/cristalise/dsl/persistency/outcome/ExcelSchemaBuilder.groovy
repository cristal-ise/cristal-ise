package org.cristalise.dsl.persistency.outcome

import org.apache.poi.xssf.usermodel.XSSFSheet
import org.cristalise.dsl.excel.ExcelGroovyParser
import org.cristalise.kernel.common.InvalidDataException

import groovy.util.logging.Slf4j

@Slf4j
class ExcelSchemaBuilder {
    def structKeys = ['name', 'documentation', 'multiplicity', 'useSequence', 'orderOfElements']
    def fieldKeys = ['name', 'type', 'documentation', 'values', 'pattern', 'range', 'length', 'minLength', 'maxLength']

    Struct build(XSSFSheet sheet) {
        def parentLifo = []

        ExcelGroovyParser.eachRow(sheet) { Map<String, Object> record ->
            switch (record['class']) {
                case 'struct':
                    log.debug 'struct -- ' + record
                    def s = new Struct(record.subMap(structKeys))
                    parentLifo.push(s)
                    break
                case 'field':
                    log.debug 'field -- ' + record
                    def f = new Field(record.subMap(fieldKeys))
                    ((Struct)parentLifo.last()).fields[f.name] = f
                    ((Struct)parentLifo.last()).orderOfElements.add(f.name)
                    break
                default:
                    throw new InvalidDataException()
            }
        }
        
        def s = (Struct) parentLifo.pop()
        return s
    }
}
