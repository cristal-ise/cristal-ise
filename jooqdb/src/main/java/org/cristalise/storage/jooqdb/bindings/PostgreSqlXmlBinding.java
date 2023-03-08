/**
 * This file is part of the CRISTAL-iSE jOOQ Cluster Storage Module.
 * Copyright (c) 2001-2017 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.storage.jooqdb.bindings;

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;
import java.util.Objects;

import org.cristalise.kernel.persistency.outcome.Outcome;
import org.jooq.Binding;
import org.jooq.BindingGetResultSetContext;
import org.jooq.BindingGetSQLInputContext;
import org.jooq.BindingGetStatementContext;
import org.jooq.BindingRegisterContext;
import org.jooq.BindingSQLContext;
import org.jooq.BindingSetSQLOutputContext;
import org.jooq.BindingSetStatementContext;
import org.jooq.Converter;
import org.jooq.conf.ParamType;
import org.jooq.exception.ConfigurationException;
import org.jooq.impl.DSL;
import org.w3c.dom.Document;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("serial")
public class PostgreSqlXmlBinding implements Binding<Object, Document> {

    @Override
    public Converter<Object, Document> converter() {
        return new Converter<Object, Document>() {
            @Override public Class<Object> fromType() {
                return Object.class; 
            }

            @Override public Class<Document> toType() { 
                return Document.class;
            }

            @Override public Document from(Object xml) {
                if (xml == null) return null;

                try {
                    return Outcome.parse(xml.toString());
                }
                catch (Exception e) {
                    log.error("", e);
                    throw new ConfigurationException(e.getMessage());
                }
            }

            @Override public Object to(Document doc) {
                if (doc == null) return null;

                try {
                    return Outcome.serialize(doc, false);
                }
                catch (Exception e) {
                    log.error("", e);
                    throw new ConfigurationException(e.getMessage());
                }
            }
        };
    }

    /** 
     * Depending on how you generate your SQL, you may need to explicitly distinguish
     * between jOOQ generating bind variables or inlined literals.
     */
    @Override
    public void sql(BindingSQLContext<Document> ctx) throws SQLException {
        // '::' is a postgres specific cast operator
        if (ctx.render().paramType() == ParamType.INLINED) {
            ctx.render().visit(DSL.inline(ctx.convert(converter()).value())).sql("::xml");
        }
        else {
            ctx.render().sql("?::xml");
        }
    }

    /**
     * Registering SQLXML type for JDBC CallableStatement OUT parameters
     */
    @Override
    public void register(BindingRegisterContext<Document> ctx) throws SQLException {
        ctx.statement().registerOutParameter(ctx.index(), Types.VARCHAR);
    }

    /** 
     * Converting the SQLXML to a String value and setting that on a JDBC PreparedStatement
     */
    @Override
    public void set(BindingSetStatementContext<Document> ctx) throws SQLException {
        Object value = ctx.convert(converter()).value();
        ctx.statement().setString(ctx.index(), value == null ? null : Objects.toString(value));
    }

    /** 
     * Getting a String value from a JDBC ResultSet and converting that to a Document
     */
    @Override
    public void get(BindingGetResultSetContext<Document> ctx) throws SQLException {
        ctx.convert(converter()).value(ctx.resultSet().getString(ctx.index()));
    }

    /** 
     * Getting a String value from a JDBC CallableStatement and converting that to a Document
     */
    @Override
    public void get(BindingGetStatementContext<Document> ctx) throws SQLException {
        ctx.convert(converter()).value(ctx.statement().getString(ctx.index()));
    }

    /** 
     * Setting a value on a JDBC SQLOutput (useful for Oracle OBJECT types)
     */
    @Override
    public void set(BindingSetSQLOutputContext<Document> ctx) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /** 
     * Getting a value from a JDBC SQLInput (useful for Oracle OBJECT types)
     */
    @Override
    public void get(BindingGetSQLInputContext<Document> ctx) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }
}