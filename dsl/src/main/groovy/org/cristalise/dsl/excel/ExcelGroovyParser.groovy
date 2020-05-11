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
package org.cristalise.dsl.excel

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
class ExcelGroovyParser {
    
    /**
     * 
     * @param filePath
     * @param sheetName
     * @param headerRowCount
     * @param block
     */
    public static void excelEachRow(String filePath, String sheetName, int headerRowCount, Closure block) {
        FileInputStream fileStream = new FileInputStream(new File(filePath))
        XSSFWorkbook workbook = new XSSFWorkbook(fileStream);

        excelEachRow(workbook, sheetName, headerRowCount, block)

        workbook.close()
        fileStream.close()
    }

    /**
     * 
     * @param workbook
     * @param sheetName
     * @param headerRowCount
     * @param block
     */
    public static void excelEachRow(XSSFWorkbook workbook, String sheetName, int headerRowCount, Closure block) {
        XSSFSheet sheet = workbook.getSheet(sheetName.trim())
        eachRow(sheet, headerRowCount, block)
    }

    /**
     * 
     * @param sheet
     * @param headerRowCount
     * @return
     */
    public static List<List<String>> excelHeader(XSSFSheet sheet, int headerRowCount) {
        List<List<String>> header = []
        DataFormatter formatter = new DataFormatter()

        for (Row row: sheet) {
            for (Cell cell : row) {
                def cellText = formatter.formatCellValue(cell)
                def currentRegion = getMergedRegionForCell(sheet, cell)

                if (currentRegion) {
                    // read the cell text from the first element of the region
                    cellText = formatter.formatCellValue(row.getCell(currentRegion.getFirstColumn()))
                }

                if (log.debugEnabled) {
                    def cellRef = new CellReference(row.getRowNum(), cell.getColumnIndex()).formatAsString()

                    log.debug "excelHeader() - row:${cell.getRowIndex()} cell:$cellRef='$cellText'" +
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
    public static void convertNamesToMaps(Map map, List<String> names, String value) {
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
    public static void eachRow(XSSFSheet sheet, int headerRowCount, Closure block) {
        DataFormatter formatter = new DataFormatter()

        def header = excelHeader(sheet, headerRowCount)

        for (Row row: sheet) {
            // skip header section
            if (row.getRowNum() < headerRowCount) continue

            def rowMap = [:]

            for (Cell cell : row) {
                def cellRef = new CellReference(row.getRowNum(), cell.getColumnIndex()).formatAsString()
                def cellText = formatter.formatCellValue(cell)

                log.debug "eachRow() - row:{} cell:{}='{}'", cell.getRowIndex(), cellRef, cellText

                convertNamesToMaps(rowMap, header[cell.columnIndex], cellText)
            }

            block(rowMap, row.rowNum - headerRowCount)

            rowMap.clear()
        }
    }

    /**
     * 
     * @param s
     * @param c
     * @return
     */
    private static CellRangeAddress getMergedRegionForCell(Sheet s, Cell c) {
        for (CellRangeAddress mergedRegion : s.getMergedRegions()) {
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
    public static Object getCellValue(Cell cell) {
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
}
