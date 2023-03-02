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

import static org.apache.commons.lang3.StringUtils.equalsAny;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.cristalise.kernel.persistency.outcomebuilder.SystemProperties.Webui_format_date;
import static org.cristalise.kernel.persistency.outcomebuilder.SystemProperties.Webui_format_datetime;
import static org.cristalise.kernel.persistency.outcomebuilder.SystemProperties.Webui_format_time;

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
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.apache.commons.beanutils.converters.BigDecimalConverter;
import org.apache.commons.beanutils.converters.BigIntegerConverter;
import org.apache.commons.beanutils.converters.BooleanConverter;
import org.apache.commons.beanutils.converters.StringConverter;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcomebuilder.Field;
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder;
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeStructure;
import org.json.JSONObject;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for generated Scripts, Script development and testing. JSONObject, Outcome and Map are the
 * 3 major formats the are used in the framework to handle outcome, and this class provides consistent type conversion
 * for fields and their valid values.
 * <br><br>
 * <b>Valid value:</b> {@link OutcomeUtils#hasValidNotBlankValue(String)}
 */
@Slf4j
public class OutcomeUtils {
    public static final String webuiDateFormat     = Webui_format_date.getString();
    public static final String webuiDateTimeFormat = Webui_format_datetime.getString();
    public static final String webuiTimeFormat     = Webui_format_time.getString();

    /**
     * Check if the String has a not blank valid value, i.e. it does not equal to 'string' nor 'null'.
     * This is based the convention that the XML based Outcome handles values of type String only.
     * <br><br>
     * Note that <i>'Empty'</i> OutcomeInitiator creates one entry for optional fields. 
     * If this is not required it is considered invalid.
     * 
     * @param value the String to be checked
     * @return true if the String in not blank and it does not equal to 'string' nor 'null' otherwise returns false
     * 
     * @see org.apache.commons.lang3.StringUtils#isNotBlank
     * @see org.apache.commons.lang3.StringUtils#equalsAny
     */
    public static boolean hasValidNotBlankValue(String value) {
        return isNotBlank(value) && ! equalsAny(value, "string", "null");
    }

    /**
     * Checks if the input object has the field or not
     * 
     * @param input  either JSONObject, Outcome or Map
     * @param key the field to check
     * @return true if the input has the field regardless its value otherwise false
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
     * Returns the value if the input object has the field and a valid value.
     * <br>
     * <b>Valid value:</b> {@link OutcomeUtils#hasValidNotBlankValue(String)}
     * 
     * @param input either JSONObject, Outcome or Map
     * @param key the field to check
     * @return the value if the input has the field with a valid value otherwise returns null
     */
    public static Object getValueOrNull(Object input, String key) {
        if (!hasField(input, key)) return null;

        Object value = null;

        if (input instanceof Map) {
            value = ((Map<?, ?>) input).get(key);
        }
        else if (input instanceof JSONObject) {
            JSONObject json = (JSONObject) input;
            if (! json.isNull(key)) value = json.get(key);
        }
        else if (input instanceof Outcome) {
            value = ((Outcome) input).getField(key);
        }
        else {
            throw new IllegalArgumentException("Does not handle input with type '" + input.getClass().getName() + "'");
        }

        if (value != null && value instanceof String && ! hasValidNotBlankValue((String)value)) {
            value = null;
        }

        return value;
    }

    /**
     * Checks if the input object has the field and a valid value.
     * <br>
     * <b>Valid value:</b> {@link OutcomeUtils#hasValidNotBlankValue(String)}
     * 
     * @param input either JSONObject, Outcome or Map
     * @param key the field to check
     * @return true if the input has the field with a valid value otherwise returns false
     */
    public static boolean hasValue(Object input, String key) {
        return getValueOrNull(input, key) != null;
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
        OutcomeStructure childStruct = builder.findChildStructure(key);
        if (childStruct == null) return null;

        if (!(childStruct instanceof Field)) throw new IllegalArgumentException("Key '"+key+"' is not class Field (" +input.getClass().getName()+")");

        Class<?> fieldClazz = ((Field)childStruct).getJavaType();

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
     * Returns the valid value of the given field as String or null. Based on apache beanutils StringConverter.
     * <br>
     * <b>Valid value:</b> {@link OutcomeUtils#hasValidNotBlankValue(String)}
     *
     * @param input either a JSONObject or an Outcome
     * @param key the field to be converted
     * @return value of the given field as String or null
     * 
     * @see org.apache.commons.beanutils.converters.StringConverter
     */
    public static String getStringOrNull(Object input, String key) {
        StringConverter converter = new StringConverter(null);

        Object value = getValueOrNull(input, key);

        if (value != null) return converter.convert(String.class, value);
        else               return null;
    }

    /**
     * Returns the valid value of the given field as BigDecimal or null. No rounding is done.
     * Based on apache beanutils BigDecimalConverter which converts Boolean.TRUE to 1 and Boolean.FALSE to 0.
     * <br>
     * <b>Valid value:</b> {@link OutcomeUtils#hasValidNotBlankValue(String)}
     *
     * @param input either JSONObject, Outcome or Map
     * @param key the field to be converted
     * @return value of the given field as BigDecimal or null
     * 
     * @see org.apache.commons.beanutils.converters.BigDecimalConverter
     */
    public static BigDecimal getBigDecimalOrNull(Object input, String key) {
        return getBigDecimalOrNull(input, key, -1, null);
    }

    /**
     * Returns the valid value of the given field as BigDecimal or null. Half-up rounding is done using the
     * given scale.
     * Based on apache beanutils BigDecimalConverter which converts Boolean.TRUE to 1 and Boolean.FALSE to 0.
     * <br>
     * <b>Valid value:</b> {@link OutcomeUtils#hasValidNotBlankValue(String)}
     *
     * @param input either a JSONObject or an Outcome
     * @param key the field to be converted
     * @param scale to be used for half-up rounding
     * @return value of the given field as BigDecimal or null
     * 
     * @see org.apache.commons.beanutils.converters.BigDecimalConverter
     */
    public static BigDecimal getBigDecimalOrNull(Object input, String key, int scale) {
        return getBigDecimalOrNull(input, key, scale, RoundingMode.HALF_UP);
    }

    /**
     * Returns the valid value of the given field as BigDecimal or null. The given rounding is done using
     * the given scale.
     * Based on apache beanutils BigDecimalConverter which converts Boolean.TRUE to 1 and Boolean.FALSE to 0.
     * <br>
     * <b>Valid value:</b> {@link OutcomeUtils#hasValidNotBlankValue(String)}
     *
     * @param input either a JSONObject or an Outcome
     * @param key the field to be converted
     * @param scale to be used for the given rounding
     * @param rounding to be used
     * @return value of the given field as BigDecimal or null
     * 
     * @see org.apache.commons.beanutils.converters.BigDecimalConverter
     */
    public static BigDecimal getBigDecimalOrNull(Object input, String key, int scale, RoundingMode rounding) {
        BigDecimalConverter converter = new BigDecimalConverter(null);

        Object value = getValueOrNull(input, key);

        if (value != null) {
            if (scale >= 0) return converter.convert(BigDecimal.class, value).setScale(scale, rounding);
            else            return converter.convert(BigDecimal.class, value);
        }

        return null;
    }

    /**
     * Returns the valid value of the given field as BigInteger or null. Based on apache beanutils BigIntegerConverter
     * which converts Boolean.TRUE to 1 and Boolean.FALSE to 0
     * <br>
     * <b>Valid value:</b> {@link OutcomeUtils#hasValidNotBlankValue(String)}
     *
     * @param input either a JSONObject or an Outcome
     * @param key the field to be converted
     * @return value of the given field as BigInteger or null
     * 
     * @see org.apache.commons.beanutils.converters.BigIntegerConverter
     */
    public static BigInteger getBigIntegerOrNull(Object input, String key) {
        BigIntegerConverter converter = new BigIntegerConverter(null);

        Object value = getValueOrNull(input, key);

        if (value != null) {
            return converter.convert(BigInteger.class, value);
        }

        return null;
    }

    /**
     * Returns the valid value of the given field as Boolean or null.
     * Based on apache beanutils BooleanConverter which converts strings {"yes", "y", "true", "on", "1"} to true
     * and converts strings {"no", "n", "false", "off", "0"} to false 
     * <br>
     * <b>Valid value:</b> {@link OutcomeUtils#hasValidNotBlankValue(String)}
     *
     * @param input either a JSONObject, an Outcome or a Map
     * @param key the field to be converted
     * @return value of the given field as Boolean or null
     * 
     * @see org.apache.commons.beanutils.converters.BooleanConverter
     */
    public static Boolean getBooleanOrNull(Object input, String key) {
        BooleanConverter converter = new BooleanConverter(null);

        Object value = getValueOrNull(input, key);

        if (value != null) return converter.convert(Boolean.class, value);
        else               return null;
    }

    /**
     * Converts the value of the given field to LocalDate or null. If the input is a JSONObject
     * it uses webui specific pattern, configurable with 'Webui.format.date' system property,
     * default is 'yyyy-MM-dd'. If the input is an Outcome or Map the ISO_LOCAL_DATE format is used.
     * <br>
     * <b>Valid value:</b> {@link OutcomeUtils#hasValidNotBlankValue(String)}
     *
     * @param input either a JSONObject, an Outcome or a Map
     * @param key the field to be converted
     * @return value of the given field in LocalDate or null
     */
    public static LocalDate getLocalDateOrNull(Object input, String key) {
        Object value = getValueOrNull(input, key);

        if (value != null) {
            if (value instanceof LocalDate) {
                return (LocalDate)value;
            }
            else if (value instanceof String) {
                DateTimeFormatter dtf = null;

                if (input instanceof JSONObject) dtf = DateTimeFormatter.ofPattern(webuiDateFormat);
                else                             dtf = DateTimeFormatter.ISO_LOCAL_DATE;

                try {
                    return LocalDate.parse((String)value, dtf);
                }
                catch (DateTimeParseException e) {
                    log.debug("getLocalDateOrNull(key:{}) - DateTimeParseException:{}", key, e.getMessage());
                }
            }
            else {
                log.debug("getLocalDateOrNull(key:{}) - value '{}' is not a String, dropping it", key, value);
            }
        }

        return null;
    }

    /**
     * Returns the valid value of the given field as LocalDateTime or null. If the input is a JSONObject
     * it uses webui specific pattern, configurable with 'Webui.format.datetime' system property,
     * default is 'yyyy-MM-dd'T'HH:mm:ss'. If the input is an Outcome or Map the ISO_LOCAL_DATETIME
     * format is used.
     * <br>
     * <b>Valid value:</b> {@link OutcomeUtils#hasValidNotBlankValue(String)}
     *
     * @param input either a JSONObject, an Outcome or a Map
     * @param key the field to be converted
     * @return value of the given field as LocalDateTime or null
     */
    public static LocalDateTime getLocalDateTimeOrNull(Object input, String key) {
        Object value = getValueOrNull(input, key);

        if (value != null) {
            if (value instanceof LocalDateTime) {
                return (LocalDateTime)value;
            }
            else if (value instanceof String) {
                DateTimeFormatter dtf = null;

                if (input instanceof JSONObject) dtf = DateTimeFormatter.ofPattern(webuiDateTimeFormat);
                else                             dtf = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

                try {
                    return LocalDateTime.parse((String)value, dtf);
                }
                catch (DateTimeParseException e) {
                    log.debug("getLocalDateTimeOrNull(key:{}) - DateTimeParseException:{}", key, e.getMessage());
                }
            }
            else {
                log.debug("getLocalDateTimeOrNull(key:{}) - value '{}' is not a String, dropping it", key, value);
            }
        }

        return null;
    }

    /**
     * Returns the valid value of the given field as OffsetDateTime or null. If the input is a JSONObject
     * it uses webui specific pattern, configurable with 'Webui.format.datetime' system property,
     * default is 'yyyy-MM-dd'T'HH:mm:ss'. If the input is an Outcome or Map the ISO_OFFSET_DATE_TIME
     * format is used.
     * <br>
     * <b>Valid value:</b> {@link OutcomeUtils#hasValidNotBlankValue(String)}
     *
     * @param input either a JSONObject, an Outcome or a Map
     * @param key the field to be converted
     * @return value of the given field as OffsetDateTime or null
     */
    public static OffsetDateTime getOffsetDateTimeOrNull(Object input, String key) {
        Object value = getValueOrNull(input, key);

        if (value != null) {
            if (value instanceof OffsetDateTime) {
                return (OffsetDateTime)value;
            }
            else if (value instanceof String) {
                DateTimeFormatter dtf = null;

                try {
                    if (input instanceof JSONObject) {
                        dtf = DateTimeFormatter.ofPattern(webuiDateTimeFormat);
                        OffsetDateTime odt = OffsetDateTime.now(ZoneId.systemDefault());
                        ZoneOffset zoneOffset = odt.getOffset();
    
                            return LocalDateTime.parse((String)value, dtf).atOffset(zoneOffset);
                    }
                    else {
                        dtf = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
                        return OffsetDateTime.parse((String)value, dtf);
                    }
                }
                catch (DateTimeParseException e) {
                    log.debug("getOffsetDateTimeOrNull(key:{}) - DateTimeParseException:{}", key, e.getMessage());
                }
            }
            else {
                log.debug("getOffsetDateTimeOrNull(key:{}) - value '{}' is not a String, dropping it", key, value);
            }
        }

        return null;
    }

    /**
     * Returns the valid value of the given field as OffsetTime or null. If the input is a JSONObject
     * it uses webui specific pattern, configurable with 'Webui.format.time' system property,
     * default is 'HH:mm:ss'. If the input is an Outcome or Map the ISO_OFFSET_TIME format is used.
     * <br>
     * <b>Valid value:</b> {@link OutcomeUtils#hasValidNotBlankValue(String)}
     *
     * @param input either a JSONObject, an Outcome or a Map
     * @param key the field to be converted
     * @return value of the given field as OffsetTime or null
     */
    public static OffsetTime getOffsetTimeOrNull(Object input, String key) {
        Object value = getValueOrNull(input, key);

        if (value != null) {
            if (value instanceof OffsetTime) {
                return (OffsetTime)value;
            }
            else if (value instanceof String) {
                DateTimeFormatter dtf = null;

                try {
                    if (input instanceof JSONObject) {
                        dtf = DateTimeFormatter.ofPattern(webuiTimeFormat);
                        OffsetDateTime odt = OffsetDateTime.now (ZoneId.systemDefault ());
                        ZoneOffset zoneOffset = odt.getOffset ();
                        return LocalTime.parse((String)value, dtf).atOffset(zoneOffset);
                    }
                    else {
                        dtf = DateTimeFormatter.ISO_OFFSET_TIME;
                        return OffsetTime.parse((String)value, dtf);
                    }
                }
                catch (DateTimeParseException e) {
                    log.debug("getOffsetTimeOrNull(key:{}) - DateTimeParseException:{}", key, e.getMessage());
                }
            }
            else {
                log.debug("getOffsetTimeOrNull(key:{}) - value '{}' is not a String, dropping it", key, value);
            }
        }

        return null;
    }
}
