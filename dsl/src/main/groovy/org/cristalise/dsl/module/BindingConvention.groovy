/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2016 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.dsl.module

import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.utils.DescriptionObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

@CompileStatic
trait BindingConvention {
    
    // @Slf4j annotation does not work on traits
    private static final Logger log = LoggerFactory.getLogger(this.class)

    /**
     * 
     */
    private static final String[] defaulBeanKeys = 
            Gateway.getProperties().getString('DSL.Module.BindingConvention.defaulBeanKeys', 'id,key,name').split(',')

    /**
     * 
     */
    private static final String variablePrefix = 
    Gateway.getProperties().getString('DSL.Module.BindingConvention.variablePrefix', '$')

    /**
     * These characters will be removed from the name of the 'variable' added to the binding
     */
    private static final String removedChars = "[\\s,./:!?;\$]+"
    
    /**
     * 
     */
    private static final boolean autoAddObject = 
            Gateway.getProperties().getBoolean('DSL.Module.BindingConvention.autoAddObject', true)

    /**
     *
     * @param name
     * @return
     */
    private String convertToValidName(String name) {
        return (name.substring(0,1).toLowerCase() + name.substring(1)).replaceAll(removedChars, '')
    }

    /**
     * Convention:
     *
     * @param obj
     * @return
     */
    @CompileDynamic
    private String getDefaultBindingsName(obj) {
        for(String beanKey: defaulBeanKeys) {
            if(obj?."$beanKey" != null) {
                log.trace('getDefaultPropertyName() - using beanKey:{}', beanKey)
                return convertToValidName(obj.class.simpleName + obj."$beanKey")
            }
        }
        return null
    }

    /**
     * 
     * @param obj
     * @return
     */
    private String getBindingsName(Object obj) {
        String propertyName = ""
        String postFix = ""

        if (obj instanceof DescriptionObject) {
            def descObj = (DescriptionObject) obj
            propertyName = convertToValidName(descObj.name)
            postFix = '_' + descObj.class.simpleName
        }
        else {
            propertyName = getDefaultBindingsName(obj)
        }

        if (propertyName) {
            def name = variablePrefix + propertyName + postFix
            log.debug('getPropertyName() - returning:{}', name)
            return name
        }
        else {
            return null
        }
    }

    /**
     * 
     * @param bindings
     * @param obj
     * @return
     */
    public String addToBingings(Binding bindings, Object obj) {
        if (autoAddObject) {
            String name = getBindingsName(obj)

            if (name) bindings[name] = obj
            else      log.warn('addToBingings() - Could not create valid bindings name for object:{}', obj)

            return name
        }
        else {
            log.trace('addToBingings() - object:{} was not added to bindings', obj)
            return null
        }
    }
}
