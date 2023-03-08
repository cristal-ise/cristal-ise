/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
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
