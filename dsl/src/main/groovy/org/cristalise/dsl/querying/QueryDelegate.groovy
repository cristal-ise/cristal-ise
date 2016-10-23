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
package org.cristalise.dsl.querying

import groovy.xml.MarkupBuilder

import org.cristalise.kernel.utils.Logger


/**
 * Wrapper for MarkapBuilder to make building cristalscript XML simpler
 */
class QueryDelegate {
    String module = ""
    String name   = ""
    int version   = -1

    MarkupBuilder xml
    StringWriter writer

    public QueryDelegate(String m, String n, int v) {
        module = m
        name = n
        version = v

        writer = new StringWriter()
        xml = new MarkupBuilder(writer)
        writer << '<?xml version="1.0" encoding="UTF-8"?>\n'
    }

    def parameter(Map attrs) {
        assert attrs.name && attrs.type

        parameter(attrs.name, attrs.type)
    }

    def parameter(String n, String t) {
        Logger.msg("QueryDelegate.parameter() name:$n type:$t")
        xml.parameter('name': n, 'type': t)
    }

    def query(Map attrs, Closure cl) {
        assert attrs.language

        query(attrs.language, cl)
    }

    def query(String lang, Closure cl) {
        xml.query(language: lang) {
            def string = cl()
            mkp.yieldUnescaped("<![CDATA[ $string ]]>")
        }
    }

    public void processClosure(Closure cl) {
        assert cl, "Query only works with a valid Closure"

        xml.cristalquery(name: name, version: version) {
            cl.delegate = this
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
        }
    }
}
