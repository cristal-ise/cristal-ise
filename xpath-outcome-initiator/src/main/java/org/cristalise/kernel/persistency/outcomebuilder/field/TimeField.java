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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.cristalise.kernel.persistency.outcomebuilder.InvalidOutcomeException;
import org.cristalise.kernel.utils.Logger;
import org.json.JSONObject;

public class TimeField extends StringField {

    public TimeField() {
        super();
    }

    @Override
    public String getDefaultValue() {
        return "12:00:00";
    }

    @Override
    public String getNgDynamicFormsControlType() {
        return "TIMEPICKER";
    }

    @Override
    public JSONObject generateNgDynamicForms() {
        JSONObject date = getCommonFieldsNgDynamicForms();

        //date.put("meridian", false);
        date.put("showSeconds", true);
        getAdditionalConfigNgDynamicForms(date).put("utc", true);

        return date;
    }

    @Override
    public void setValue(Object value) throws InvalidOutcomeException {
        Logger.msg(0, "TimeField.setValue() - value=" + value + " class:" + value.getClass().getSimpleName());

        if (value instanceof String) {
            String sVal = (String) value;

            try {
                LocalDateTime ldt = null;

                if      (sVal.endsWith("Z")) ldt = LocalDateTime.ofInstant(Instant.parse(sVal), ZoneOffset.UTC);
                else if (sVal.contains("T")) ldt = LocalDateTime.parse(sVal);

                if (ldt != null) {
                    DateTimeFormatter dtf  = DateTimeFormatter.ofPattern("HH:mm:ss");
                    setData(dtf.format(ldt.toLocalTime()));
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
