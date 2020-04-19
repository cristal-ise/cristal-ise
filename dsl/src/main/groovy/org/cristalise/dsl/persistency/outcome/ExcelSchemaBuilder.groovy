package org.cristalise.dsl.persistency.outcome

import org.apache.poi.xssf.usermodel.XSSFSheet
import org.cristalise.dsl.excel.ExcelGroovyParser
import org.cristalise.kernel.common.InvalidDataException

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

    def attributeKeys = [
        'name', 'type', 'multiplicity', 'values', 'pattern', 'default',
        'range', 'minInclusive', 'maxInclusive', 'minExclusive', 'maxExclusive',
        'totalDigits', 'fractionDigits'
    ]

    /**
     * Contains the actually processed Struct or Field
     */
    def parentLifo = []

    /**
     * 
     * @param sheet
     * @return
     */
    Struct build(XSSFSheet sheet) {
        ExcelGroovyParser.eachRow(sheet) { Map<String, Object> record ->
            switch (record['class']) {
                case 'struct'   : convertToStruct(record); break;
                case 'field'    : convertToField(record); break;
                case 'attribute': convertToAttribute(record); break;
                default:
                    throw new InvalidDataException('Uncovered class value:' + record['class'])
            }
        }

        return (Struct) parentLifo.pop()
    }

    /**
     * Convert comma separated string to list before calling map constructor
     */
    private void fixValues(Map map) {
        if (map.values) map.values = map.values.trim().split('\\s*,\\s*')
    }

    private void convertToStruct(Map<String, Object> record) {
        log.debug 'convertToStruct() - {}', record
        
        // if previous row was a field remove it from lifo
        if (parentLifo.size() > 1 && parentLifo.last() instanceof Field) parentLifo.removeLast()

        def fMap = record.subMap(structKeys)
        Struct parentS = parentLifo.empty ? null : (Struct)parentLifo.last()

        // This is the closing record of the currently processed struct declaration
        if (parentS && (fMap.name == parentS.name || fMap.name.startsWith('---'))) {
            if (parentLifo.size() > 1) parentLifo.removeLast() // remove it from lifo
        }
        else {
            def s = new Struct(fMap)

            // conversion code comes here

            if (parentS) parentS.addStruct(s)
            parentLifo.add(s)
        }
    }

    private void setRange(Map record, Attribute attrOrField) {
        attrOrField.setRange(record.range)

        if (! (attrOrField.type == 'xs:integer' || attrOrField.type == 'xs:decimal')) {
            throw new InvalidDataException(
                    "Field/Attribute '${attrOrField.name}' uses invalid type '${attrOrField.type}'. 'range' must be integer or decimal")
        }
    }

    private void convertToField(Map<String, Object> record) {
        log.debug 'convertToField() - {}', record

        def fMap = record.subMap(fieldKeys)

        fixValues(fMap)

        def f = new Field(fMap)

        if (fMap.multiplicity) f.setMultiplicity(fMap.multiplicity)
        if (fMap.range) setRange(fMap, f)

        if (fMap.totalDigits != null || fMap.fractionDigits!= null) {
            if (f.type != 'xs:decimal') {
                throw new InvalidDataException(
                    "Field '${f.name}' uses invalid type '${f.type}'. 'totalDigits' and 'fractionDigits' must be decimal")
            }
        }

        // perhaps previous row was a field - see comment bellow
        if (parentLifo.last() instanceof Field) parentLifo.removeLast()

        def s = (Struct) parentLifo.last()
        s.addField(f)

        // next row can be an attribute of this field
        parentLifo.add(f)
    }

    private void convertToAttribute(Map<String, Object> record) {
        log.debug 'convertToAttribute() - {}', record
        def aMap = record.subMap(attributeKeys)

        if (record.documentation)
            throw new InvalidDataException("Attribute '${aMap.name}' cannot have a documentation")

        fixValues(aMap)

        def a = new Attribute(aMap)

        if (aMap.multiplicity) a.setMultiplicity(aMap.multiplicity)
        if (aMap.range) setRange(aMap, a)

        def prev = parentLifo.last()

        if      (parentLifo.last() instanceof Struct) ((Struct)prev).attributes.add(a)
        else if (parentLifo.last() instanceof Field)  ((Field)prev).attributes.add(a)
    }
}
