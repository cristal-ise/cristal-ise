package org.cristalise.dsl.test.excel

import static org.assertj.core.api.Assertions.assertThat

import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.cristalise.dsl.excel.ExcelGroovyParser
import org.junit.Test

//@CompileStatic
class ExcelParserTest {

    @Test
    public void test() {
        def excpected = [
            [f1:'a ', f2:'b ', f3:'c ', f4:'d d'],
            [f1:'1',  f2:'2',  f3:'3',  f4:'4 4']
        ]

        ExcelGroovyParser.excelEachRow('src/test/data/parsers/data.xlsx', 'dataSheet', 1) { Map<String, Object> record, int i ->
            assertThat(record).containsAllEntriesOf(excpected[i])
        }
    }


    @Test
    public void twoLineHeaderOnlyTest() {
        def expected = [['h0','f0'], ['h1', 'f1'], ['h1', 'f2'], ['h2', 'f3'], ['h2', 'f4']]
        
        FileInputStream fileStream = new FileInputStream(new File('src/test/data/parsers/data.xlsx'))
        XSSFWorkbook workbook = new XSSFWorkbook(fileStream);
        XSSFSheet sheet = workbook.getSheet('TwoLineHeader')

        def header = ExcelGroovyParser.excelHeader(sheet , 2)
        println header
        assertThat(header).isEqualTo(expected)
    }

    @Test
    public void twoLineHeaderTest() {
        def excpected = [
            [h0:[f0: 'class0'], h1:[f1:'a ', f2:'b '], h2:[f3:'c ', f4:'d d']],
            [h0:[f0: 'class1'], h1:[f1:'1',  f2:'2'],  h2:[f3:'3',  f4:'4 4']],
        ]

        ExcelGroovyParser.excelEachRow('src/test/data/parsers/data.xlsx', 'TwoLineHeader', 2) { Map record, int i ->
            println record
            assertThat(record).containsAllEntriesOf(excpected[i])
        }
    }
}
