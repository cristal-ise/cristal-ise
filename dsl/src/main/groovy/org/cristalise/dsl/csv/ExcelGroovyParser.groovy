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

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.ss.util.CellReference
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic @Slf4j
class ExcelGroovyParser implements TabularGroovyParser {

    Map<String, Object> options = [:]
    XSSFSheet sheet = null

    DataFormatter formatter = new DataFormatter()

    public ExcelGroovyParser(XSSFSheet s, Map opts) {
        options.headerRowCount = opts.headerRowCount != null ? opts.headerRowCount : 1
        sheet = s
    }

    @Override
    public void setHeaderRowCount(int rowCount) {
        options.headerRowCount = rowCount
    }

    @Override
    public void setHeader(List<List<String>> h) {
        options.header = h
    }

    /**
     * 
     * @param sheet
     * @param headerRowCount
     * @return
     */
    @Override
    public List<List<String>> getHeader() {
        int headerRowCount = options.headerRowCount as int

        List<List<String>> header = []

        for (Row row: sheet) {
            for (Cell cell : row) {
                def cellText = formatter.formatCellValue(cell)
                def currentRegion = getMergedRegionForCell(cell)

                if (currentRegion) {
                    // read the cell text from the first element of the region
                    cellText = formatter.formatCellValue(row.getCell(currentRegion.getFirstColumn()))
                }

                if (log.debugEnabled) {
                    def cellRef = new CellReference(row.getRowNum(), cell.getColumnIndex()).formatAsString()

                    log.debug "getHeader() - row:${cell.getRowIndex()} cell:$cellRef='$cellText'" +
                    ((currentRegion == null) ? '' : " - region:"+currentRegion.formatAsString())
                } 

                if (header[cell.columnIndex] == null) header[cell.columnIndex] = []

                header[cell.columnIndex] << cellText
            }

            // stop the loop after processing the header rows
            if (row.getRowNum() == headerRowCount - 1) break
        }

        return header
    }

    /**
     * Recursively process the list of names to build the nested Maps and Lists
     *
     * @param map
     * @param names List of String for one column e.g. 'contacts.address[0].purpose'
     * @param value Is a String as presented in the excel
     */
    private void convertNamesToMaps(Map map, List<String> names, String value) {
        def name = names.head()
        def namesTail = names.tail()

        log.debug "convertNamesToMaps() - {} names:{} value:'{}'", name, names, value

        if (namesTail) {
            if(!map[name]) { map[name] = [:] } //init Map

            convertNamesToMaps((Map)map[name], namesTail, value)
        }
        else {
            //Assign the value to the map
            map[name] = value

            log.debug "convertNamesToMaps() - map[{}] = {}", name, map[name]
        }
    }

    /**
     * 
     * @param sheet
     * @param headerRowCount
     * @param block
     */
    @Override
    public void eachRow(Closure block) {
        def header = getHeader()
        int headerRowCount = options.headerRowCount as int

        for (Row row: sheet) {
            // skip header section
            if (row.getRowNum() < headerRowCount) continue

            def rowMap = [:]

            log.debug "eachRow() - row #{} physicalNumberOfCells:{}", row.getRowNum(), row.getPhysicalNumberOfCells()

            for (Cell cell : row) {
                def cellRef = new CellReference(row.getRowNum(), cell.getColumnIndex()).formatAsString()
                def cellText = formatter.formatCellValue(cell)

                log.debug "eachRow() - row:{} cell:{}='{}'", cell.getRowIndex(), cellRef, cellText

                convertNamesToMaps(rowMap, header[cell.columnIndex], cellText)
            }

            //Issue #410: if the excel was edited with different editor (e.g. google spreadsheet), the row iterator will continue with empty rows
            if (row != null && row.getPhysicalNumberOfCells() != 0) {
                block(rowMap, row.rowNum - headerRowCount)
                rowMap.clear()
            }
            else {
                break;
            }
        }
    }

    /**
     * 
     * @param c
     * @return
     */
    private CellRangeAddress getMergedRegionForCell(Cell c) {
        for (CellRangeAddress mergedRegion : sheet.getMergedRegions()) {
           if (mergedRegion.isInRange(c)) {
              return mergedRegion;
           }
        }
        // Cell is not in any merged regions
        return null;
     }

    /**
     * NOTE: This method is not ready to be used, because it cannot return an Integer even if 
     * the number format defines an int. This is due to the limitation that cell.getNumericCellValue()
     * returns double.
     * 
     * @param cell
     * @return
     */
    private Object getCellValue(Cell cell) {
        switch (cell.getCellType()) {
            case CellType.STRING:
                //print '(STRING) '
                return cell.getStringCellValue()
            case CellType.NUMERIC:
                //print '(NUMERIC) '
                if (DateUtil.isCellDateFormatted(cell)) return cell.getDateCellValue()
                else                                    return cell.getNumericCellValue()
            case CellType.BOOLEAN:
                //print '(BOOLEAN) '
                return cell.getBooleanCellValue()
            case CellType.FORMULA:
                //print '(FORMULA) '
                switch (cell.getCachedFormulaResultType()) {
                    case CellType.NUMERIC: return cell.getNumericCellValue()
                    case CellType.STRING:  return cell.getStringCellValue()
                    case CellType.BOOLEAN: return cell.getBooleanCellValue()
                    case CellType.ERROR:   return cell.getErrorCellValue()
                    default:               return null
                }
            case CellType.ERROR:
                //print '(ERROR) '
                return cell.getErrorCellValue()
            case CellType.BLANK:
                //print '(BLANK) '
                return ''
            default:
                return null
        }
    }

    /**
     * Category method
     * 
     * @param self
     * @param sheetName
     * @param options
     * @return
     */
    public static List<List<String>> excelHeader(File self, String sheetName, Map options = [:]) {
        FileInputStream fileStream = new FileInputStream(self)
        XSSFWorkbook workbook = new XSSFWorkbook(fileStream);

        def header = excelHeader(workbook.getSheet(sheetName.trim()), options)

        workbook.close()
        fileStream.close()

        return header
    }

    /**
     * Category method
     * 
     * @param sheet
     * @param options
     * @return
     */
    public static List<List<String>> excelHeader(XSSFSheet sheet, Map options = [:]) {
        return new ExcelGroovyParser(sheet, options).getHeader()
    }

    /**
     * Category method
     * 
     * @param self
     * @param sheetName
     * @param options
     * @param block
     */
    public static void excelEachRow(File self, String sheetName, Map options = [:], Closure block) {
        FileInputStream fileStream = new FileInputStream(self)
        XSSFWorkbook workbook = new XSSFWorkbook(fileStream);

        XSSFSheet sheet = workbook.getSheet(sheetName.trim())
        excelEachRow(sheet, options, block)

        workbook.close()
        fileStream.close()
    }

    /**
     * Category method
     * 
     * @param filePath
     * @param sheetName
     * @param headerRowCount
     * @param block
     */
    public static void excelEachRow(XSSFSheet sheet, Map options = [:], Closure block) {
        def egp = new ExcelGroovyParser(sheet, options)
        egp.eachRow(block)
    }
}
