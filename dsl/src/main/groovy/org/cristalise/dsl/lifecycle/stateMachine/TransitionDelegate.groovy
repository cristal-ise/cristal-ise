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
package org.cristalise.dsl.lifecycle.stateMachine

import org.cristalise.kernel.lifecycle.instance.stateMachine.Transition
import org.cristalise.kernel.lifecycle.instance.stateMachine.TransitionOutcome
import org.cristalise.kernel.lifecycle.instance.stateMachine.TransitionQuery
import org.cristalise.kernel.lifecycle.instance.stateMachine.TransitionScript

import groovy.util.logging.Slf4j


/**
 *
 */
@Slf4j
class TransitionDelegate {
    Transition trans

    public TransitionDelegate(Transition t) {
        trans = t
    }

    public void processClosure(Closure cl) {
        assert cl, "TransitionDelegate only works with a valid Closure"

        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()
    }

    public void property(Map<String,Object> attrs) {
        log.debug "property() - attrs: $attrs"
        assert attrs, ""

        //FIXME: dynamic groovy is needed for this line only
        attrs.each { String k, v -> trans."$k" = v }
    }

    public void outcome(Map attrs) {
        log.debug "outcome() - attrs: $attrs"
        assert attrs && attrs.name && attrs.version, "Transition Property Name or Version is null"

        trans.outcome = new TransitionOutcome(attrs.name, attrs.version)
    }

    public void script(Map attrs) {
        log.debug "script() - attrs: $attrs"
        assert attrs && attrs.name && attrs.version

        trans.script = new TransitionScript(attrs.name, attrs.version)
    }

    public void query(Map attrs) {
        log.debug "query() - attrs: $attrs"
        assert attrs && attrs.name && attrs.version

        trans.query = new TransitionQuery(attrs.name, attrs.version)
    }
}
