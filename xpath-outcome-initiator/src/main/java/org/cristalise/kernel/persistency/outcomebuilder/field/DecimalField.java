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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.persistency.outcomebuilder.InvalidOutcomeException;
import org.cristalise.kernel.process.Gateway;
import org.exolab.castor.xml.schema.Facet;
import org.json.JSONObject;

public class DecimalField extends NumberField {

    private static final String[] strFields = {"mask", "placeholder"};
    private static final String[] excFields = {"pattern", "errmsg", "precision", "scale"};

    public DecimalField() {
        super(Arrays.asList(strFields), Arrays.asList(excFields));
    }

    /**
     * precision is defined in NumberField
     */
    String scale;

    @Override
    public String getDefaultValue() {
        return "0.0";
    }

    @Override
    public void setValue(Object value) throws InvalidOutcomeException {
        if (value instanceof BigDecimal) {
            BigDecimal decimalVal = (BigDecimal)value;
            decimalVal = decimalVal.setScale(2, RoundingMode.HALF_UP);
            super.setData(decimalVal.toString());
        }

        super.setData(value.toString());
    }

    @Override
    protected void setAppInfoDynamicFormsExceptionValue(String name, String value) {
        super.setAppInfoDynamicFormsExceptionValue(name, value);

        if (name.equals("precision")) {
            precision = value;
        }
        else if (name.equals("scale")) {
            scale = value;
        }
    }

    @Override
    public void setNgDynamicFormsValidators(JSONObject validators) {
        //locale specific separators could be used, but it should be based on the locale of the browser
        //char separator = new DecimalFormatSymbols(Locale.getDefault(Locale.Category.FORMAT)).getDecimalSeparator();
        char separator = Gateway.getProperties().getString("Webui.decimal.separator", ".").charAt(0);

        super.setNgDynamicFormsValidators(validators);

        if (StringUtils.isNotBlank(pattern)) {
            validators.put("pattern", pattern);
        }
        else {
            Integer totalDigits = null;
            Integer fractionDigits = null;

            if(contentType.hasFacet(Facet.TOTALDIGITS)) {
                Facet f = contentType.getFacet(Facet.TOTALDIGITS);
                totalDigits = Integer.valueOf(f.getValue());
            }

            if(contentType.hasFacet(Facet.FRACTIONDIGITS)) {
                Facet f = contentType.getFacet(Facet.FRACTIONDIGITS);
                fractionDigits = Integer.valueOf(f.getValue());
            }

            if (totalDigits == null && fractionDigits == null) {
                //default validator for any decimal field
                validators.put("pattern", "^\\d+\\" + separator + "?\\d+$");
            }
            else if (totalDigits != null) {
                if (fractionDigits == null) {
                    validators.put("pattern", "^\\d{0," + totalDigits + "}$");
                }
                else
                    validators.put("pattern", "^\\d{0," + (totalDigits - fractionDigits) + "}\\" + separator + "?\\d{0," + fractionDigits + "}$");
            }
            else {
                validators.put("pattern", "^\\d+\\" + separator + "?\\d{0," + fractionDigits + "}$");
            }
        }
    }
}
