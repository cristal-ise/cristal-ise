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
package org.cristalise.kernel.persistency.outcomebuilder.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcomebuilder.Field;
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder;
import org.cristalise.kernel.process.Gateway;
import org.json.JSONObject;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for generated Scripts, Script development and testing. JSONObject, Outcome and Map are the
 * 3 major formats the are used in the framework to handle outcome, and this class provides consistent type conversion
 * for field values.
 * <br>
 * <br>
 * <b>Valid value</b> is not null or in case of String type it is not blank. This is based the convention that
 * the XML based Outcome handles values of type String only.
 */
@Slf4j
public class OutcomeUtils {
    public static final String webuiDateFormat     = Gateway.getProperties().getString("Webui.format.date",     "yyyy-MM-dd");
    public static final String webuiDateTimeFormat = Gateway.getProperties().getString("Webui.format.datetime", "yyyy-MM-dd'T'HH:mm:ss");
    public static final String webuiTimeFormat     = Gateway.getProperties().getString("Webui.format.time",     "HH:mm:ss");

    /**
     * Checks if the input object has the field or not
     * 
     * @param input  either JSONObject, Outcome or Map
     * @param key the field to check
     * @return true if the input has the field regardless its value
     */
    public static boolean hasField(Object input, String key) {
        if (input instanceof JSONObject) {
            return ((JSONObject) input).has(key);
        }
        else if (input instanceof Outcome) {
            return ((Outcome) input).hasField(key);
        }
        else if (input instanceof Map) {
            return ((Map<?, ?>) input).containsKey(key);
        }
        else {
            throw new IllegalArgumentException("Does not handle input with type '" + input.getClass().getName() + "'");
        }
    }

    /**
     * Checks if the input object has the field and a valid value.
     * 
     * @param input  either JSONObject, Outcome or Map
     * @param key the field to check
     * @return true if the input has the field with a valid value
     */
    public static boolean hasValue(Object input, String key) {
        if (!hasField(input, key)) return false;

        if (input instanceof JSONObject) {
           JSONObject json = (JSONObject) input;
           Object value = json.opt(key);

           if (json.isNull(key)) return false;

           return value instanceof String ? StringUtils.isNotBlank(value.toString()) : true;
        }
        else if (input instanceof Outcome) {
            Outcome outcome = (Outcome) input;
            String value = outcome.getField(key);
            return StringUtils.isNotBlank(value);
        }
        else if (input instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) input;
            Object value = map.get(key);

            if (value == null) return false;

            return value instanceof String ? StringUtils.isNotBlank(value.toString()) : true;
        }
        else {
            throw new IllegalArgumentException("Does not handle input with type '" + input.getClass().getName() + "'");
        }
    }

    /**
     * Uses the OutcomeBuilder (i.e. Schema) to return a value in the correct type
     * 
     * @param input
     * @param key
     * @param builder
     * @return
     */
    public static Object getValueOrNull(Object input, String key, OutcomeBuilder builder) {
        Field f = (Field)builder.findChildStructure(key);
        Class<?> fieldClazz = f.getJavaType();

        if (fieldClazz.equals(String.class)) {
            return getStringOrNull(input, key);
        }
        else if(fieldClazz.equals(Boolean.class)) {
            return getBooleanOrNull(input, key);
        }
        else if(fieldClazz.equals(BigInteger.class)) {
            return getBigIntegerOrNull(input, key);
        }
        else if(fieldClazz.equals(BigDecimal.class)) {
            return getBigDecimalOrNull(input, key);
        }
        else if(fieldClazz.equals(LocalDate.class)) {
            return getLocalDateOrNull(input, key);
        }
        else if(fieldClazz.equals(OffsetDateTime.class)) {
            return getOffsetDateTimeOrNull(input, key);
        }
        else if(fieldClazz.equals(OffsetTime.class)) {
            return getOffsetTimeOrNull(input, key);
        }
        else {
            throw new IllegalArgumentException("Uncovered class '"+ fieldClazz + "' for field:'"+key+"'");
        }
    }

    /**
     * Converts the value of the given field to String or null.
     *
     * @param input either a JSONObject or an Outcome
     * @param key the field to be converted
     * @return value of the given field in String or null
     */
    public static String getStringOrNull(Object input, String key) {
        if (input instanceof JSONObject) {
            JSONObject json = (JSONObject) input;
            if (json.has(key) && !json.isNull(key)) {
                Object value = json.get(key);

                if (value instanceof String && "".equals(value)) return null;

                return json.getString(key);
            }
        }
        else if (input instanceof Outcome) {
            String value = ((Outcome) input).getField(key);

            if (StringUtils.isNotBlank(value)) return value;
        }
        else if (input instanceof Map) {
            String value = (String) ((Map<?, ?>) input).get(key);

            if (StringUtils.isNotBlank(value)) return value;
        }
        else {
            throw new IllegalArgumentException("Does not handle input with type '" + input.getClass().getName() + "'");
        }
        return null;
    }

    /**
     * Converts the value of the given field to BigDecimal or null. No rounding is done.
     *
     * @param input either JSONObject, Outcome or Map
     * @param key the field to be converted
     * @return value of the given field in BigDecimal or null
     */
    public static BigDecimal getBigDecimalOrNull(Object input, String key) {
        return getBigDecimalOrNull(input, key, -1, null);
    }

    /**
     * Converts the value of the given field to BigDecimal or null. Half-up rounding is done using the
     * given scale.
     *
     * @param input either a JSONObject or an Outcome
     * @param key the field to be converted
     * @param scale to be used for half-up rounding
     * @return value of the given field in BigDecimal or null
     */
    public static BigDecimal getBigDecimalOrNull(Object input, String key, int scale) {
        return getBigDecimalOrNull(input, key, scale, RoundingMode.HALF_UP);
    }

    /**
     * Converts the value of the given field to BigDecimal or null. The given rounding is done using
     * the given scale.
     *
     * @param input either a JSONObject or an Outcome
     * @param key the field to be converted
     * @param scale to be used for the given rounding
     * @param rounding to be used
     * @return value of the given field in BigDecimal or null
     */
    public static BigDecimal getBigDecimalOrNull(Object input, String key, int scale, RoundingMode rounding) {
        if (input instanceof JSONObject) {
            BigDecimal value = ((JSONObject) input).optBigDecimal(key, null);

            if (value != null) {
                if (scale >= 0) return value.setScale(scale, rounding);
                else            return value;
            }
        }
        else if (input instanceof Outcome) {
            String value = ((Outcome) input).getField(key);

            if (StringUtils.isNotBlank(value)) {
                if (scale >= 0) return new BigDecimal(value).setScale(scale, rounding);
                else            return new BigDecimal(value);
            }
        }
        else if (input instanceof Map) {
            String value = (String) ((Map<?, ?>) input).get(key);

            if (StringUtils.isNotBlank(value)) {
                if (scale >= 0) return new BigDecimal(value).setScale(scale, rounding);
                else            return new BigDecimal(value);
            }
        }
        else {
            throw new IllegalArgumentException("Does not handle input with type '" + input.getClass().getName() + "'");
        }
        return null;
    }

    /**
     * Converts the value of the given field to BigInteger or null.
     *
     * @param input either a JSONObject or an Outcome
     * @param key the field to be converted
     * @return value of the given field in BigInteger or null
     */
    public static BigInteger getBigIntegerOrNull(Object input, String key) {
        if (input instanceof JSONObject) {
            JSONObject json = (JSONObject) input;
            if (json.has(key) && !json.isNull(key)) {
                Object value = json.get(key);

                if (value instanceof String && "".equals(value)) return null;

                return json.getBigInteger(key);
            }
        }
        else if (input instanceof Outcome) {
            String value = ((Outcome) input).getField(key);

            if (StringUtils.isNotBlank(value)) return new BigInteger(value);
        }
        else if (input instanceof Map) {
            String value = (String) ((Map<?, ?>) input).get(key);

            if (StringUtils.isNotBlank(value)) return new BigInteger(value);
        }
        else {
            throw new IllegalArgumentException("Does not handle input with type '" + input.getClass().getName() + "'");
        }
        return null;
    }

    /**
     * Converts the value of the given field to Boolean or null.
     *
     * @param input either a JSONObject or an Outcome
     * @param key the field to be converted
     * @return value of the given field in Boolean or null
     */
    public static Boolean getBooleanOrNull(Object input, String key) {
        if (input instanceof JSONObject) {
            JSONObject json = (JSONObject) input;
            if (json.has(key) && !json.isNull(key)) {
                Object value = json.get(key);

                if (value instanceof String && "".equals(value)) return null;

                return json.getBoolean(key);
            }
        }
        else if (input instanceof Outcome) {
            String value = ((Outcome) input).getField(key);

            if (StringUtils.isNotBlank(value)) return Boolean.valueOf(value);
        }
        else if (input instanceof Map) {
            String value = (String) ((Map<?, ?>) input).get(key);

            if (StringUtils.isNotBlank(value)) return Boolean.valueOf(value);
        }
        else {
            throw new IllegalArgumentException("Does not handle input with type '" + input.getClass().getName() + "'");
        }
        return null;
    }

    /**
     * Converts the value of the given field to LocalDate or null. If the input is a JSONObject
     * it uses webui specific pattern, configurable with 'Webui.format.date' system property,
     * default is 'yyyy-MM-dd'. If the input is an Outcome or Map the ISO_LOCAL_DATE format is used.
     *
     * @param input either a JSONObject, an Outcome or a Map
     * @param key the field to be converted
     * @return value of the given field in LocalDate or null
     */
    public static LocalDate getLocalDateOrNull(Object input, String key) {
        if (input instanceof JSONObject) {
            JSONObject json = (JSONObject) input;
            if (json.has(key) && !json.isNull(key)) {
                Object value = json.get(key);

                if (value instanceof String) {
                  String v = (String)value;
                  DateTimeFormatter dtf = DateTimeFormatter.ofPattern(webuiDateFormat);

                  if (StringUtils.isNotBlank(v)) return LocalDate.parse(v, dtf);
                }
                else {
                    log.warn("getLocalDateOrNull(key:{}) - json value is not a String, dropping it", key);
                }
            }
        }
        else if (input instanceof Outcome) {
            String value = ((Outcome) input).getField(key);

            if (StringUtils.isNotBlank(value)) return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        }
        else if (input instanceof Map) {
            String value = (String) ((Map<?, ?>) input).get(key);

            if (StringUtils.isNotBlank(value)) return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        }
        else {
            throw new IllegalArgumentException("Does not handle input with type '" + input.getClass().getName() + "'");
        }
        return null;
    }

    /**
     * Converts the value of the given field to LocalDateTime or null. If the input is a JSONObject
     * it uses webui specific pattern, configurable with 'Webui.format.datetime' system property,
     * default is 'yyyy-MM-dd'T'HH:mm:ss'. If the input is an Outcome or Map the ISO_LOCAL_DATETIME
     * format is used.
     *
     * @param input either a JSONObject, an Outcome or a Map
     * @param key the field to be converted
     * @return value of the given field in LocalDateTime or null
     */
    public static LocalDateTime getLocalDateTimeOrNull(Object input, String key) {
        if (input instanceof JSONObject) {
            JSONObject json = (JSONObject) input;
            if (json.has(key) && !json.isNull(key)) {
                Object value = json.get(key);

                if (value instanceof String) {
                    String v = (String)value;
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern(webuiDateTimeFormat);

                    if (StringUtils.isNotBlank(v)) return LocalDateTime.parse(v, dtf);
                }
                else {
                    log.warn("getLocalDateTimeOrNull(key:{}) - json value is not a String, dropping it", key);
                }
            }
        }
        else if (input instanceof Outcome) {
            String value = ((Outcome) input).getField(key);

            if (StringUtils.isNotBlank(value)) return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        else if (input instanceof Map) {
            String value = (String) ((Map<?, ?>) input).get(key);

            if (StringUtils.isNotBlank(value)) return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        else {
            throw new IllegalArgumentException("Does not handle input with type '" + input.getClass().getName() + "'");
        }
        return null;
    }

    /**
     * Converts the value of the given field to OffsetDateTime or null. If the input is a JSONObject
     * it uses webui specific pattern, configurable with 'Webui.format.datetime' system property,
     * default is 'yyyy-MM-dd'T'HH:mm:ss'. If the input is an Outcome or Map the ISO_OFFSET_DATE_TIME
     * format is used.
     *
     * @param input either a JSONObject, an Outcome or a Map
     * @param key the field to be converted
     * @return value of the given field in OffsetDateTime or null
     */
    public static OffsetDateTime getOffsetDateTimeOrNull(Object input, String key) {
        if (input instanceof JSONObject) {
            JSONObject json = (JSONObject) input;
            if (json.has(key) && !json.isNull(key)) {
                Object value = json.get(key);

                if (value instanceof String) {
                    String v = (String)value;
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern(webuiDateTimeFormat);

                    if (StringUtils.isNotBlank(v)) {
                        OffsetDateTime odt = OffsetDateTime.now (ZoneId.systemDefault ());
                        ZoneOffset zoneOffset = odt.getOffset ();
                        return LocalDateTime.parse(v, dtf).atOffset(zoneOffset);
                    }
                }
                else {
                    log.warn("getOffsetDateTimeOrNull(key:{}) - json value is not a String, dropping it", key);
                }
            }
        }
        else if (input instanceof Outcome) {
            String value = ((Outcome) input).getField(key);

            if (StringUtils.isNotBlank(value)) return OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
        else if (input instanceof Map) {
            String value = (String) ((Map<?, ?>) input).get(key);

            if (StringUtils.isNotBlank(value)) return OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
        else {
            throw new IllegalArgumentException("Does not handle input with type '" + input.getClass().getName() + "'");
        }
        return null;
    }

    /**
     * Converts the value of the given field to OffsetTime or null. If the input is a JSONObject
     * it uses webui specific pattern, configurable with 'Webui.format.time' system property,
     * default is 'HH:mm:ss'. If the input is an Outcome or Map the ISO_OFFSET_TIME format is used.
     *
     * @param input either a JSONObject, an Outcome or a Map
     * @param key the field to be converted
     * @return value of the given field in OffsetTime or null
     */
    public static OffsetTime getOffsetTimeOrNull(Object input, String key) {
        if (input instanceof JSONObject) {
            JSONObject json = (JSONObject) input;
            if (json.has(key) && !json.isNull(key)) {
                Object value = json.get(key);

                if (value instanceof String) {
                    String v = (String)value;
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern(webuiTimeFormat);

                    if (StringUtils.isNotBlank(v)) {
                        OffsetDateTime odt = OffsetDateTime.now (ZoneId.systemDefault ());
                        ZoneOffset zoneOffset = odt.getOffset ();
                        return LocalTime.parse(v, dtf).atOffset(zoneOffset);
                    }
               }
                else {
                    log.warn("getOffsetTimeOrNull(key:{}) - json value is not a String, dropping it", key);
                }
            }
        }
        else if (input instanceof Outcome) {
            String value = ((Outcome) input).getField(key);

            if (StringUtils.isNotBlank(value)) return OffsetTime.parse(value, DateTimeFormatter.ISO_OFFSET_TIME);
        }
        else if (input instanceof Map) {
            String value = (String) ((Map<?, ?>) input).get(key);

            if (StringUtils.isNotBlank(value)) return OffsetTime.parse(value, DateTimeFormatter.ISO_OFFSET_TIME);
        }
        else {
            throw new IllegalArgumentException("Does not handle input with type '" + input.getClass().getName() + "'");
        }
        return null;
    }
}
