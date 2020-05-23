package org.cristalise.dsl.csv;

import java.util.List;

import groovy.lang.Closure;
import groovy.transform.CompileStatic

/**
 * Defines the common interface for parsers reading tabular data (i.e. csv, excel)
 */
@CompileStatic
public interface TabularGroovyParser {
    
    public enum ParserTypes {EXCEL, CSV, GSHEET} //GSHEET is not supported yet

    /**
     * Sets the number of rows to be used as header, default is 1
     * @param rowCount number of rows to be used as header (default: 1)
     */
    public void setHeaderRowCount(int rowCount)

    /**
     * Reads the header information of the file. The parser can handle headers with multiple rows. 
     * In that case the header has to be in a specific format ...
     * 
     * @return parsed header rows converted to 2 dimensional array of strings (i.e. Number of columns x Header row count)
     */
    public List<List<String>> getHeader()

    /**
     * Sets the header to be used for files which does not contain header lines
     * @param header to be used
     */
    public void setHeader(List<List<String>> header)

    /**
     * 
     * @param codeBlock
     */
    public void eachRow(Closure codeBlock)
}
