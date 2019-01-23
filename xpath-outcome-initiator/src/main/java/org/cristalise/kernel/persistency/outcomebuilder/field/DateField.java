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
package org.cristalise.kernel.persistency.outcomebuilder.field;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.cristalise.kernel.persistency.outcomebuilder.InvalidOutcomeException;
import org.cristalise.kernel.utils.Logger;
import org.json.JSONObject;

public class DateField extends StringField {
    public static final String javaTimeDateFormat = "yyyy-MM-dd";
    public static final String primeNGDateFormat = "yy-mm-dd";

    public DateField() {
        super();
    }

    @Override
    public String getDefaultValue() {
        return "";
    }

    @Override
    public String getValue(String valueTemplate) {
        if (valueTemplate.equals("now"))
            return LocalDateTime.now().format(DateTimeFormatter.ISO_DATE);
        else
            return valueTemplate;
    }

    @Override
    public String getNgDynamicFormsControlType() {
        return "DATEPICKER";
    }

    @Override
    public JSONObject generateNgDynamicForms(Map<String, Object> inputs) {
        JSONObject date = getCommonFieldsNgDynamicForms();

        date.put("format", primeNGDateFormat);

        JSONObject additional = getAdditionalConfigNgDynamicForms(date);
        additional.put("showButtonBar", true);

        return date;
    }

    @Override
    public void setValue(Object value) throws InvalidOutcomeException {
        Logger.msg(0, "DateField.setValue() - value=" + value + " class:" + value.getClass().getSimpleName());

        if (value instanceof String) {
            String sVal = (String) value;

            try {
                ZonedDateTime zdt = null;

                if      (sVal.endsWith("Z")) zdt = ZonedDateTime.ofInstant(Instant.parse(sVal), ZoneOffset.UTC);
                else if (sVal.contains("T")) zdt = ZonedDateTime.parse(sVal);

                if (zdt != null) {
                    Logger.msg(8,"DateField.setValue() - ZonedDateTime:%s", zdt);

                    // now the local date can be extracted
                    setData(zdt.toLocalDate().toString());
                }
                else
                    setData(sVal);
            }
            catch (DateTimeParseException e) {
                Logger.error(e);
                throw new InvalidOutcomeException(e.getMessage());
            }
        }
        else
            setData(value.toString());
    }
}
