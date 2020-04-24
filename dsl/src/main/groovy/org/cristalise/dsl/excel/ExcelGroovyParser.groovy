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

import groovy.transform.CompileDynamic
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
    public static void eachRow(String filePath, String sheetName, int headerRowCount, Closure block) {
        FileInputStream fileStream = new FileInputStream(new File(filePath))
        XSSFWorkbook workbook = new XSSFWorkbook(fileStream);

        eachRow(workbook, sheetName, headerRowCount, block)

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
    public static void eachRow(XSSFWorkbook workbook, String sheetName, int headerRowCount, Closure block) {
        XSSFSheet sheet = workbook.getSheet(sheetName.trim())
        eachRow(sheet, headerRowCount, block)
    }

    /**
     * 
     * @param sheet
     * @param headerRowCount
     * @return
     */
    public static List<List<String>> getHeader(XSSFSheet sheet, int headerRowCount) {
        List<List<String>> header = []

        DataFormatter formatter = new DataFormatter()
        for (Row row: sheet) {
            for (Cell cell : row) {
                def cellRef = new CellReference(row.getRowNum(), cell.getColumnIndex()).formatAsString()
                def cellText = formatter.formatCellValue(cell)

                def currentRegion = getMergedRegionForCell(sheet, cell)

                log.debug "getHeader() - row:${cell.getRowIndex()} cell:$cellRef='$cellText'" +  
                          ((currentRegion == null) ? '' : " - region:"+currentRegion.formatAsString())

                if (header[cell.columnIndex] == null) header[cell.columnIndex] = []

                if (currentRegion) {
                    // read the cell text from the first element of the region
                    header[cell.columnIndex] << formatter.formatCellValue(row.getCell(currentRegion.getFirstColumn()))
                }
                else {
                    header[cell.columnIndex] << cellText
                }
            }

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
    @CompileDynamic
    public static void convertNamesToMaps(Map map, List<String> names, String value) {
        def name = names.head()
        def namesTail = names.tail()
        int index = -1

        log.debug "convertNamesToMaps() - {} index:{} names:{} value:'{}'", name, index, names, value

        if (namesTail) {
            //Names are not fully converted to maps and lists yet
            if (index == -1) {
                if(!map[name]) { map[name] = [:] } //init Map

                convertNamesToMaps(map[name], namesTail, value)
            }
            else {
                //Dealing with repeating section, so handle it as List of Maps
                if (!map[name]) { map[name] = [] } //init List
                if (!map[name][index]) { map[name][index] = [:] } //init Map in the List

                convertNamesToMaps(map[name][index], namesTail, value)
            }
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

        def header = getHeader(sheet, headerRowCount)

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
        // Not in any
        return null;
     }

    /**
     * NOTE: This method is not ready to be used, because it cannot return an Integer 
     * even if the number format defines an int. 
     * 
     * @param cell
     * @return
     */
    public static Object getCellValue(Cell cell) {
        switch (cell.getCellTypeEnum()) {
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
                switch (cell.getCachedFormulaResultTypeEnum()) {
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
