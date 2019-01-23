package org.cristalise.dsl.csv

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord

import groovy.transform.CompileStatic;

@CompileStatic
class CSVGroovyParser {

    private static CSVFormat defaultFormat  = CSVFormat.DEFAULT.withIgnoreSurroundingSpaces().withHeader()
    private static Charset   defaultCharset = StandardCharsets.UTF_8

    public static void parse(final URL url, final Charset charset, final CSVFormat format, Closure block) throws IOException {
        executeLoop( CSVParser.parse(url, charset, format), block)
    }

    public static void parse(final File file, Closure block) throws IOException {
        executeLoop( CSVParser.parse(file, Charset.defaultCharset(), defaultFormat), block)
    }

    public static void parse(final File file, final Charset charset, final CSVFormat format, Closure block) throws IOException {
        executeLoop( CSVParser.parse(file, charset, format), block)
    }

    public static void parse(final String string, Closure block) throws IOException {
        executeLoop( CSVParser.parse(string, defaultFormat), block)
    }

    public static void parse(final String string, final CSVFormat format, Closure block) throws IOException {
        executeLoop( CSVParser.parse(string, format), block)
    }

    private static void executeLoop(CSVParser parser, Closure block) {
        int idx = 0;

        for(CSVRecord record : parser) {
            if( parser.headerMap == null) {
                block(record, idx++)
            }
            else {
                //copy the record into a LinkedHashMap which keeps the order of fields
                def newMapRecord = [:] //uses LinkedHashMap
                def oldMapRecord = record.toMap() //uses HashMap

                for (String key: parser.headerMap.keySet()) {
                    newMapRecord[key] = oldMapRecord[key]
                }

                block(newMapRecord, idx++)
            }
        }

        parser.close()
    }
}
