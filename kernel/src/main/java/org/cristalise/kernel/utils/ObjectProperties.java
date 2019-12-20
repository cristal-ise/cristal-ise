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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.process.Gateway;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;

@Slf4j
public class ObjectProperties extends Properties {

    private static final long serialVersionUID = 8214748637885650335L;

    public ObjectProperties() {
    }

    public ObjectProperties(Properties defaults) {
        super(defaults);
    }

    public String getString(String propName) {
        return getString(propName, null);
    }

    public String getString(String propName, String defaultValue) {
        String value = super.getProperty(propName, defaultValue);
        if (value != null) value = value.trim();
        return value;
    }

    /**
     * ogattaz proposal
     * 
     * @param propName
     *            the name of the property
     * @return the object value of the property. Returns null if the property doesn't exist or if the properties of the gateway is null
     */
    public Object getObject(String propName) {
        return getObject(propName, null);
    }

    /**
     * ogattaz proposal
     * 
     * @param propName
     *            the name of the property
     * @param defaultValue
     *            the default value.
     * @return the object value of the property. Returns the default value if the property doesn't exist or if the properties of the gateway
     *         is null.
     */
    public Object getObject(String propName, Object defaultValue) {
        Object wValue = get(propName);
        if (wValue == null) {
            return defaultValue;
        }
        return wValue;
    }

    /**
     * ogattaz proposal
     * 
     * @param aPropertyName
     *            the name of the paroperty
     * @return the boolean value of the property. Returns false if the property doesn't exist or if the value is not a String or a Boolean
     *         instance
     */
    public boolean getBoolean(String aPropertyName) {
        return getBoolean(aPropertyName, Boolean.FALSE);
    }

    /**
     * ogattaz proposal
     * 
     * @param aPropertyName
     *            the name of the parameter stored in the clc file
     * @param defaultValue
     *            the default value
     * @return the boolean value of the property. Returns the default value if the property doesn't exist or if the value is not a String or
     *         a Boolean instance
     */
    public boolean getBoolean(String aPropertyName, boolean defaultValue) {
        Object wValue = getObject(aPropertyName, Boolean.valueOf(defaultValue));
        if (wValue instanceof Boolean) {
            return ((Boolean) wValue).booleanValue();
        }
        if (wValue instanceof String) {
            return Boolean.parseBoolean((String) wValue);
        }
        log.error("getBoolean(): unable to retrieve a int value for [" + aPropertyName + "]. Returning default value [" + defaultValue
                + "]. object found=" + wValue);

        return defaultValue;
    }

    /**
     * ogattaz proposal
     * 
     * @param aPropertyName
     *            the name of the property
     * @return the int value of the property. Returns -1 if the property doesn't exist or if the value is not a String or an Integer
     *         instance
     */
    public int getInt(String aPropertyName) {
        return getInt(aPropertyName, -1);
    }

    /**
     * ogattaz proposal
     * 
     * @param aPropertyName
     *            the name of the property
     * @param defaultValue
     *            the default value
     * @return the int value of the property. Returns the default vakue if the property doesn't exist or if the value is not a String or an
     *         Integer instance
     */
    public int getInt(String aPropertyName, int defaultValue) {
        Object wValue = getObject(aPropertyName, Integer.valueOf(defaultValue));
        if (wValue instanceof Integer) {
            return ((Integer) wValue).intValue();
        }
        if (wValue instanceof String) {
            try {
                return Integer.parseInt((String) wValue);
            }
            catch (NumberFormatException ex) {}
        }
        log.error("getInt(): unable to retrieve a int value for [" + aPropertyName + "]. Returning default value [" + defaultValue
                + "]. object found=" + wValue);
        return defaultValue;
    }

    /**
     * Allow setting of properties as Objects
     * 
     * @param aPropertyName
     *            the name of the property
     * @param aPropertyValue the value of the property
     */
    public void setProperty(String aPropertyName, Object aPropertyValue) {
        put(aPropertyName, aPropertyValue);
    }

    public void dumpProps(int logLevel) {
        for (Enumeration<?> e = propertyNames(); e.hasMoreElements();) {
            String name = (String) e.nextElement();
            Object value = getObject(name);

            if (value == null) log.info("{}: 'null'", name);
            else               log.info("{}: ({}):'{}'", name, getObject(name).getClass().getSimpleName(), getObject(name).toString());
        }
    }

    public Object getInstance(String propName, Object defaultVal)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException
    {
        Object prop = getObject(propName, defaultVal);

        if (prop == null || prop.equals("")) {
            throw new InstantiationException("Property '" + propName + "' was not defined. Cannot instantiate.");
        }
        if (prop instanceof String) {
            return Class.forName(((String) prop).trim()).newInstance();
        }
        return prop;
    }

    public Object getInstance(String propName) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return getInstance(propName, null);
    }

    public ArrayList<?> getInstances(String propName, Object defaultVal)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException
    {
        Object val = getObject(propName, defaultVal);
        if (val == null) return null;
        if (val instanceof ArrayList) {
            return (ArrayList<?>) val;
        }
        else if (val instanceof String) {
            ArrayList<Object> retArr = new ArrayList<Object>();
            StringTokenizer tok = new StringTokenizer((String) val, ",");
            while (tok.hasMoreTokens())
                retArr.add(getInstance(tok.nextToken()));
            return retArr;
        }
        else {
            ArrayList<Object> retArr = new ArrayList<Object>();
            retArr.add(val);
            return retArr;
        }
    }

    public ArrayList<?> getInstances(String propName) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return getInstances(propName, null);
    }

    /**
     * Check if the value of the Property shall be redacted for security reasons. It converts the key to lower case,
     * and uses the SystemProperties.keywordsToRedact System Property to get the comma separated keywords 
     * to identify the Property. The default is 'password,pwd'.
     * 
     * @param key of the Property. It is converted to lower case for the comparison
     * @return if the value of the Property shall be redacted or not
     */
    private boolean propertiesToRedact(String key) {
        String keywordsToRedact = Gateway.getProperties().getString("SystemProperties.keywordsToRedact", "password,pwd");

        return StringUtils.containsAny(key.toLowerCase(), keywordsToRedact.split(","));
    }

    /**
     * Convert the properties to an XML using SystemProperties.xsd. It is based on MVEL2 template
     * 
     * @param processName the name of process of which properties are converted
     * @return the Outcome created from the Properties
     * @throws IOException could not load MVEL template from classpath
     * @throws ObjectNotFoundException SystemProperties Schema v0 was not found
     * @throws InvalidDataException The Schema is invalid
     */
    public Outcome convertToOutcome(String processName) throws IOException, InvalidDataException, ObjectNotFoundException {
        List<Map<String, Object>> props = new ArrayList<Map<String, Object>>();

        String templ = FileStringUtility.url2String(this.getClass().getResource("resources/templates/SystemProperties_xsd.tmpl"));
        CompiledTemplate expr = TemplateCompiler.compileTemplate(templ);

        for (Entry<Object, Object> entry: entrySet()) {
            String key = (String)entry.getKey();

            if (key.startsWith("//")) continue; //Skip commented lines

            Map<String, Object> prop = new HashMap<String, Object>();

            prop.put("Name", key);
            prop.put("SetInConfigFiles", true);

            if (propertiesToRedact(key)) prop.put("Value", "REDACTED");
            else                         prop.put("Value", entry.getValue());

            props.add(prop);
        }

        Map<Object, Object> vars = new HashMap<Object, Object>();
        vars.put("ProcessName", processName);
        vars.put("properties", props);

        String xml = (String)TemplateRuntime.execute(expr, vars);
        Schema xsd = LocalObjectLoader.getSchema("SystemProperties", 0);

        return new Outcome(xml, xsd);
    }
}
