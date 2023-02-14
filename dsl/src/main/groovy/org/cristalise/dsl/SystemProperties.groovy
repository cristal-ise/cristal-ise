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
package org.cristalise.dsl

import org.cristalise.kernel.utils.SystemPropertyOperations

import groovy.transform.Immutable
import lombok.Getter

/**
 * 
 * Defines all SystemProperties that are supported in the dsl to configure the behavior of the
 * application. Due to the limitation of javadoc, the actual usable string cannot be shown easily,
 * therefore replace underscores with dots to get the actual System Property:
 * 
 * <pre>
 *   ModuleScript_lineSeparator => ModuleScript.lineSeparator
 * </pre>
 * 
 * @see #DSL_Module_BindingConvention_defaulBeanKeys
 * @see #DSL_Module_BindingConvention_variablePrefix
 * @see #DSL_Module_BindingConvention_autoAddObject
 * @see #ModuleScript_lineSeparator
 */
enum SystemProperties implements SystemPropertyOperations {

    /**
     * Comma separated keys to read from the Object when automatically adding variables to the Binding.
     * Default value is 'id,key,name'.
     */
    DSL_Module_BindingConvention_defaulBeanKeys('DSL.Module.BindingConvention.defaulBeanKeys', 'id,key,name'),
    /**
     * Add this prefix when automatically adding variables to the Binding. Default value is '$'.
     */
    DSL_Module_BindingConvention_variablePrefix('DSL.Module.BindingConvention.variablePrefix', '$'),
    /**
     * Enable the automatically variable creation to the Binding. Default value is true'.
     */
    DSL_Module_BindingConvention_autoAddObject('DSL.Module.BindingConvention.autoAddObject', true),
    /**
     * Sets the java system property 'line.separator' to be used in module scripts.
     * Default value is 'linux', i.e. '\n' will be used. Any other value will force the system 
     * to use windows specific '\r\n' line endings.
     */
    ModuleScript_lineSeparator('ModuleScript.lineSeparator', 'linux');

    final Object defaultValue;
    final String systemPropertyName;

    private SystemProperties(String name) {
        this(name, null);
    }

    private SystemProperties(String name, Object value) {
        systemPropertyName = name;
        defaultValue = value;
    }

    @Override
    public String toString() {
        return systemPropertyName;
    }
}
