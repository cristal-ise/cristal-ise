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

import java.time.LocalDate
import java.time.format.DateTimeParseException

import org.apache.commons.lang3.math.NumberUtils
import org.apache.commons.lang3.time.DateUtils

import com.opencsv.CSVParser
import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReader
import com.opencsv.CSVReaderBuilder

import groovy.transform.CompileStatic;
import groovy.util.logging.Slf4j

@Slf4j @CompileStatic
class CSVGroovyParser implements TabularGroovyParser {

    Map<String, Object> options = [:]
    CSVReader reader = null

    public CSVGroovyParser(Reader ioReader, Map opts) {
        setDefaultOptions(opts)
        reader = getCSVReader(ioReader)
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
     * @param r
     * @return
     */
    private CSVReader getCSVReader(Reader ioReader) {
        def parser = new CSVParserBuilder()
            .withSeparator(options.separatorChar as char)
            .withQuoteChar(options.quoteChar as char)
            .withEscapeChar(options.escapeChar as char)
            .withStrictQuotes(options.strictQuotes as boolean)
            .build()

        return new CSVReaderBuilder(ioReader)
            .withCSVParser(parser)
            .withSkipLines(options.skipRows as int)
            .build()
    }

    /**
     * 
     * @param opts
     */
    private void setDefaultOptions(Map opts) {
        options.headerRowCount = opts.headerRowCount != null ? opts.headerRowCount : 1
        options.skipLeftCols   = opts.skipLeftCols   != null ? opts.skipLeftCols   : 0
        options.skipRightCols  = opts.skipRightCols  != null ? opts.skipRightCols  : 0
        options.trimHeader     = opts.trimHeader     != null ? opts.trimHeader     : true
        options.trimData       = opts.trimData       != null ? opts.trimData       : false
        options.strictQuotes   = opts.strictQuotes   != null ? opts.strictQuotes   : CSVParser.DEFAULT_STRICT_QUOTES
        options.skipRows       = opts.skipRows       != null ? opts.skipRows       : CSVReader.DEFAULT_SKIP_LINES
        options.useStringOnly  = opts.useStringOnly  != null ? opts.useStringOnly  : false
        options.separatorChar  = opts.separatorChar          ? opts.separatorChar  : CSVParser.DEFAULT_SEPARATOR
        options.quoteChar      = opts.quoteChar              ? opts.quoteChar      : CSVParser.DEFAULT_QUOTE_CHARACTER
        options.escapeChar     = opts.escapeChar             ? opts.escapeChar     : CSVParser.DEFAULT_ESCAPE_CHARACTER
        options.dateFormatter  = opts.dateFormatter          ? opts.dateFormatter  : ''
        options.header         = opts.header                 ? opts.header         : []
    }

    /**
     *
     * @param reader
     * @param rowCount
     * @param skipLeftCols
     * @param skipRightCols
     * @param trim
     * @return
     */
    @Override
    public List<List<String>> getHeader() {
        int rowCount = options.headerRowCount as int
        int skipLeftCols = options.skipLeftCols as int
        int skipRightCols = options.skipRightCols as int
        boolean trim = (boolean)options.trimData

        assert rowCount, "row count for header must be grater than zero"

        log.debug "getHeader() - rowCount: $rowCount, skipLeftCols: $skipLeftCols, skipRightCols: $skipRightCols"

        List<String[]> headerRows = []
        Integer size = null

        //read header lines into a list of arrays, check size of each rows
        for (i in 0..rowCount-1) {
            headerRows[i] = reader.readNext();

            assert headerRows[i], "$i. row in header is null or zero size"

            log.debug "getHeader() - headerRows[$i] size: ${headerRows[i].size()}"

            //compare current size with the previous
            if (size) {
                assert headerRows[i].size() == size, "$i. header row size is not equal with the previous. All header rows must have the same size"
            }
            size = headerRows[i].size()
        }

        List<String> currentNames = []
        List<List<String>> header = []

        //construct the path of each header, make sure to skip columns if needed
        for (int i in skipLeftCols..size-(1+skipRightCols)) {
            List<String> aList = []
            for (j in 0..rowCount-1) {

                def name = headerRows[j][i]

                //if not null/empty take this string otherwise use the buffer of currentNames
                if (name) { currentNames[j] = trim && name instanceof String ? name.trim() : name }

                name = currentNames[j]

                //if not null/empty append it to the list
                if (name) { aList.add(name) }
            }
            log.info "getHeader() - header[${i-skipLeftCols}] = $aList"
            header.add(aList)
        }
        return header
    }

    /**
     * Cast the string value to a real type
     * @param value in string format
     * @return the value converted to its real type
     */
    private Object getValue(String value) {
        boolean trim = (boolean)options.trimData

        if (!value) {
            //value is null or empty string
            return null
        }
        else if ((boolean)options.useStringOnly) {
            return trim ? value.trim() : value
        }
        else if (value == 'null') {
            return null
        }
        else if (NumberUtils.isCreatable(value)) {
            if (value.isInteger()) {
                return value.trim() as Integer
            }
            else if (value.isBigInteger()) {
                return value.trim() as BigInteger
            }
            else if (value.isBigDecimal()) {
                return value.trim() as BigDecimal
            }
            else {
                return value.trim() as Number
            }
        }
        else {
            try {
                if((String)options.dateFormatter) {
                    return LocalDate.parse(value.trim(), (String)options.dateFormatter)
                }
                else {
                    return LocalDate.parse(value.trim())
                }
            }
            catch (DateTimeParseException e) {
                return trim ? value.trim() : value
            }
        }
    }

    /**
     * Recursively process the list of names to build the nested Maps and Lists
     *
     * @param map
     * @param names List of String for one column e.g. 'contacts.address[0].purpose'
     * @param value Is a String as returned by OpensCSV
     * @return
     */
    private void convertNamesToMaps(Map<String, Object> map, List<String> names, String value) {
        String name = names.head()
        List<String> namesTail = names.tail()
        int index = -1

        //Dealing with repeating section, so handle it as List of Maps
        if(name.contains('[') && name.endsWith(']')) {
            int i = name.indexOf('[')
            index = name.substring(i+1,name.size()-1) as int
            name = name.substring(0, i)
        }

        log.debug "$name index:$index names:$names value:'$value'"

        if(namesTail) {
            //Names are not fully converted to maps and lists yet
            if(index == -1) {
                if(!map[name]) { map[name] = [:] } //init Map

                convertNamesToMaps((Map)map[name], namesTail, value)
            }
            else {
                //Dealing with repeating section, so handle it as List of Maps
                if(!map[name]) { map[name] = [] } //init List
                if(!((List)map[name])[index]) { ((List)map[name])[index] = [:] } //init Map in the List

                convertNamesToMaps((Map)((List)map[name])[index], namesTail, value)
            }
        }
        else {
            map[name] = getValue(value)
            log.debug "map[$name] = ${map[name]}"
        }
    }

    /**
     *
     * @param reader
     * @param options
     * @param cl
     */
    @Override
    public void eachRow(Closure cl) {
        String[] nextLine;

        int headerRowCount        = options.headerRowCount as int
        int skipLeftCols          = options.skipLeftCols as int
        int skipRightCols         = options.skipRightCols as int
        List<List<String>> header = options.header as List

        int rowIndex = 0

        //CSV has no header
        if (!headerRowCount && !header) {
            log.warn "No header was specified so reverting to original openCsv behaviour"
            while ((nextLine = reader.readNext()) != null) {
                cl(nextLine, rowIndex++)
            }
        }
        else {
            if (!header) {
                header = getHeader()
            }
            else {
                log.debug "external header: $header"
            }

            assert header, "no header is availalle"


            while ((nextLine = reader.readNext()) != null) {
                assert header.size() == nextLine.size() - (skipLeftCols + skipRightCols), "Header size must be equal with the size of data line"

                def map = [:]

                //header is a List of Lists
                header.eachWithIndex { List<String> names, int i ->
                    def value = nextLine[i+skipLeftCols]
                    if (value) convertNamesToMaps(map, names, value)
                }

                cl(map, rowIndex++)
            }
        }
    }

    /**
     * Category method
     *
     * @param self
     * @param options
     * @return
     */
    public static List<List<String>> csvHeader(File self, Map options = [:]) {
        return new CSVGroovyParser(new FileReader(self), options).getHeader()
    }

    /**
     * Category method
     * 
     * @param self
     * @param options
     * @return
     */
    public static List<List<String>> csvHeader(String self, Map options = [:]) {
        return new CSVGroovyParser(new StringReader(self), options).getHeader()
    }

    /**
     * Category method
     *
     * @param self
     * @param options
     * @param cl
     */
    public static void csvEachRow(File self, Map options = [:], Closure cl) {
        new CSVGroovyParser(new FileReader(self), options).eachRow(cl)
    }

    /**
     * Category method
     *
     * @param self
     * @param options
     * @param cl
     */
    public static void csvEachRow(String self, Map options = [:], Closure cl) {
        new CSVGroovyParser(new StringReader(self), options).eachRow(cl)
    }

    /**
     * @deprecated use {@link #csvEachRow(File, Closure)} to avoid name collision with 
     * ExcelGroovyParser when it is used as groovy Category or Extension.
     * @param file
     * @param cl
     * @throws IOException
     */
    @Deprecated
    public static void parse(final File file, Closure cl) throws IOException {
        csvEachRow(file, [headerRowCount: 1, trimData: true, useStringOnly: true], cl)
    }

    /**
     * @deprecated use {@link #csvEachRow(String, Closure)} to avoid name collision with 
     * ExcelGroovyParser when it is used as groovy Category or Extension.
     * @param string
     * @param cl
     * @throws IOException
     */
    @Deprecated
    public static void parse(final String string, Closure cl) throws IOException {
        csvEachRow(string, [headerRowCount: 1, trimData: true, useStringOnly: true], cl)
    }
}
