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

    def unitKeys = ['values', 'default']

    /**
     * Contains the actually processed Structs or Field
     */
    def parentLifo = []

    /**
     * 
     * @param sheet
     * @return
     */
    Struct build(XSSFSheet sheet) {
        ExcelGroovyParser.eachRow(sheet, 2) { Map<String, Object> record, int i ->
            switch (record['xsd']['class']) {
                case 'struct'   : convertToStruct(record); break;
                case 'field'    : convertToField(record); break;
                case 'attribute': convertToAttribute(record); break;
                default:
                    throw new InvalidDataException('Uncovered class value:' + record['xsd']['class'])
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

        def sMap = record['xsd'].subMap(structKeys)
        Struct parentS = parentLifo.empty ? null : (Struct)parentLifo.last()

        // This is the closing record of the currently processed struct declaration
        if (parentS && (sMap.name == parentS.name || sMap.name.startsWith('---'))) {
            if (parentLifo.size() > 1) parentLifo.removeLast() // remove it from lifo
        }
        else {
            def s = new Struct(sMap)

            // conversion code comes here

            if (parentS) parentS.addStruct(s)
            parentLifo.add(s)
        }
    }

    private void setRange(Map map, Attribute attrOrField) {
        attrOrField.setRange(map.range)

        if (! (attrOrField.type == 'xs:integer' || attrOrField.type == 'xs:decimal')) {
            throw new InvalidDataException(
                    "Field/Attribute '${attrOrField.name}' uses invalid type '${attrOrField.type}'. 'range' must be integer or decimal")
        }
    }

    private void convertToField(Map<String, Object> record) {
        log.debug 'convertToField() - {}', record

        def fMap = record['xsd'].subMap(fieldKeys)
        def unitMap = (record['unit']) ? record['unit'] : [:]

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

        if (unitMap) {
            if (unitMap['unit.values'] ) {
                unitMap['values']  = unitMap['unit.values']
                unitMap.remove('unit.values')
            }
            if (unitMap['unit.default']) {
                unitMap['default'] = unitMap['unit.default']
                unitMap.remove('unit.default')
            }
            fixValues(unitMap)
            f.unit = new Unit(unitMap)
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
        def aMap = record['xsd'].subMap(attributeKeys)

        if (record['xsd'].documentation)
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
