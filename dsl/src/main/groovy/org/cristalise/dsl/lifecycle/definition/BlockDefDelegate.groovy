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

import static org.cristalise.kernel.graph.model.BuiltInEdgeProperties.ALIAS

import org.cristalise.dsl.property.PropertyDelegate
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.graph.model.GraphPoint
import org.cristalise.kernel.graph.model.GraphableEdge
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.lifecycle.ActivitySlotDef
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.lifecycle.NextDef
import org.cristalise.kernel.lifecycle.WfVertexDef;
import org.cristalise.kernel.utils.LocalObjectLoader
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j


@CompileStatic @Slf4j
class BlockDefDelegate extends PropertyDelegate {

    public NextDef firstEdge = null
    public WfVertexDef lastSlotDef = null
    public CompositeActivityDef compActDef

    BlockDefDelegate(CompositeActivityDef parent, WfVertexDef originSlotDef) {
        compActDef = parent
        lastSlotDef = originSlotDef
    }

    public void initialiseDelegate() {
        log.debug('initialiseDelegate() - NOTHING DONE')
    }

    public void finaliseDelegate() {
        log.debug('finaliseDelegate() - NOTHING DONE')
    }

    public void processClosure(Closure cl) {
        assert cl, "Block only works with a valid Closure"

        initialiseDelegate()

        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()

        finaliseDelegate()
    }

    protected NextDef addAsNext(WfVertexDef newSlotDef) {
        log.debug('addAsNext() - newSlotDef:{} lastSlotDef:{}', newSlotDef, lastSlotDef)

        NextDef nextDef = null
        if (lastSlotDef) nextDef = compActDef.addNextDef(lastSlotDef, newSlotDef)
        else             compActDef.getChildrenGraphModel().setStartVertexId(newSlotDef.ID)

        lastSlotDef = newSlotDef;
        if (!firstEdge) firstEdge = nextDef

        return nextDef
    }

    protected ActivitySlotDef addActDefAsNext(String actName, ActivityDef actDef) {
        def newSlotDef = compActDef.addExistingActivityDef(actName, actDef, new GraphPoint())
        addAsNext(newSlotDef)
        return newSlotDef
    }

    public NextDef finaliseBlock(WfVertexDef newLastSlotDef, NextDef currentFirstEdge, Object alias) {
        log.debug('finaliseBlock() - setting lastSlotDef:{} to newLastSlotDef:{}', lastSlotDef, newLastSlotDef)

        lastSlotDef = newLastSlotDef

        if (alias && currentFirstEdge) currentFirstEdge.setBuiltInProperty(ALIAS, alias)

        return null
    }

    public BlockDefDelegate Block(@DelegatesTo(BlockDefDelegate) Closure cl) {
        return Block(null, cl)
    }

    public BlockDefDelegate Block(Map<String, Object> initialProps = null, @DelegatesTo(BlockDefDelegate) Closure cl = null) {
        throw new InvalidDataException('Nested blocks is not supported')
    }

    public LoopDefDelegate LoopInfinite(@DelegatesTo(LoopDefDelegate) Closure cl) {
        return LoopInfinite(null, cl)
    }

    public LoopDefDelegate LoopInfinite(Map<String, Object> initialProps = null, @DelegatesTo(LoopDefDelegate) Closure cl = null) {
        // Add the conditions to make it endless
        if (!initialProps) initialProps = [:]

        //initialProps.groovy = true
        initialProps.RoutingExpr = 'true'

        return Loop(initialProps, cl)
    }

    public LoopDefDelegate Loop(@DelegatesTo(LoopDefDelegate) Closure cl) {
        return Loop(null, cl)
    }

    public LoopDefDelegate Loop(Map<String, Object> initialProps = null, @DelegatesTo(LoopDefDelegate) Closure cl = null) {
        def loopD =  new LoopDefDelegate(compActDef, lastSlotDef, initialProps)

        if (cl) {
            loopD.processClosure(cl)

            finaliseBlock(loopD.joinDefLast, loopD.firstEdge, initialProps?.Alias as String)
        }

        return loopD
    }

    def Act(ActivityDef actDef, @DelegatesTo(PropertyDelegate) Closure cl = null) {
        return Act(actDef.actName, actDef, cl)
    }

    def Act(String actName, ActivityDef actDef, @DelegatesTo(PropertyDelegate) Closure cl = null) {
        def newSlotDef = addActDefAsNext(actName, actDef)

        if (cl) {
            def propD = new PropertyDelegate()
            propD.processClosure(cl)

            propD.props?.each { k, v ->
                newSlotDef.properties.put(k, v, props.getAbstract().contains(k))
            }
        }
        return newSlotDef
    }

    def ElemActDef(String actDefName, int actVer, @DelegatesTo(PropertyDelegate) Closure cl = null) {
        return Act(LocalObjectLoader.getElemActDef(actDefName, actVer), cl)
    }

    def ElemActDef(String actName, String actDefName, int actVer, @DelegatesTo(PropertyDelegate) Closure cl = null) {
        return Act(actName, LocalObjectLoader.getElemActDef(actDefName, actVer), cl)
    }

    def ElemActDef(ActivityDef actDef, @DelegatesTo(PropertyDelegate) Closure cl = null) {
        return Act(actDef.actName, actDef, cl)
    }

    def ElemActDef(String actName, ActivityDef actDef, @DelegatesTo(PropertyDelegate) Closure cl = null) {
        return Act(actName, actDef, cl)
    }

    def CompActDef(String actDefName, int actVer, @DelegatesTo(PropertyDelegate) Closure cl = null) {
        return Act(LocalObjectLoader.getCompActDef(actDefName, actVer), cl)
    }

    def CompActDef(String actName, String actDefName, int actVer, @DelegatesTo(PropertyDelegate) Closure cl = null) {
        return Act(actName, LocalObjectLoader.getCompActDef(actDefName, actVer), cl)
    }

    def CompActDef(CompositeActivityDef actDef, @DelegatesTo(PropertyDelegate) Closure cl = null) {
        return Act(actDef.actName, actDef, cl)
    }

    def CompActDef(String actName, CompositeActivityDef actDef, @DelegatesTo(PropertyDelegate) Closure cl = null) {
        return Act(actName, actDef, cl)
    }

    public AndSplitDefDelegate AndSplit(@DelegatesTo(AndSplitDefDelegate) Closure cl) {
        return AndSplit(null, cl)
    }

    public AndSplitDefDelegate AndSplit(Map<String, Object> initialProps = null, @DelegatesTo(AndSplitDefDelegate) Closure cl = null) {
        def andD =  new AndSplitDefDelegate(compActDef, lastSlotDef, initialProps)

        if (cl) {
            andD.processClosure(cl)
            finaliseBlock(andD.lastSlotDef, andD.firstEdge, initialProps?.Alias as String)
        }

        return andD
    }

    public OrSplitDefDelegate OrSplit(Map<String, Object> initialProps = null, @DelegatesTo(OrSplitDefDelegate) Closure cl) {
        def orD =  new OrSplitDefDelegate(compActDef, lastSlotDef, initialProps)

        if (cl) {
            orD.processClosure(cl)
            finaliseBlock(orD.lastSlotDef, orD.firstEdge, initialProps?.Alias as String)
        }

        return orD
    }

    public XOrSplitDefDelegate XOrSplit(Map<String, Object> initialProps = null, @DelegatesTo(XOrSplitDefDelegate) Closure cl) {
        def xorD =  new XOrSplitDefDelegate(compActDef, lastSlotDef, initialProps)

        if (cl) {
            xorD.processClosure(cl)
            finaliseBlock(xorD.lastSlotDef, xorD.firstEdge, initialProps?.Alias as String)
        }

        return xorD
    }
}
