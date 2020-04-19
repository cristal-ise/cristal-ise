package org.cristalise.dsl.persistency.outcome

import org.apache.poi.xssf.usermodel.XSSFSheet
import org.cristalise.dsl.excel.ExcelGroovyParser
import org.cristalise.kernel.common.InvalidDataException

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@Slf4j
class ExcelSchemaBuilder {
    def structKeys = ['name', 'documentation', 'multiplicity', 'useSequence']
    def fieldKeys = [
        'name', 'type', 'documentation', 'multiplicity', 'values', 'pattern', 'default',
        'length', 'minLength', 'maxLength', 
        'range', 'minInclusive', 'maxInclusive', 'minExclusive', 'maxExclusive',
        'totalDigits', 'fractionDigits'
    ]

    /**
     * Used to 
     */
    def parentLifo = []

    Struct build(XSSFSheet sheet) {
        ExcelGroovyParser.eachRow(sheet) { Map<String, Object> record ->
            switch (record['class']) {
                case 'struct':    convertToStruct(record); break;
                case 'field':     convertToField(record); break;
                case 'attribute': convertToAttribute(record); break;
                default:
                    throw new InvalidDataException('Uncovered class value:' + record['class'])
            }
        }

        assert parentLifo.size() == 1

        return (Struct) parentLifo.removeLast()
    }

    private void convertToStruct(Map<String, Object> record) {
        log.debug 'convertToStruct() - {}', record

        def fMap = record.subMap(structKeys)
        Struct parentS = parentLifo.empty ? null : (Struct)parentLifo.last()

        // This is the closing record of the currently processed struct declaration
        if (parentS && fMap.name == parentS.name) {
            if (parentLifo.size() > 1) parentLifo.removeLast() // remove it from lifo
        }
        else {
            def s = new Struct(fMap)

            // conversion code comes here

            if (parentS) parentS.addStruct(s)
            parentLifo.add(s)
        }
    }

    private void convertToField(Map<String, Object> record) {
        log.debug 'convertToField() - {}', record

        def fMap = record.subMap(fieldKeys)

        // convert comma separated string to list before calling map constructor
        if (fMap.values) fMap.values = fMap.values.trim().split('\\s*,\\s*')

        def f = new Field(fMap)

        if (fMap.multiplicity) {
            f.setMultiplicity(fMap.multiplicity)
        }

        if (fMap.range) {
            f.setRange(fMap.range)

            if (! (f.type == 'xs:integer' || f.type == 'xs:decimal')) {
                throw new InvalidDataException(
                    "Field '${f.name}' uses invalid type '${f.type}'. 'range' must be integer or decimal")
            }
        }

        if (fMap.totalDigits != null || fMap.fractionDigits!= null) {
            if (f.type != 'xs:decimal') {
                throw new InvalidDataException(
                    "Field '${f.name}' uses invalid type '${f.type}'. 'totalDigits' and 'fractionDigits' must be decimal")
            }
        }
            
        def s = (Struct)parentLifo.last()
        s.addField(f)
    }

    private void convertToAttribute(Map<String, Object> record) {
        log.debug 'convertToAttribute() - {}', record
        throw new UnsupportedOperationException('attribute is not implemented yet record:'+record)
    }
}
