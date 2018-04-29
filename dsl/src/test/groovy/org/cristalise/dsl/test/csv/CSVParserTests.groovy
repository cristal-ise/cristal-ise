package org.cristalise.dsl.test.csv


import java.nio.charset.Charset

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.cristalise.dsl.csv.CSVGroovyParser

import spock.lang.Specification


class CSVParserTests extends Specification {

    def "CSVParser without header"() {
        setup:
        def csv = 'a,b,c'
        
        when:
        def parser = CSVParser.parse(csv, CSVFormat.DEFAULT)

        then:
        parser != null
        parser.getHeaderMap() == null
        parser.getRecordNumber() == 0
        def record = parser.records[0]
        parser.getRecordNumber() == 1
        record.toMap().isEmpty() == true
        record[0] == 'a'
        record[1] == 'b'
        record[2] == 'c'
    }


    def "CSVParser with header"() {
        given:
        def csv = '''f1,f2,f3,f4
                     a ,b ,c ,"d d"'''

        def format = CSVFormat.RFC4180.withIgnoreSurroundingSpaces().withHeader()
        
        when:
        def parser = CSVParser.parse(csv, format)

        then: "CSVParser "
        parser != null
        parser.getHeaderMap() != null
        parser.getRecordNumber() == 0
        def record = parser.records[0]
        parser.getRecordNumber() == 1
        def map = record.toMap()
        map['f1'] == 'a'
        map.f2 == 'b'
        map.f3 == 'c'
        map.f4 == 'd d'
    }


    def "CSVGroovyParser without header"() {
        setup:
        def csv = 'a,b,c'

        expect:
        CSVGroovyParser.parse(csv) { record, i -> 
            assert record[0] == 'a'
            assert record[1] == 'b'
            assert record[2] == 'c'
        }
    }


    def "CSVGroovyParser with header"() {
        given: "CSV string with header"

        def csv = '''f1,f2,f3,f4
                     a ,b ,c ,"d d"'''

        def format = CSVFormat.RFC4180.withIgnoreSurroundingSpaces().withHeader()

        expect: "CSVGroovyParser reads the line using a Closure"

        CSVGroovyParser.parse(csv, format) { record, i -> 
            assert record['f1'] == 'a'
            assert record.'f2' == 'b'
            assert record.f3 == 'c'
            assert record.f4 == 'd d'
        }
    }

    def "CSVGroovyParser parse file with header"() {
        given: "CSV string with header"

        expect: "CSVGroovyParser reads the line using a Closure"

        CSVGroovyParser.parse(new File('src/test/data/data.csv')) { record, i -> 
            if (i == 0) {
                assert record['f1'] == 'a'
                assert record.'f2' == 'b'
                assert record.f3 == 'c'
                assert record.f4 == 'd d'
            }
            else if (i == 1) {
                assert record['f1'] == '1'
                assert record.'f2' == '2'
                assert record.f3 == '3'
                assert record.f4 == '4 4'
            }
        }
    }
}
