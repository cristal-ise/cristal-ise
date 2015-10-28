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

import groovy.transform.CompileStatic

import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine
import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.persistency.outcome.Outcome
import org.cristalise.kernel.process.Bootstrap
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.utils.Logger


/**
 *
 */
@CompileStatic
class StateMachineBuilder {
    String module = ""
    String name = ""
    int version = -1

    StateMachine sm
    String smXML

    DomainPath domainPath = null

    public StateMachineBuilder() {}

    /**
     * 
     * @param module
     * @param name
     * @param version
     */
    public StateMachineBuilder(String module, String name, int version) {
        this.module  = module
        this.name    = name
        this.version = version
    }
        
    /**
     * 
     * @param module
     * @param delegate
     */
    public StateMachineBuilder(String module, StateMachineDelegate delegate) {
        this(module, delegate.name, delegate.version)
        this.sm = delegate.sm
    }

    public StateMachineBuilder loadXML(String xmlFile) {
        Logger.msg 5, "StateMachineBuilder.loadXML() - From file:$xmlFile"

        smXML = new File(xmlFile).getText()
        sm = (StateMachine)Gateway.getMarshaller().unmarshall(smXML)
        sm.validate()

        return this
    }

    public static StateMachine create(String module, String name, int version, Closure cl) {
        def builder = build(module, name, version, cl)
        builder.create()
        return builder.sm
    }

    public static StateMachineBuilder build(String module, String name, int version, Closure cl) {
        def delegate = new StateMachineDelegate(name, version)

        delegate.processClosure(cl)

        def builder = new StateMachineBuilder(module, delegate)
        builder.sm.validate()

        builder.smXML =  Gateway.getMarshaller().marshall(builder.sm)

        Logger.msg(5, builder.smXML)

        return builder
    }

    public static StateMachine create(String module, String name, int version, String xmlFile) {
        def builder = build(module, name, version, xmlFile)
        builder.create()
        return builder.sm
    }

    public static StateMachineBuilder build(String module, String name, int version, String xmlFile) {
        def builder = new StateMachineBuilder(module, name, version)
        return builder.loadXML(xmlFile)
    }

    /**
     * Bootstrap method to create the ResourceItem from a fully configured StateMachine
     *  
     * @return the DomainPath of the newly created resource Item
     */
    public DomainPath create() {
        return domainPath = Bootstrap.createResource(module, name, version, "SM", [new Outcome(-1, smXML, "StateMachine", version)] as Set, false)
    }
}
