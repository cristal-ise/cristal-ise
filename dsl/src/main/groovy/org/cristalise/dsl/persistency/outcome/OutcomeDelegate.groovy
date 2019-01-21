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
package org.cristalise.dsl.persistency.outcome

import groovy.xml.MarkupBuilder


/**
 *
 */
class OutcomeDelegate {
    String rootElement = ""
    
    MarkupBuilder xml
    StringWriter writer

    public OutcomeDelegate() {
        writer = new StringWriter()
        xml = new MarkupBuilder(writer)
        writer << '<?xml version="1.0" encoding="UTF-8"?>\n'
    }

    public OutcomeDelegate(String root) {
        this()

        assert root
        rootElement = root
    }

    public void processClosure(Closure cl) {
        assert cl, "OutcomeDelegate only works with a valid Closure"
        
        if(rootElement) {
            xml."$rootElement" {
                cl.delegate = xml
                cl.resolveStrategy = Closure.DELEGATE_FIRST
                cl()
            }
        }
        else {
            cl.delegate = xml
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
        }

    }
}
