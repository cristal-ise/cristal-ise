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
package org.cristalise.dsl.test.scripting

import groovy.transform.CompileStatic

import org.cristalise.dsl.scripting.ScriptBuilder
import org.cristalise.kernel.utils.Logger
import org.custommonkey.xmlunit.DetailedDiff
import org.custommonkey.xmlunit.XMLUnit


/**
 *
 */
@CompileStatic
class ScriptTestBuilder extends ScriptBuilder {
    
    @Override
    public boolean equals(Object xml) {
        Logger.msg(5, "ScriptTestBuilder.equals()")

        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setIgnoreComments(true);

        DetailedDiff diff = new DetailedDiff( XMLUnit.compareXML( (String)xml, this.scriptXML) );

        if(!diff.identical()) Logger.error(diff.toString())

        return diff.identical();
    }
}
