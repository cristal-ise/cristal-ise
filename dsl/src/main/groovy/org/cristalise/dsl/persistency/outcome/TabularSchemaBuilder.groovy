package org.cristalise.dsl.persistency.outcome

import org.apache.poi.xssf.usermodel.XSSFSheet
import org.cristalise.dsl.csv.CSVGroovyParser
import org.cristalise.dsl.csv.ExcelGroovyParser
import org.cristalise.kernel.common.InvalidDataException

import groovy.util.logging.Slf4j

/**
 * 
 *
 */
@Slf4j
class TabularSchemaBuilder {
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

    Struct build(String csv) {
        CSVGroovyParser.csvEachRow(csv, headerRows:2) { Map<String, Object> record, int i ->
            switch (record['xsd']['class']) {
                case 'struct'   : convertToStruct(record); break;
                case 'field'    : convertToField(record); break;
                case 'attribute': convertToAttribute(record); break;
                default:
                    throw new InvalidDataException('Uncovered class value:' + record['xsd']['class'])
            }
        }
    }


    /**
     * Convert comma separated string to list before calling map constructor
     */
    private void fixListValues(Map map) {
        def regex = '\\s*,\\s*'
        if (map.values)       map.values       = map.values      .trim().split(regex)
        if (map.updateFields) map.updateFields = map.updateFields.trim().split(regex)
    }

    private void convertToStruct(Map<String, Object> record) {
        log.debug 'convertToStruct() - {}', record
        
        // if previous row was a field remove it from lifo
        if (parentLifo.size() > 1 && parentLifo.last() instanceof Field) parentLifo.removeLast()

        def sMap = record['xsd'].subMap(Struct.keys)

        Struct parentS = parentLifo.empty ? null : (Struct)parentLifo.last()

        // This is the closing record of the currently processed struct declaration
        if (parentS && (sMap.name == parentS.name || sMap.name.startsWith('---'))) {
            if (parentLifo.size() > 1) parentLifo.removeLast() // remove it from lifo
        }
        else {
            Map dynamicFormsMap = (record['dynamicForms']) ?: [:]
            Map additionalMap   = (record['additional'])   ?: [:]

            def s = new Struct(sMap)

            if (dynamicFormsMap && dynamicFormsMap.find { it.value }) {
                fixListValues(dynamicFormsMap)
                s.dynamicForms = new DynamicForms(dynamicFormsMap)
            }

            if (additionalMap && additionalMap.find { it.value }) {
                if (!s.dynamicForms) s.dynamicForms = new DynamicForms()
                s.dynamicForms.additional = new Additional(additionalMap)
            }
    
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

        Map fMap = record['xsd'].subMap(Field.keys)

        Map unitMap         = (record['unit'])         ?: [:]
        Map lovMap          = (record['listOfValues']) ?: [:]
        Map referenceMap    = (record['reference'])    ?: [:]
        Map dynamicFormsMap = (record['dynamicForms']) ?: [:]
        Map warningMap      = (record['warning'])      ?: [:]
        Map additionalMap   = (record['additional'])   ?: [:]

        fixListValues(fMap)

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
            fixListValues(unitMap)
            f.unit = new Unit(unitMap)
        }

        if (lovMap) {
            fixListValues(lovMap)
            f.listOfValues = new ListOfValues(lovMap)
        }

        if (referenceMap) {
            f.reference = new Reference(referenceMap)
        }

        if (dynamicFormsMap && dynamicFormsMap.find { it.value }) {
            fixListValues(dynamicFormsMap)
            f.dynamicForms = new DynamicForms(dynamicFormsMap)
        }

        if (warningMap && warningMap.find { it.value }) {
            if (!f.dynamicForms) f.dynamicForms = new DynamicForms()
            f.dynamicForms.warning = new Warning(warningMap)
        }

        if (additionalMap && additionalMap.find { it.value }) {
            if (!f.dynamicForms) f.dynamicForms = new DynamicForms()
            f.dynamicForms.additional = new Additional(additionalMap)
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
        def aMap = record['xsd'].subMap(Attribute.keys)

        if (record['xsd'].documentation)
            throw new InvalidDataException("Attribute '${aMap.name}' cannot have a documentation")

        fixListValues(aMap)

        def a = new Attribute(aMap)

        if (aMap.multiplicity) a.setMultiplicity(aMap.multiplicity)
        if (aMap.range) setRange(aMap, a)

        def prev = parentLifo.last()

        if      (parentLifo.last() instanceof Struct) ((Struct)prev).attributes.add(a)
        else if (parentLifo.last() instanceof Field)  ((Field)prev).attributes.add(a)
    }
}
