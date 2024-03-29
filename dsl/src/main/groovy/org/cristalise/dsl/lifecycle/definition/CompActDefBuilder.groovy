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

import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.lifecycle.renderer.LifecycleRenderer
import org.jfree.graphics2d.svg.SVGGraphics2D
import org.jfree.graphics2d.svg.SVGUtils

import groovy.transform.CompileStatic

 
/**
 *
 */
@CompileStatic
class CompActDefBuilder {

    public static CompositeActivityDef build(String name, int version, @DelegatesTo(CompActDefDelegate) Closure cl) {
        return build([module: "", name: name, version: version] as LinkedHashMap, cl)
    }

    public static CompositeActivityDef build(Map<String, Object> attrs, @DelegatesTo(CompActDefDelegate) Closure cl) {
        def delegate = new CompActDefDelegate((String)attrs.name, (Integer)attrs.version)
        delegate.processClosure(cl)
        if (delegate.generate) attrs['generate'] = true
        return (CompositeActivityDef) delegate.activityDef
    }

    /**
     *
     * @param caDef
     */
    public static void generateWorkflowSVG(String dir, CompositeActivityDef caDef) {
        LifecycleRenderer generator = new LifecycleRenderer(caDef.getChildrenGraphModel(), true)
        int zoomFactor = generator.getZoomFactor(1000, 1000)

        SVGGraphics2D svgG2D = new SVGGraphics2D(1000, 1000)
        svgG2D.scale((double) zoomFactor / 100, (double) zoomFactor / 100)

        generator.draw(svgG2D)

        SVGUtils.writeToSVG(new File("${dir}/${caDef.name}_${caDef.version}.svg"), svgG2D.getSVGElement())
    }

}
