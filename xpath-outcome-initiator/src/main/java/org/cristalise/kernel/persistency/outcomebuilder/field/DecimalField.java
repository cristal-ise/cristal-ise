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
import java.util.regex.PatternSyntaxException;

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

    /**
     * Compute validation regex pattern in this order of precedence:
     * <pre>
     * 1. use pattern as is if available
     * 2. generate pattern from precision and scale if available
     * 3. generate pattern from totalDigits and fractionDigits if available
     * 4. set default pattern
     * </pre>
     */
    @Override
    public void setNgDynamicFormsValidators(JSONObject validators) {
        super.setNgDynamicFormsValidators(validators);

        if (StringUtils.isBlank(pattern)) {
            if (StringUtils.isNotBlank(precision) || StringUtils.isNotBlank(scale)) {
                pattern = generatePrecisionScalePattern();
            }
            else {
                //this case also generates the default pattern
                pattern = generateTotalFractionDigitsPattern();
            }
        }

        if (StringUtils.isNotBlank(pattern)) validators.put("pattern", pattern);
    }

    private String generatePattern(Integer precisionNumber, boolean precisionSmaller, Integer scaleNumber, boolean scaleSmaller) {
        //locale specific separators could be used, but it should be based on the locale of the browser
        //char separator = new DecimalFormatSymbols(Locale.getDefault(Locale.Category.FORMAT)).getDecimalSeparator();
        char separator = Gateway.getProperties().getString("Webui.decimal.separator", ".").charAt(0);

        if (precisionNumber == null && scaleNumber == null) {
            if (Gateway.getProperties().getBoolean("Webui.decimal.generateDefaultPattern", false)) {
                //default validator for any decimal field
                if (StringUtils.isBlank(errmsg)) errmsg = "Invalid decimal number";
                return "^-?\\d+\\" + separator + "?\\d*$";
            }
            else
                return null;
        }
        else if (precisionNumber != null) {
            if (scaleNumber == null) {
                if (precisionSmaller) {
                    if (StringUtils.isBlank(errmsg)) errmsg = "Use max "+precisionNumber+" digits";
                    return "^-?\\d{0," + precisionNumber + "}$";
                }
                else {
                    if (StringUtils.isBlank(errmsg)) errmsg = "Use exactly "+precisionNumber+" digits";
                    return "^-?\\d{" + precisionNumber + "}$";
                }
            }
            else {
                if (precisionSmaller && scaleSmaller) {
                    // It is not simple (impossible?) to write regexp that accepts 1234.5 and 123.45, 
                    // but not 1234.56 - this should be covered with validation code/expression.
                    if (StringUtils.isBlank(errmsg)) errmsg = "Use max "+precisionNumber+" digits with max "+scaleNumber+" decimal places";
                    return "^-?\\d{0," + precisionNumber + "}\\" + separator + "?\\d{0," + scaleNumber + "}$";
                }
                else if (precisionSmaller && !scaleSmaller) {
                    if (StringUtils.isBlank(errmsg)) errmsg = "Use max "+precisionNumber+" digits with exactly "+scaleNumber+" decimal places";
                    return "^-?\\d{0," + (precisionNumber - scaleNumber) + "}\\" + separator + "\\d{" + scaleNumber + "}$";
                }
                else if (!precisionSmaller && !scaleSmaller) {
                    if (StringUtils.isBlank(errmsg)) errmsg = "Use exactly "+precisionNumber+" digits with exactly "+scaleNumber+" decimal places";
                    return "^-?\\d{" + (precisionNumber - scaleNumber) + "}\\" + separator + "\\d{" + scaleNumber + "}$";
                }
            }
        }
        else {
            if (StringUtils.isBlank(errmsg)) errmsg = "Ivalid decimal number";

            if (scaleSmaller) {
                if (StringUtils.isBlank(errmsg)) errmsg = "Use max "+precisionNumber+" decimal places";
                return "^-?\\d+\\" + separator + "?\\d{0," + scaleNumber + "}$";
            }
            else {
                if (StringUtils.isBlank(errmsg)) errmsg = "Use exactly "+precisionNumber+" decimal places";
                return "^-?\\d+\\" + separator + "\\d{" + scaleNumber + "}$";
            }
        }

        throw new PatternSyntaxException("Cannot generate regexp from the inputs", "unknows", -1);
    }

    private String generatePrecisionScalePattern() {
        Integer precisionNumber = null;
        boolean precisionSmaller = false;
        Integer scaleNumber = null;
        boolean scaleSmaller = false;

        if (StringUtils.isNotBlank(precision)) {
            if (StringUtils.isNumeric(precision))  {
                precisionNumber = Integer.parseInt(precision);
            }
            else if (precision.endsWith("-"))  {
                precisionNumber = Integer.parseInt(precision.substring(0, precision.indexOf("-")));
                precisionSmaller = true;
            }
        }

        if (StringUtils.isNotBlank(scale)) {
            if (StringUtils.isNumeric(scale))  {
                scaleNumber = Integer.parseInt(scale);
            }
            else if (scale.endsWith("-"))  {
                scaleNumber = Integer.parseInt(scale.substring(0, scale.indexOf("-")));
                scaleSmaller = true;
            }
        }

        return generatePattern(precisionNumber, precisionSmaller, scaleNumber, scaleSmaller);
    }

    private String generateTotalFractionDigitsPattern() {
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

        return generatePattern(totalDigits, true, fractionDigits, true);
    }

    @Override
    public boolean hasValidator() {
        return super.hasValidator() || StringUtils.isNotBlank(scale);
    }
}
