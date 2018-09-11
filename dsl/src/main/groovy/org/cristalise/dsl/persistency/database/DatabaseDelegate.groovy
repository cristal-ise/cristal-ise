/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.dsl.persistency.database

import org.codehaus.groovy.runtime.StringBufferWriter
import org.cristalise.dsl.persistency.outcome.Struct
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.utils.Logger
/**
 *
 */
class DatabaseDelegate {

    String importLines = "import static org.jooq.impl.DSL.*\nimport java.sql.Date\nimport java.sql.Timestamp\n\n"
    String commentLine = "//Script to be executed by domain handler class.\n\n"
    String uuidField = "UUID"
    String dbCreateString
    String dbInsertString
    String dbSelectString
    String dbUpdateString

    /**
     *
     * @param name
     * @param cl
     */
    void processClosure(String name, Closure cl) {

        assert cl, "Database only works with a valid Closure"

        Logger.msg 1, "Database(start) ---------------------------------------"

        def objBuilder = new ObjectGraphBuilder()
        objBuilder.classLoader = this.class.classLoader
        objBuilder.classNameResolver = 'org.cristalise.dsl.persistency.outcome'
        cl.delegate = objBuilder
        buildDB(name, cl())

        Logger.msg 1, "Database(end) +++++++++++++++++++++++++++++++++++++++++"
    }

    /**
     *
     * @param name
     * @param s
     */
    void buildDB(String name, Struct s) {
        if (!s) throw new InvalidDataException("Database cannot be built from empty declaration")
        def tableName = name.toUpperCase()
        def table = "def ${tableName} = table(name('${tableName}'))\n"
        def dbBuffer = new StringBuffer()
        def writer = new StringBufferWriter(dbBuffer)
        writer.append(importLines)
        writer.append(commentLine)
        writer.append(table)

        def fields = buildFields(writer, s)
        String commonLines = writer.toString()

        // construct create table
        dbCreateString = buildScriptContent(tableName, commonLines, fields, DatabaseType.CREATE)
        // construct insert query
        dbInsertString = buildScriptContent(tableName, commonLines, fields, DatabaseType.INSERT)
        // construct select query
        dbSelectString = buildScriptContent(tableName, commonLines, fields, DatabaseType.SELECT)
        // construct update query
        dbUpdateString = buildScriptContent(tableName, commonLines, fields, DatabaseType.UPDATE)
    }

    /**
     *
     * @param w
     * @param s
     * @return
     */
    private List<String> buildFields(StringBufferWriter w, Struct s) {
        Logger.msg 1, "DatabaseDelegate.buildStruct() - Struct: $s.name"

        def jqFields = new ArrayList()
        if (s.fields || s.structs || s.anyField) {
            w.append("def ${uuidField} = field(name('${uuidField}'), UUID.class)\n")
            jqFields.add(uuidField)
            s.fields.each {
                def fieldName = "${it.name}".toUpperCase()
                def type = getJooqFieldTye(it.type)
                w.append("def ${fieldName} = field(name('${fieldName}'), ${type})\n")
                jqFields << it.name
            }
        }

        return jqFields

    }

    /**
     *
     * @param structTye
     * @return
     * @throws IOException
     */
    private String getJooqFieldTye(String structTye) throws IOException {

        def fieldType
        if (structTye.contains(":")) {
            fieldType = structTye.split(":")[1]
        }

        switch (fieldType.toLowerCase()) {
            case "double":
                return "Double.class"
            case "int":
            case "integer":
                return "Integer.class"
            case "uuid":
                return "UUID.class"
            case "boolean":
                return "Boolean.class"
            case "float":
                return "Float.class"
            case "long":
                return "Long.class"
            case "byte":
                return "Byte.class"
                break
            case "char":
            case "character":
                return "Character.class"
            case "short":
                return "Short.class"
            case "datetime":
                return "Timestamp.class"
            case "date":
                return "Date.class"
            case "string":
                return "String.class"
            case "decimal":
                return "BigDecimal.class"
            default:
                throw new IOException("Invalid field data type. '${fieldType}'")
        }
    }

    /**
     *
     * @param name
     * @param commonLines
     * @param fields
     * @param type
     * @return
     */
    private String buildScriptContent(String name, String commonLines, List<String> fields, DatabaseType type) {

        def w = new StringBufferWriter(new StringBuffer(commonLines + "\n"))

        if (type == DatabaseType.CREATE) { // create table content

            w.append("dsl.createTableIfNotExists(${name})\n")
            fields.each {
                w.append("        .column(${it.toUpperCase()})\n")
            }
            w.append("        .constraints(constraint('PK_' + ${name}).primaryKey(${uuidField}))\n")
            w.append("        .execute()\n")

        } else if (type == DatabaseType.INSERT) { // insert query content

            w.append("def insertQueryResult = dsl.insertQuery(${name})\n")
            fields.each {
                if (it == uuidField){
                    w.append("insertQueryResult.addValue(${it.toUpperCase()}, uuid)\n")
                }else {
                    w.append("insertQueryResult.addValue(${it.toUpperCase()}, outcome.getField('${it}'))\n")
                }
            }
            w.append("def result = insertQueryResult.execute()\n\n")
            w.append("result")

        } else if (type == DatabaseType.SELECT) { //select query content
            w.append("def selectQueryResult = dsl.select()\n")
            w.append("        .from(${name})\n")
            w.append("        .where(${uuidField}.equal(uuid))\n")
            w.append("        .fetchOne()\n\n")
            w.append("selectQueryResult")
        } else if (type == DatabaseType.UPDATE) { // update query content
            w.append("def updateQueryResult = dsl.updateQuery(${name})\n")
            fields.each {
                if (it != uuidField) {
                    w.append("updateQueryResult.addValue(${it.toUpperCase()}, outcome.getField('${it}'))\n")
                }
            }
            w.append("updateQueryResult.addConditions(${uuidField}.equal(uuid))\n")
            w.append("def result = updateQueryResult.execute()\n\n")
            w.append("result")
        }

        return w.toString()
    }

}
