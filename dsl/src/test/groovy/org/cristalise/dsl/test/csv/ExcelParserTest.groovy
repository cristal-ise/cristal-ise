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

import static org.assertj.core.api.Assertions.assertThat

import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.cristalise.dsl.csv.ExcelGroovyParser
import org.junit.Test

import groovy.transform.CompileStatic

@CompileStatic
class ExcelParserTest {

    @Test
    public void test() {
        def excpected = [
            [f1:'a ', f2:'b ', f3:'c ', f4:'d d'],
            [f1:'1',  f2:'2',  f3:'3',  f4:'4 4']
        ]

        ExcelGroovyParser.excelEachRow(new File ('src/test/data/parsers/data.xlsx'), 'dataSheet') { Map<String, Object> record, int i ->
            assertThat(record).containsAllEntriesOf(excpected[i])
        }
    }


    @Test
    public void twoLineHeaderOnlyTest() {
        def expected = [['h0','f0'], ['h1', 'f1'], ['h1', 'f2'], ['h2', 'f3'], ['h2', 'f4']]

        def header = ExcelGroovyParser.excelHeader(new File('src/test/data/parsers/data.xlsx'), 'TwoLineHeader', [headerRowCount: 2])
        assertThat(header).isEqualTo(expected)
    }

    @Test
    public void twoLineHeaderTest() {
        def excpected = [
            [h0:[f0: 'class0'], h1:[f1:'a ', f2:'b '], h2:[f3:'c ', f4:'d d']],
            [h0:[f0: 'class1'], h1:[f1:'1',  f2:'2'],  h2:[f3:'3',  f4:'4 4']],
            [h1:[f1:'11',  f2:'22'],  h2:[f3:'33',  f4:'444']],
        ]

        ExcelGroovyParser.excelEachRow(new File('src/test/data/parsers/data.xlsx'), 'TwoLineHeader', [headerRowCount: 2]) { Map record, int i ->
            assertThat(record).containsAllEntriesOf(excpected[i])
        }
    }
}
