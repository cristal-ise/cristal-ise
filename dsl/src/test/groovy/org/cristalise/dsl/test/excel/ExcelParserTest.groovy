package org.cristalise.dsl.test.excel

import static org.assertj.core.api.Assertions.assertThat
import org.cristalise.dsl.excel.ExcelGroovyParser
import org.junit.Test
import groovy.transform.CompileStatic

//@CompileStatic
class ExcelParserTest {

    @Test
    public void test() {
        List<Map<String, Object>> excpected = [
            [f1:'a ', f2:'b ', f3:'c ', f4:'d d'],
            [f1:'1',  f2:'2',  f3:'3',  f4:'4 4']
        ]

        int i = 0

        ExcelGroovyParser.eachRow('src/test/data/data.xlsx', 'dataSheet') { Map<String, Object> record ->
            assertThat(record).containsAllEntriesOf(excpected[i++])
        }
    }

    @Test
    public void test2() {
        def excpected = [
            [H1:'f1', H2:'f2', H3:'f3', H4:'f4'],
            [H1:'a ', H2:'b ', H3:'c ', H4:'d d'],
            [H1:'1',  H2:'2',  H3:'3',  H4:'4 4']
        ]

        int i = 0

        ExcelGroovyParser.eachRow('src/test/data/data.xlsx', 'dataSheet', ['H1','H2','H3','H4']) { Map record ->
            assertThat(record).containsAllEntriesOf(excpected[i++])
        }
    }

    @Test
    public void test3() {
        def excpected = [
            [H1:'a ', H2:'b ', H3:'c ', H4:'d d'],
            [H1:'1',  H2:'2',  H3:'3',  H4:'4 4']
        ]

        int i = 0

        ExcelGroovyParser.eachRow('src/test/data/data.xlsx', 'dataSheet', ['H1','H2','H3','H4'], true) { Map record ->
            assertThat(record).containsAllEntriesOf(excpected[i++])
        }
    }
}
