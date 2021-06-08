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
package org.cristalise.dsl.lifecycle.definition

import org.cristalise.dsl.csv.TabularGroovyParserBuilder
import org.cristalise.kernel.lifecycle.ActivityDef

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

 
/**
 *
 */
@CompileStatic @Slf4j
class ElemActDefBuilder {

    public static ActivityDef build(String name, int version, Closure cl) {
        return build([module: "", name: name, version: version] as LinkedHashMap, cl)
    }

    public static ActivityDef build(LinkedHashMap<String, Object> attrs, Closure cl) {
        def delegate = new ElemActDefDelegate((String)attrs.name, (Integer)attrs.version)
        delegate.processClosure(cl)
        return delegate.activityDef
    }

    public static List<ActivityDef> build(Map<String, Object> attrs, File file) {
        log.info("build(file) - module:{} name:{} version:{} file:{}", attrs.module, attrs.name, attrs.version, file.name)

        def parser = TabularGroovyParserBuilder.build(file, (String)attrs.name, 2)
        def delegate = new ElemActDefDelegate((String)attrs.name, (Integer)attrs.version)
        return delegate.processTabularData(parser)
    }
}
