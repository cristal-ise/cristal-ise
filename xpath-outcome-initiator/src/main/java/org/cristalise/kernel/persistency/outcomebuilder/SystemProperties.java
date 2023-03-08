/**
 * This file is part of the CRISTAL-iSE XPath Outcome Initiator module.
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
package org.cristalise.kernel.persistency.outcomebuilder;

import org.cristalise.kernel.utils.SystemPropertyOperations;

import lombok.Getter;

/**
 * Defines all SystemProperties that are supported in the kernel to configure the behavior of the
 * application. Due to the limitation of javadoc, the actual usable string cannot be shown easily,
 * therefore replace underscores with dots to get the actual System Property:
 * 
 * <pre>
 *   SimpleType_DefaultValues => SimpleType.DefaultValues
 * </pre>
 * 
 * @see #Webui_autoComplete_default
 * @see #Webui_decimal_generateDefaultPattern
 * @see #Webui_decimal_separator
 * @see #Webui_format_date
 * @see #Webui_format_date_default
 * @see #Webui_format_datetime
 * @see #Webui_format_time
 * @see #Webui_inputField_boolean_defaultValue
 * @see #Webui_inputField_date_defaultValue
 * @see #Webui_inputField_dateTime_defaultValue
 * @see #Webui_inputField_decimal_defaultValue
 * @see #Webui_inputField_integer_defaultValue
 * @see #Webui_inputField_string_defaultValue
 * @see #Webui_inputField_time_defaultValue
 */
@Getter
public enum SystemProperties implements SystemPropertyOperations{

    Webui_autoComplete_default("Webui.autoComplete.default", "off"),
    Webui_format_date_default("Webui.format.date.default", "yy-mm-dd"),
    Webui_format_date("Webui.format.date", "yyyy-MM-dd"),
    Webui_format_datetime("Webui.format.datetime", "yyyy-MM-dd'T'HH:mm:ss"),
    Webui_format_time("Webui.format.time", "HH:mm:ss"),
    /**
     * Specifies the default value for boolean input fields. Default is 'false'.
     */
    Webui_inputField_boolean_defaultValue("Webui.inputField.boolean.defaultValue", false),
    Webui_inputField_date_defaultValue("Webui.inputField.date.defaultValue"),
    Webui_inputField_dateTime_defaultValue("Webui.inputField.dateTime.defaultValue"),
    Webui_inputField_decimal_defaultValue("Webui.inputField.decimal.defaultValue", 0.0d),
    Webui_inputField_integer_defaultValue("Webui.inputField.integer.defaultValue", 0),
    Webui_inputField_string_defaultValue("Webui.inputField.string.defaultValue", ""),
    Webui_inputField_time_defaultValue("Webui.inputField.time.defaultValue"),
    Webui_decimal_separator("Webui.decimal.separator", "."),
    Webui_decimal_generateDefaultPattern("Webui.decimal.generateDefaultPattern", false);

    private Object defaultValue;
    private String systemPropertyName;

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
