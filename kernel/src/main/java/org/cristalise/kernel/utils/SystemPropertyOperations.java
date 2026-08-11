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

import java.util.Properties;

import org.apache.commons.beanutils.converters.BooleanConverter;
import org.apache.commons.beanutils.converters.IntegerConverter;
import org.apache.commons.beanutils.converters.StringConverter;
import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.process.Gateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Java enum type does not support inheritance. This interface provides implementations to handle 
 * SystemProperties in a uniform way enabling modules to define their SystemProperties independently.
 */
public interface SystemPropertyOperations {

    /**
     * This logger name is made unusual to avoid the problem when this interface is used in SystemProperty enums.
     * When all the values of such SystemProperty is statically imported, the log variable will be imported as well. 
     * This will somehow supersede the log variable declared of the importing class.
     */
    final static Logger enumLogger = LoggerFactory.getLogger(SystemPropertyOperations.class);

    /**
     * @return the String name of the SystemProperty, e.g. 'ItemServer.Telnet.host'
     */
    String getSystemPropertyName();

    /**
     * @return the default value of the actual SystemPoperty, can be null.
     */
    Object getDefaultValue();

    /**
     * Retrieve the actual name of the SystemProperty. It is used for those SystemProperty names, 
     * which are based on {@link String#format(String, Object...)}
     * 
     * @param nameArgs array of Strings to be used in {@link String#format(String, Object...)} 
     * to retrieve the actual name of the SystemProperty
     * @return actual name of the SystemProperty
     * 
     * @implNote Internal method
     */
    default String getActualName(Object... nameArgs) {
        return nameArgs == null ? getSystemPropertyName() : String.format(getSystemPropertyName(), nameArgs);
    }

    /**
     * @return the value of the SystemProperty as Object. If no value is provided return the configured 
     * default value if available otherwise return null.
     */
    default Object getObject() {
        return getObject(null, (Object[])null);
    }

    /**
     * @param defaultOverwrite Use this value as default. Overwrites the default value defined in the enum. Can be null.
     * @return the value of the SystemProperty as Object. If no value is provided return the provided default value 
     * or configured default value if available otherwise return null
     */
    default Object getObject(Object defaultOverwrite) {
        return getObject(defaultOverwrite, (Object[])null);
    }

    /**
     * Retrieve the value of the SystemProperty as Object for cases when the name of the SystemProperty
     * is the combination of a predefined String and input string(s) required to configure the actual functionality.
     * <br>
     * To support this the SystemProperty name is based on {@link String#format(String, Object...)} 
     * i.e. it contains string like 'OutcomeInit.%s'. 
     * 
     * @param nameArgs array of Strings to be used in {@link String#format(String, Object...)}.
     * to retrieve the actual name of the SystemProperty. Can be null.
     * @return the value of the SystemProperty as Object. If no value is provided return configured default value 
     * if available otherwise return null
     */
    default Object getObject(Object... nameArgs) {
        return getObject(null, nameArgs);
    }

    /**
     * Retrieve the value of the SystemProperty as Object covering all the cases. It can retrieve the value 
     * associated with the SystemProperty name or it can retrieve the value when the name of the SystemProperty
     * is the combination of a predefined String and input string(s) required to configure the actual functionality.
     * <br>
     * To support this the SystemProperty name is based on {@link String#format(String, Object...)} 
     * i.e. it contains string like 'OutcomeInit.%s'. 
     * 
     * @param defaultOverwrite Use this value as default. Overwrites the default value defined in the enum. Can be null.
     * @param nameArgs array of Strings to be used in {@link String#format(String, Object...)} 
     * to retrieve the actual name of the SystemProperty. Can be null.
     * @return the value of the SystemProperty as Object. If no value is provided return configured default value 
     * if available otherwise return null
     */
    default Object getObject(Object defaultOverwrite, Object...nameArgs) {
        String actualName = getActualName(nameArgs);
        Object actualDefaultValue = defaultOverwrite == null ? getDefaultValue() : defaultOverwrite;

        Object actualValue = Gateway.getProperties().get(actualName);

        enumLogger.trace("getObject() - {} => {} (default:{})", actualName, actualValue, actualDefaultValue);

        if (actualDefaultValue == null) {
            return actualValue;
        }
        else {
            if (actualValue == null) return actualDefaultValue;
            else                     return actualValue;
        }
    }

    /**
     * @return the value of the SystemProperty as String (trimmed). If no value is provided return the configured 
     * default value if available otherwise return null.
     * 
     * @implNote Based on {@link StringConverter}
     */
    default String getString() {
        return getString(null, (Object[])null);
    }

    /**
     * @param defaultOverwrite Use this value as default. Overwrites the default value defined in the enum. Can be null.
     * @return the value of the SystemProperty as String (trimmed). If no value is provided return the provided default value 
     * or configured default value if available otherwise return null.
     * 
     * @implNote Based on {@link StringConverter}
     */
    default String getString(String defaultOverwrite) {
        return getString(defaultOverwrite, (Object[])null);
    }

    /**
     * Retrieve the value of the SystemProperty as String (trimmed) for cases when the name of the SystemProperty
     * is the combination of a predefined String and input string(s) required to configure the actual functionality.
     * <br>
     * To support this the SystemProperty name is based on {@link String#format(String, Object...)} 
     * i.e. it contains string like 'OutcomeInit.%s'. 
     * 
     * @param nameArgs array of Strings to be used in {@link String#format(String, Object...)}.
     * to retrieve the actual name of the SystemProperty. Can be null.
     * @return the value of the SystemProperty as String (trimmed). If no value is provided return configured default value 
     * if available otherwise return null
     * 
     * @implNote Based on {@link StringConverter}
     */
    default String getString(Object... nameArgs) {
        return getString(null, nameArgs);
    }

    /**
     * Retrieve the value of the SystemProperty as String (trimmed) covering all the cases. It can retrieve the value 
     * associated with the SystemProperty name or it can retrieve the value when the name of the SystemProperty
     * is the combination of a predefined String and input string(s) required to configure the actual functionality.
     * <br>
     * To support this the SystemProperty name is based on {@link String#format(String, Object...)} 
     * i.e. it contains string like 'OutcomeInit.%s'. 
     * 
     * @param defaultOverwrite Use this value as default. Overwrites the default value defined in the enum. Can be null.
     * @param nameArgs array of Strings to be used in {@link String#format(String, Object...)} 
     *     to retrieve the actual name of the SystemProperty. Can be null.
     * @return the value of the SystemProperty as String (trimmed). If no value is provided return configured default value 
     *     if available otherwise return null.
     * 
     * @implNote Based on {@link StringConverter}
     */
    default String getString(String defaultOverwrite, Object... nameArgs) {
        Object actualValue = getObject(defaultOverwrite, nameArgs);

        if (enumLogger.isDebugEnabled()) enumLogger.trace("getString() - {} => {}", getActualName(nameArgs), actualValue);

        if (actualValue != null) return new StringConverter(null).convert(String.class, actualValue).trim();
        else                     return null;
    }

    /**
     * @return the value of the SystemProperty as Integer. If no value is provided return the configured 
     * default value if available otherwise return null.
     * 
     * @implNote Based on {@link IntegerConverter}
     */
    default Integer getInteger() {
        return getInteger(null, (Object[])null);
    }

    /**
     * @param defaultOverwrite Use this value as default. Overwrites the default value defined in the enum
     * @return the value of the SystemProperty as Integer. If no value is provided return the provided default value 
     * or configured default value if available otherwise return null.
     * 
     * @implNote Based on {@link IntegerConverter}
     */
    default Integer getInteger(Integer defaultOverwrite) {
        return getInteger(defaultOverwrite, (Object[])null);
    }

    /**
     * Retrieve the value of the SystemProperty as Integer for cases when the name of the SystemProperty
     * is the combination of a predefined String and input string(s) required to configure the actual functionality.
     * <br>
     * To support this the SystemProperty name is based on {@link String#format(String, Object...)} 
     * i.e. it contains string like 'OutcomeInit.%s'. 
     * 
     * @param nameArgs array of Strings to be used in {@link String#format(String, Object...)}.
     * to retrieve the actual name of the SystemProperty. Can be null.
     * @return the value of the SystemProperty as Integer. If no value is provided return configured default value 
     * if available otherwise return null
     * 
     * @implNote Based on {@link IntegerConverter}
     */
    default Integer getInteger(Object... args) {
        return getInteger(null, args);
    }

    /**
     * Retrieve the value of the SystemProperty as Integer covering all the cases. It can retrieve the value 
     * associated with the SystemProperty name or it can retrieve the value when the name of the SystemProperty
     * is the combination of a predefined String and input string(s) required to configure the actual functionality.
     * <br>
     * To support this the SystemProperty name is based on {@link String#format(String, Object...)} 
     * i.e. it contains string like 'OutcomeInit.%s'. 
     * 
     * @param defaultOverwrite Use this value as default. Overwrites the default value defined in the enum. Can be null.
     * @param nameArgs array of Strings to be used in {@link String#format(String, Object...)} 
     * to retrieve the actual name of the SystemProperty. Can be null.
     * @return the value of the SystemProperty as Integer. If no value is provided return configured default value 
     * if available otherwise return null
     * 
     * @implNote Based on {@link IntegerConverter}
     */
    default Integer getInteger(Integer defaultOverwrite, Object... args) {
        Object actualValue = getObject(defaultOverwrite, args);

        if (enumLogger.isDebugEnabled()) enumLogger.trace("getInteger() - {} => {}", getActualName(args), actualValue);

        if (actualValue != null) return new IntegerConverter(null).convert(Integer.class, actualValue);
        else                     return null;
    }

    /**
     * @return the value of the SystemProperty as Boolean. If no value is provided return the configured 
     * default value if available otherwise return null.
     * 
     * @implNote Based on {@link BooleanConverter}
     */
    default Boolean getBoolean() {
        return getBoolean(null, (Object[])null);
    }

    /**
     * @param defaultOverwrite Use this value as default. Overwrites the default value defined in the enum
     * @return the value of the SystemProperty as Boolean. If no value is provided return the provided default value 
     * or configured default value if available otherwise return null.
     * 
     * @implNote Based on {@link BooleanConverter}
     */
    default Boolean getBoolean(Boolean defaultOverwrite) {
        return getBoolean(defaultOverwrite, (Object[])null);
    }

    /**
     * Retrieve the value of the SystemProperty as Boolean for cases when the name of the SystemProperty
     * is the combination of a predefined String and input string(s) required to configure the actual functionality.
     * <br>
     * To support this the SystemProperty name is based on {@link String#format(String, Object...)} 
     * i.e. it contains string like 'OutcomeInit.%s'. 
     * 
     * @param nameArgs array of Strings to be used in {@link String#format(String, Object...)}.
     * to retrieve the actual name of the SystemProperty. Can be null.
     * @return the value of the SystemProperty as Boolean. If no value is provided return configured default value 
     * if available otherwise return null
     * 
     * @implNote Based on {@link BooleanConverter}
     */
    default Boolean getBoolean(Object... args) {
        return getBoolean(null, args);
    }

    /**
     * Retrieve the value of the SystemProperty as Boolean covering all the cases. It can retrieve the value 
     * associated with the SystemProperty name or it can retrieve the value when the name of the SystemProperty
     * is the combination of a predefined String and input string(s) required to configure the actual functionality.
     * <br>
     * To support this the SystemProperty name is based on {@link String#format(String, Object...)} 
     * i.e. it contains string like 'OutcomeInit.%s'. 
     * 
     * @param defaultOverwrite Use this value as default. Overwrites the default value defined in the enum. Can be null.
     * @param nameArgs array of Strings to be used in {@link String#format(String, Object...)} 
     * to retrieve the actual name of the SystemProperty. Can be null.
     * @return the value of the SystemProperty as Boolean. If no value is provided return configured default value 
     * if available otherwise return null
     * 
     * @implNote Based on {@link BooleanConverter}
     */
    default Boolean getBoolean(Boolean defaultOverwrite, Object... args) {
        Object actualValue = getObject(defaultOverwrite, args);

        if (enumLogger.isDebugEnabled()) enumLogger.trace("getBoolean() - {} => {}", getActualName(args), actualValue);

        if (actualValue != null) return new BooleanConverter(null).convert(Boolean.class, actualValue);
        else                     return null;
    }

    /**
     * Retrieve the value of the SystemProperty as String and try to instantiate the resulting String 
     * using {@link Class#forName(String)}. It covers cases when the name of the SystemProperty
     * is the combination of a predefined String and input string(s) required to configure the actual functionality.
     * <br>
     * To support this the SystemProperty name is based on {@link String#format(String, Object...)} 
     * i.e. it contains string like 'OutcomeInit.%s'. 
     * 
     * @param nameArgs array of Strings to be used in {@link String#format(String, Object...)}.
     * to retrieve the actual name of the SystemProperty. Can be null.
     * @return the value of the SystemProperty as in instance of the Class. 
     * @throws ReflectiveOperationException No value was available for the SystemProperty.
     * 
     * @implNote Based on {@link StringConverter}
     * 
     * @return
     */
    default Object getInstance(Object... nameArgs) throws ReflectiveOperationException {
        String actualValue = getString(nameArgs);

        if (enumLogger.isDebugEnabled()) enumLogger.trace("getInstance() - {} => {}", getActualName(nameArgs), actualValue);

        if (StringUtils.isBlank(actualValue)) {
            throw new InstantiationException("SystemProperty '" + getActualName(nameArgs) + "' was not defined.");
        }
        else {
            return Class.forName(actualValue).getDeclaredConstructor().newInstance();
        }
    }

    /**
     * Set the value of the SystemProperty maintained.
     * 
     * @param value of the SystemProperty
     * @return the previous value associated with SystemProperty, or null if there was no mapping for SystemProperty.
     * 
     * @apiNote Use this only for testing
     */
    default Object set(Object value) {
        return Gateway.getProperties().put(getSystemPropertyName(), value);
    }

    /**
     * Set the value in the given Properties object using the SystemProperty name.
     * 
     * @param props the properties object to be updated
     * @param value of the SystemProperty
     * @return the previous value associated with SystemProperty, or null if there was no mapping for SystemProperty.
     * 
     * @apiNote Use this only for testing
     */
    default Object set(Properties props, Object value) {
        return props.put(getSystemPropertyName(), value);
    }
}
