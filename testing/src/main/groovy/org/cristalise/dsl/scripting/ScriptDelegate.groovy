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

import org.cristalise.kernel.utils.Logger


/**
 *
 */
class ScriptDelegate {
    String name = ""
    int version = -1

    MarkupBuilder xml
    StringWriter writer

    public ScriptDelegate() {
        writer = new StringWriter()
        xml = new MarkupBuilder(writer)
        writer << '<?xml version="1.0" encoding="UTF-8"?>\n'
    }

    def script(Map attrs, Closure cl) {
        assert attrs.language
        String sName = attrs?.name

        script(attrs.language, sName, cl)
    }

    def javascript(Closure cl) {
        script('javascript', cl)
    }

    def script(String lang, String sName = "", Closure cl) {
        xml.script(language: "$lang", name: "$sName") {
            def string = cl()
            mkp.yieldUnescaped("<![CDATA[ $string ]]>")
        }
    }

    def output(String type) {
        xml.output(type: "$type")
    }

    def input(String name, String type) {
        xml.param(name: name, type: type)
    }

    public void processClosure(Closure cl) {
        assert cl, "Script only works with a valid Closure"

        Logger.msg 1, "Script(start) ---------------------------------------"

        xml.cristalscript {
            cl.delegate = this
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
        }

        Logger.msg 1, "Script(end) +++++++++++++++++++++++++++++++++++++++++"
    }
}
