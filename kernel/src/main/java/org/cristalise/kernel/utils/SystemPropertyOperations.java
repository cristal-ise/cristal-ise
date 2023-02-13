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
package org.cristalise.kernel.utils;

import org.cristalise.kernel.process.Gateway;

/**
 * Java enum type does not support inheritance. This class enables to reuse the same code 
 * to handle SystemProperties in a uniform way enabling modules to define their SystemProperties independently.
 */
public interface SystemPropertyOperations {

    /**
     * @return the String name of the SystemProperty, e.g. 'ItemServer.Telnet.host'
     */
    String getSystemPropertyName();

    /**
     * @return the default value of the SystemPoperty, can be null.
     */
    Object getDefaultValue();

    /**
     * @return the value of the SystemProperty as Object. If no value is provided return the configured 
     * default value if available otherwise return null.
     */
    default Object get() {
        if (getDefaultValue() == null) {
            return Gateway.getProperties().get(getSystemPropertyName());
        }
        else {
            Object actValue = Gateway.getProperties().get(getSystemPropertyName());

            if (actValue == null) return getDefaultValue();
            else                  return Gateway.getProperties().get(getSystemPropertyName());
        }
    }

    /**
     * @return the value of the SystemProperty as Boolean. If no value is provided return the configured 
     * default value if available otherwise return null.
     */
    default Boolean getBoolean() {
        return getBoolean(null);
    }

    /**
     * @param defaultOverwrite Use this value as default. Overwrites the the default value defined in the enum
     * @return the value of the SystemProperty as Boolean. If no value is provided return the provided default value 
     * or configured default value if available otherwise return null.
     */
    default Boolean getBoolean(Boolean defaultOverwrite) {
        Object actualDefaultValue = defaultOverwrite == null ? getDefaultValue() : defaultOverwrite;

        if (actualDefaultValue == null) {
            return Gateway.getProperties().getBoolean(getSystemPropertyName());
        }
        else {
            return Gateway.getProperties().getBoolean(getSystemPropertyName(), (Boolean) actualDefaultValue);
        }
    }

    /**
     * @return the value of the SystemProperty as Integer. If no value is provided return the configured 
     * default value if available otherwise return null.
     */
    default Integer getInteger() {
        return getInteger(null);
    }

    /**
     * @param defaultOverwrite Use this value as default. Overwrites the the default value defined in the enum
     * @return the value of the SystemProperty as Integer. If no value is provided return the provided default value 
     * or configured default value if available otherwise return null.
     */
    default Integer getInteger(Integer defaultOverwrite) {
        Object actualDefaultValue = defaultOverwrite == null ? getDefaultValue() : defaultOverwrite;

        if (actualDefaultValue == null) {
            return Gateway.getProperties().getInteger(getSystemPropertyName());
        }
        else {
            return Gateway.getProperties().getInteger(getSystemPropertyName(), (Integer)actualDefaultValue);
        }
    }

    /**
     * @return the value of the SystemProperty as String. If no value is provided return the configured 
     * default value if available otherwise return null.
     */
    default String getString() {
        return getString(null);
    }

    /**
     * @param defaultOverwrite Use this value as default. Overwrites the the default value defined in the enum
     * @return the value of the SystemProperty as String. If no value is provided return the provided default value 
     * or configured default value if available otherwise return null.
     */
    default String getString(String defaultOverwrite) {
        Object actualDefaultValue = defaultOverwrite == null ? getDefaultValue() : defaultOverwrite;

        if (actualDefaultValue == null) {
            return Gateway.getProperties().getString(getSystemPropertyName());
        }
        else {
            return Gateway.getProperties().getString(getSystemPropertyName(), (String)actualDefaultValue);
        }
    }

    /**
     * @param value of the SystemProperty
     * @return the previous value associated with SystemProperty, or null if there was no mapping for SystemProperty.
     */
    default Object set(Object value) {
        return Gateway.getProperties().put(getSystemPropertyName(), value);
    }
}
