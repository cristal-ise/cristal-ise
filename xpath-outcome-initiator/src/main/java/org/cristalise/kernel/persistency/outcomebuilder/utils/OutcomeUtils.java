package org.cristalise.kernel.persistency.outcomebuilder.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcomebuilder.Field;
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder;
import org.cristalise.kernel.persistency.outcomebuilder.field.LongStringField;
import org.cristalise.kernel.persistency.outcomebuilder.field.StringField;
import org.cristalise.kernel.process.Gateway;
import org.json.JSONObject;

/**
 *
 *
 */
public class OutcomeUtils {
    public static final String webuiDateFormat     = Gateway.getProperties().getString("Webui.format.date",     "yyyy-MM-dd");
    public static final String webuiDateTimeFormat = Gateway.getProperties().getString("Webui.format.datetime", "yyyy-MM-dd'T'HH:mm:ss");

    /**
     *
     * @param input
     * @param key
     * @return
     */
    public static boolean hasField(Object input, String key) {
        if (input instanceof JSONObject) {
            return ((JSONObject)input).has(key);
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
     * 
     * @param input
     * @param key
     * @return
     */
    public static boolean hasValue(Object input, String key) {
        if (input instanceof JSONObject) {
            JSONObject json = (JSONObject) input;
            return json.has(key) && !json.isNull(key);
        }
        else if (input instanceof Outcome) {
            String value = ((Outcome) input).getField(key);
            return StringUtils.isNotBlank(value);
        }
        else if (input instanceof Map) {
            String value = (String) ((Map<?, ?>) input).get(key);
            return StringUtils.isNotBlank(value);
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
     * @param schema
     * @return
     */
    public static Object getValueOrNull(Object input, String key, OutcomeBuilder builder) {
        Field f = (Field)builder.findChildStructure(key);
        Class<?> fieldClazz = f.getInstanceClass();

        if (fieldClazz.equals(StringField.class) || fieldClazz.equals(LongStringField.class)) {
            
        }
        
        return null;
    }

    /**
     * Converts the value of the given field to BigDecimal or null. No rounding is done.
     *
     * @param input either a JSONObject or an Outcome
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
     * Converts the value of the given field to Integer or null.
     *
     * @param input either a JSONObject or an Outcome
     * @param key the field to be converted
     * @return value of the given field in Integer or null
     */
    public static Integer getIntegerOrNull(Object input, String key) {
        if (input instanceof JSONObject) {
            JSONObject json = (JSONObject) input;
            if (json.has(key) && !json.isNull(key)) {
                Object value = json.get(key);

                if (value instanceof String && "".equals(value)) return null;

                return json.getBigDecimal(key).intValue();
            }
        }
        else if (input instanceof Outcome) {
            String value = ((Outcome) input).getField(key);

            if (StringUtils.isNotBlank(value)) return new Integer(value);
        }
        else if (input instanceof Map) {
            String value = (String) ((Map<?, ?>) input).get(key);

            if (StringUtils.isNotBlank(value)) return new Integer(value);
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

            if (StringUtils.isNotBlank(value)) return new Boolean(value);
        }
        else if (input instanceof Map) {
            String value = (String) ((Map<?, ?>) input).get(key);

            if (StringUtils.isNotBlank(value)) return new Boolean(value);
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

                if (value instanceof String && "".equals(value)) return null;

                return LocalDate.parse(json.getString(key), DateTimeFormatter.ofPattern(webuiDateFormat));
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
     * @return value of the given field in LocalDate or null
     */
    public static LocalDateTime getLocalDateTimeOrNull(Object input, String key) {
        if (input instanceof JSONObject) {
            JSONObject json = (JSONObject) input;
            if (json.has(key) && !json.isNull(key)) {
                Object value = json.get(key);

                if (value instanceof String && "".equals(value)) return null;

                return LocalDateTime.parse(json.getString(key), DateTimeFormatter.ofPattern(webuiDateTimeFormat));
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
}
