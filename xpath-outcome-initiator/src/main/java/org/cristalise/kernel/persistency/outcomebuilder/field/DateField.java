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

import static org.cristalise.kernel.persistency.outcomebuilder.SystemProperties.Webui_format_date_default;
import static org.cristalise.kernel.persistency.outcomebuilder.SystemProperties.Webui_inputField_date_defaultValue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.cristalise.kernel.persistency.outcomebuilder.InvalidOutcomeException;
import org.json.JSONObject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DateField extends StringField {
    public DateField() {
        super();
        javaType = LocalDate.class;
    }

    @Override
    public String getDefaultValue() {
        return Webui_inputField_date_defaultValue.getString("");
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
    public JSONObject generateNgDynamicForms(Map<String, Object> inputs, boolean withLayout) {
        JSONObject date = getCommonFieldsNgDynamicForms(withLayout);

        date.put("format", Webui_format_date_default.getString());

        JSONObject additional = getAdditionalConfigNgDynamicForms(date);
        additional.put("showButtonBar", true);
        
        return date;
    }

    @Override
    public void setValue(Object value) throws InvalidOutcomeException {
        log.info("setValue() - value=" + value + " class:" + value.getClass().getSimpleName());

        if (value instanceof String) {
            String sVal = (String) value;

            try {
                ZonedDateTime zdt = null;

                if      (sVal.endsWith("Z")) zdt = ZonedDateTime.ofInstant(Instant.parse(sVal), ZoneOffset.UTC);
                else if (sVal.contains("T")) zdt = ZonedDateTime.parse(sVal);

                if (zdt != null) {
                    log.trace("setValue() - ZonedDateTime:%s", zdt);

                    // now the local date can be extracted
                    setData(zdt.toLocalDate().toString());
                }
                else
                    setData(sVal);
            }
            catch (DateTimeParseException e) {
                log.error("", e);
                throw new InvalidOutcomeException(e.getMessage());
            }
        }
        else
            setData(value.toString());
    }
}
