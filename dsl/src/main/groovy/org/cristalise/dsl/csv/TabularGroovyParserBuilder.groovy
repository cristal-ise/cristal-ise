package org.cristalise.dsl.csv

import static org.cristalise.dsl.csv.TabularGroovyParser.ParserTypes.CSV
import static org.cristalise.dsl.csv.TabularGroovyParser.ParserTypes.EXCEL

import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.cristalise.dsl.csv.TabularGroovyParser.ParserTypes
import org.cristalise.kernel.common.InvalidDataException

import groovy.transform.CompileStatic

@CompileStatic
class TabularGroovyParserBuilder {
    
    private ParserTypes type = null
    private File file = null
    private String sheetName = null

    private List<List<String>> header = null
    private Integer headerRowCount = null

    public TabularGroovyParserBuilder() {
    }

    public TabularGroovyParserBuilder csvParser(File f) {
        type = CSV
        file = f
        return this
    }

    public TabularGroovyParserBuilder excelParser(File f, String name) {
        type = EXCEL
        file = f
        sheetName = name
        return this
    }

    public TabularGroovyParserBuilder withHeaderRowCount(int count) {
        headerRowCount = count
        return this
    }

    public TabularGroovyParserBuilder withHeader(List<List<String>> h) {
        header = h
        return this
    }

    private TabularGroovyParser initCsvParser(Map options) {
        Reader reader = new FileReader(file)

        return new CSVGroovyParser(reader, options)
    }

    private TabularGroovyParser initExcelParser(Map options) {
        InputStream is= new FileInputStream(file)
        XSSFWorkbook workbook = new XSSFWorkbook(is);
        XSSFSheet sheet = workbook.getSheet(sheetName.trim())

        return new ExcelGroovyParser(sheet, options)
    }

    public TabularGroovyParser build() {
        if (type == null) {
            throw new InvalidDataException()
        }

        def options = [:]

        if (headerRowCount != null) options['headerRowCount'] = headerRowCount
        if (header != null)         options['header']         = header
 
        switch (type) {
            case CSV:   return initCsvParser(options)
            case EXCEL: return initExcelParser(options)
            default: 
                throw new InvalidDataException()
        }
    }
}
