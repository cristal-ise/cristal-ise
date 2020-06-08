package org.cristalise.dsl.test.csv


import java.time.LocalDate

import org.cristalise.dsl.csv.CSVGroovyParser

import groovy.xml.MarkupBuilder
import spock.lang.Specification


class CSVGroovyParserTests extends Specification {

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

        def csv = 'f1,f2,f3,f4\n'+
                  'a ,b ,c,"d d "'

        expect: "CSVGroovyParser reads the line using a Closure"

        CSVGroovyParser.parse(csv) { record, i -> 
            println record
            assert record['f1'] == 'a'
            assert record.'f2' == 'b'
            assert record.f3 == 'c'
            assert record.f4 == 'd d'
        }
    }

    def "CSVGroovyParser parse file with header"() {
        given: "CSV file with header"

        expect: "CSVGroovyParser reads the line using a Closure"

        CSVGroovyParser.parse(new File('src/test/data/parsers/data.csv')) { record, i -> 
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
