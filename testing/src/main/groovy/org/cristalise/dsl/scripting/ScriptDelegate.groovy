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
package org.cristalise.dsl.scripting

import groovy.xml.MarkupBuilder


/**
 * Is is a wrapper for MarkapBuilder to make building cristalscript XML simpler
 */
class ScriptDelegate {
    String module = ""
    String name   = ""
    int version   = -1

    MarkupBuilder xml
    StringWriter writer

    public ScriptDelegate(String m, String n, int v) {
        module = m
        name = n
        version = v

        writer = new StringWriter()
        xml = new MarkupBuilder(writer)
        writer << '<?xml version="1.0" encoding="UTF-8"?>\n'
    }

    def script(Map attrs, Closure cl) {
        assert attrs.language

        script(attrs.language, cl)
    }

    def javascript(Closure cl) {
        script('javascript',  cl)
    }

    def script(String lang, Closure cl) {
        xml.script(language: "$lang", name: "$name") {
            def string = cl()
            mkp.yieldUnescaped("<![CDATA[ $string ]]>")
        }
    }

    def output(String type) {
        xml.output(type: "$type")
    }

    def input(String iName, String type) {
        xml.param(name: iName, type: type)
    }

    public void processClosure(Closure cl) {
        assert cl, "Script only works with a valid Closure"

        xml.cristalscript {
            cl.delegate = this
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
        }
    }
}
