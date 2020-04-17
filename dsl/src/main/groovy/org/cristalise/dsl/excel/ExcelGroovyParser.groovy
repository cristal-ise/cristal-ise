package org.cristalise.dsl.excel

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.Header
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.util.CellReference
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import groovy.transform.CompileStatic

@CompileStatic
class ExcelGroovyParser {
    
    public static void eachRow(String filePath, String sheetName, List<String> headerRow = null, Boolean skipFirstRow = null, Closure block) {
        FileInputStream fileStream = new FileInputStream(new File(filePath))
        XSSFWorkbook workbook = new XSSFWorkbook(fileStream);

        eachRow(workbook, sheetName, headerRow,skipFirstRow, block)

        workbook.close()
        fileStream.close()
    }

    public static void eachRow(XSSFWorkbook workbook, String sheetName, List<String> headerRow = null, Boolean skipFirstRow = null, Closure block) {
        XSSFSheet sheet = workbook.getSheet(sheetName.trim())
        eachRow(sheet, headerRow, skipFirstRow, block)
    }

    public static void eachRow(XSSFSheet sheet, List<String> headerRow = null, Boolean skipFirstRow = null, Closure block) {
        DataFormatter formatter = new DataFormatter()

        List<String> keys = headerRow ?: [] as List<String>
        //TODO: use Object instead of String for value - see getCellValue()
        Map<String, String> rowMap = [:]
        def generateKeys = !keys

        for (Row row: sheet) {
            for (Cell cell : row) {
                def cellRef = new CellReference(row.getRowNum(), cell.getColumnIndex()).formatAsString()
                def cellText = formatter.formatCellValue(cell)

                //print "$cellRef='$cellText' "

                if (cell.getRowIndex() == 0) {
                    if (generateKeys)       keys << cellText
                    else if (!skipFirstRow) rowMap[keys[cell.getColumnIndex()]] = cellText
                }
                else {
                    rowMap[keys[cell.getColumnIndex()]] = cellText
                }
            }
            //println ""

            if ((!skipFirstRow && !generateKeys) || row.getRowNum() != 0) block(rowMap)

            rowMap.clear()
        }
    }

    /**
     * NOTE: This method is not ready, because it cannot return an Integer even if the number format defines an int
     */
    public static Object getCellValue(Cell cell, String cellText) {
        switch (cell.getCellTypeEnum()) {
            case CellType.STRING:
                //print '(STRING) '
                return cell.getStringCellValue()
                break
            case CellType.NUMERIC:
                //print '(NUMERIC) '
                if (DateUtil.isCellDateFormatted(cell)) return cell.getDateCellValue()
                else                                    return cell.getNumericCellValue() 
                break;
            case CellType.BOOLEAN:
                //print '(BOOLEAN) '
                return cell.getBooleanCellValue()
                break;
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
                break
            case CellType.BLANK:
                //print '(BLANK) '
                return ''
                break
            default:
                return null
        }
    }
}
