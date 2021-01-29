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
package org.cristalise.dsl.csv

import static org.cristalise.dsl.csv.TabularGroovyParser.ParserTypes.CSV
import static org.cristalise.dsl.csv.TabularGroovyParser.ParserTypes.EXCEL

import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.cristalise.dsl.csv.TabularGroovyParser.ParserTypes
import org.cristalise.kernel.common.InvalidDataException

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic @Slf4j
class TabularGroovyParserBuilder {
    
    private ParserTypes type = null
    private File file = null
    private String sheetName = null

    private List<List<String>> header = null
    private Integer headerRowCount = null

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
        
        try {
            XSSFSheet sheet = workbook.getSheet(sheetName.trim())
            return new ExcelGroovyParser(workbook, sheet, options)
        }
        catch (Exception e) {
            log.error("initExcelParser()", e)
        }
        
        return null
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

    public static TabularGroovyParser build(File file, String sheet, int headerRowCount) {
        def fileName = file.name
        def type = fileName.substring(fileName.lastIndexOf('.')+1).toUpperCase()

        switch(type) {
            case 'XLSX': return new TabularGroovyParserBuilder().excelParser(file, sheet).withHeaderRowCount(headerRowCount).build()
            case 'CSV':  return new TabularGroovyParserBuilder().csvParser(file).withHeaderRowCount(headerRowCount).build()
            default: throw new UnsupportedOperationException("Unsupported file type:$type name:$fileName")
        }
    }
}
