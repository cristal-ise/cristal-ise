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

import com.opencsv.CSVParser
import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReader
import com.opencsv.CSVReaderBuilder

import groovy.transform.CompileStatic;
import groovy.util.logging.Slf4j

@Slf4j @CompileStatic
class CSVGroovyParser {
    
    Map options = [:]

    /**
     *
     * @param reader
     * @param rowCount
     * @param skipLeftCols
     * @param skipRightCols
     * @param trim
     * @return
     */
    public static List getCsvHeader(CSVReader reader, int rowCount, int skipLeftCols, int skipRightCols, boolean trim) {
        assert rowCount, "row count for header must be grater than zero"

        log.debug "rowCount: $rowCount, skipLeftCols: $skipLeftCols, skipRightCols: $skipRightCols"

        List<String[]> headerRows = []
        Integer size = null

        //read header lines into a list of arrays, check size of each rows
        for (i in 0..rowCount-1) {
            headerRows[i] = reader.readNext();

            assert headerRows[i], "$i. row in header is null or zero size"

            log.debug "headerRows[$i] size: ${headerRows[i].size()}"

            //compare current size with the previous
            if (size) {
                assert headerRows[i].size() == size, "$i. header row size is not equal with the previous. All header rows must have the same size"
            }
            size = headerRows[i].size()
        }

        def currentNames = []
        def header = []

        //construct the path of each header, make sure to skip columns if needed
        for (int i in skipLeftCols..size-(1+skipRightCols)) {
            List aList = []
            for (j in 0..rowCount-1) {

                def name = headerRows[j][i]

                //if not null/empty take this string otherwise use the buffer of currentNames
                if (name) { currentNames[j] = trim && name instanceof String ? name.trim() : name }

                name = currentNames[j]

                //if not null/empty append it to the list
                if (name) { aList << name }
            }
            log.info "header[${i-skipLeftCols}] = $aList"
            header << aList
        }
        return header
    }


    /**
     * Recursively process the list of names to build the nested Maps and Lists
     *
     * @param map
     * @param names List of String for one column e.g. 'contacts.address[0].purpose'
     * @param value Is a String as returned by OpensCSV
     * @return
     */
    public static void convertNamesToMaps(Map<String, Object> map, List<String> names, boolean trim, String dateFormater, boolean stringOnly, String value) {
        String name = names.head()
        List<String> namesTail = names.tail()
        int index = -1

        //Dealing with repeating section, so handle it as List of Maps
        //TODO: use regex here instead of string methods
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

                convertNamesToMaps((Map)map[name], namesTail, trim, dateFormater, stringOnly, value)
            }
            else {
                //Dealing with repeating section, so handle it as List of Maps
                if(!map[name]) { map[name] = [] } //init List
                if(!((List)map[name])[index]) { ((List)map[name])[index] = [:] } //init Map in the List

                convertNamesToMaps((Map)((List)map[name])[index], namesTail, trim, dateFormater, stringOnly, value)
            }
        }
        else {
            //Assign the value to the map and cast it to a real type
            if (stringOnly || !value) {
                //value is null or empty string
                map[name] = trim && value ? value.trim() : value
            }
            else if (value.isInteger()) {
                map[name] = value.trim() as Integer
            }
            else if (value.isBigInteger()) {
                map[name] = value.trim() as BigInteger
            }
            else if (value.isBigDecimal()) {
                map[name] = value.trim() as BigDecimal
            }
            else {
                try {
                    if(dateFormater) {
                        map[name] = LocalDate.parse(value.trim(), dateFormater)
                    }
                    else {
                        map[name] = LocalDate.parse(value.trim())
                    }
                }
                catch (DateTimeParseException e) {
                    //OpenCSV always returns String
                    map[name] = trim && value ? value.trim() : value
                }
            }

            log.debug "map[name] = " + map[name].dump()
        }
    }

    /**
     *
     * @param reader
     * @param options
     * @param cl
     */
    private static void processEachRow(CSVReader reader, Map options, Closure cl) {
        String[] nextLine;

        int headerRows        = options.headerRows as int
        int skipLeftCols      = options.skipLeftCols as int
        int skipRightCols     = options.skipRightCols as int
        List<String[]> header = options.header as List
        int index = 0

        //CSV has no header
        if(!headerRows && !header) {
            log.warn "No header was specified so reverting to original openCsv behaviour"
            //TODO: processing lines could be done in parallel, but be careful as closure written by user
            while ((nextLine = reader.readNext()) != null) {
                cl(nextLine,index++)
            }
        }
        else {
            if(!header) {
                header = getCsvHeader(reader, headerRows, skipLeftCols, skipRightCols, options.trimHeader as boolean)
            }
            else {
                log.debug "external header: $header"
            }

            assert header, "no header is availalle"

            def map = [:]

            //TODO: processing lines could be done in parallel, but be careful as closure written by user
            while ((nextLine = reader.readNext()) != null) {
                assert header.size() == nextLine.size() - (skipLeftCols + skipRightCols), "Header size must be equal with the size of data line"

                //header is a List of Lists
                header.eachWithIndex { names, i ->
                    convertNamesToMaps(
                        map, 
                        names as List<String>, 
                        (boolean)options.trimData,
                        (String)options.dateFormater,
                        (boolean)options.useStringOnly,
                        nextLine[i+skipLeftCols])
                }

                log.debug "map given to closure of user: $map"

                cl(map, index++)
            }
        }
    }

    /**
     *
     * @param options
     * @return
     */
    public static void setDefaultCsvOptions(Map options) {
        assert options != null, "options cannot be null"

        options.headerRows    = options.headerRows    ?: 1 //Elvis operator
        options.skipLeftCols  = options.skipLeftCols  ?: 0
        options.skipRightCols = options.skipRightCols ?: 0
        options.trimHeader    = options.trimHeader    ?: true
        options.trimData      = options.trimData      ?: false
        options.strictQuotes  = options.strictQuotes  ?: CSVParser.DEFAULT_STRICT_QUOTES
        options.skipRows      = options.skipRows      ?: CSVReader.DEFAULT_SKIP_LINES
        options.separatorChar = options.separatorChar ?: CSVParser.DEFAULT_SEPARATOR
        options.quoteChar     = options.quoteChar     ?: CSVParser.DEFAULT_QUOTE_CHARACTER
        options.escapeChar    = options.escapeChar    ?: CSVParser.DEFAULT_ESCAPE_CHARACTER
        options.useStringOnly = options.useStringOnly ?: false
        //options.dateFormater  = options.dateFormater  ?: 'yyyy/MM/dd'
    }

    /**
     *
     * @param reader
     * @param options
     * @return
     */
    private static CSVReader getCSVReader(Reader reader, Map options) {
        def parser = new CSVParserBuilder()
            .withSeparator(options.separatorChar as char)
            .withQuoteChar(options.quoteChar as char)
            .withEscapeChar(options.escapeChar as char)
            .withStrictQuotes(options.strictQuotes as boolean)
            .build()
        
        return new CSVReaderBuilder(reader)
            .withCSVParser(parser)
            .withSkipLines(options.skipRows as int)
            .build()
    }

    /**
     * Category method
     *
     * @param self
     * @return
     */
    public static List csvHeader(File self) {
        csvHeader(self,[:])
    }

    /**
     * Category method
     *
     * @param self
     * @param options
     * @return
     */
    public static List csvHeader(File self, Map options) {
        setDefaultCsvOptions(options)
        def reader = getCSVReader(new FileReader(self), options)
        return getCsvHeader(
            reader,
            (int)options.headerRows,
            (int)options.skipLeftCols,
            (int)options.skipRightCols,
            (boolean)options.trimHeader)
    }

    /**
     * Category method
     *
     * @param self
     * @param cl
     */
    public static void csvEachRow(File self, Closure cl) {
        csvEachRow(self, [:], cl)
    }

    /**
     * Category method
     *
     * @param self
     * @param options
     * @param cl
     */
    public static void csvEachRow(File self, Map options, Closure cl) {
        setDefaultCsvOptions(options)
        def reader = getCSVReader(new FileReader(self), options)
        processEachRow(reader, options, cl)
    }

    /**
     * Category method
     *
     * @param self
     * @return
     */
    public static List csvHeader(String self) {
        csvHeader(self,[:])
    }

    /**
     * Category method
     *
     * @param self
     * @param options
     * @return
     */
    public static List csvHeader(String self, Map options) {
        setDefaultCsvOptions(options)
        def reader = getCSVReader(new StringReader(self), options)
        return getCsvHeader(
            reader,
            (int)options.headerRows,
            (int)options.skipLeftCols,
            (int)options.skipRightCols,
            (boolean)options.trimHeader)
    }

    /**
     * Category method
     *
     * @param self
     * @param cl
     */
    public static void csvEachRow(String self, Closure cl) {
        csvEachRow(self, [:], cl)
    }

    /**
     * Category method
     *
     * @param self
     * @param options
     * @param cl
     */
    public static void csvEachRow(String self, Map options, Closure cl) {
        setDefaultCsvOptions(options)
        def reader = getCSVReader(new StringReader(self), options)
        processEachRow(reader, options, cl)
    }

    /**
     * @deprecated use {@link #csvEachRow(File, Closure)} to avoid name collision with 
     * CSVGroovyParser when it is used as groovy Category or Extension.
     * @param file
     * @param cl
     * @throws IOException
     */
    @Deprecated
    public static void parse(final File file, Closure cl) throws IOException {
        csvEachRow(file, [headerRows: 1, trimData: true, useStringOnly: true], cl)
    }

    /**
     * @deprecated use {@link #csvEachRow(String, Closure)} to avoid name collision with 
     * CSVGroovyParser when it is used as groovy Category or Extension.
     * @param string
     * @param cl
     * @throws IOException
     */
    @Deprecated
    public static void parse(final String string, Closure cl) throws IOException {
        csvEachRow(string, [headerRows: 1, trimData: true, useStringOnly: true], cl)
    }

}
