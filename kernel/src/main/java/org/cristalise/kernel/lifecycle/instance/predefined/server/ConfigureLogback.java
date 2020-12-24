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
package org.cristalise.kernel.lifecycle.instance.predefined.server;

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCHEMA_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCHEMA_VERSION;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * {@value #description}
 */
@Slf4j
public class ConfigureLogback extends PredefinedStep {
    public static final String description = "Updates the log levels for Root level and for the named Loggers";

    public ConfigureLogback() {
        super();
        setBuiltInProperty(SCHEMA_NAME, "LoggerConfig");
    }

    /**
     * 
     */
    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath item, int transitionID, String requestData, TransactionKey transactionKey)
            throws InvalidDataException, ObjectCannotBeUpdated, ObjectNotFoundException, CannotManageException,
                   ObjectAlreadyExistsException, InvalidCollectionModification
    {
        Schema schema = LocalObjectLoader.getSchema(
                (String)getBuiltInProperty(SCHEMA_NAME), 
                Integer.parseInt((String)getBuiltInProperty(SCHEMA_VERSION)),
                transactionKey);

        Outcome config = new Outcome(requestData, schema);

        config.validateAndCheck();

        try {
            if (config.hasField("Root")) {
                setLogLevel(null, config.getField("Root"));
            }

            for (Map<String, String> record: config.getAllRecords("//Logger")) {
                setLogLevel(record.get("Name"), record.get("Level"));
            }
        }
        catch (InvalidDataException | ReflectiveOperationException | XPathExpressionException e) {
            log.warn("Could not configure logback", e);
            log.debug("requestData: {}", requestData);
        }

        return requestData;
    }

    private static final String LOGBACK_CLASSIC        = "ch.qos.logback.classic";
    private static final String LOGBACK_CLASSIC_LOGGER = "ch.qos.logback.classic.Logger";
    private static final String LOGBACK_CLASSIC_LEVEL  = "ch.qos.logback.classic.Level";

    /**
     * Dynamically sets the logback log level for the given class to the specified level.
     *
     * @param loggerName Name of the logger to set its log level. If blank, root logger will be used.
     * @param logLevel One of the supported log levels: TRACE, DEBUG, INFO, WARN, ERROR, FATAL, OFF. blank value is considered as 'OFF'.
     * @throws ReflectiveOperationException could not find Logback classes or methods
     * @throws InvalidDataException Logback is not in the classpath
     */
    public static boolean setLogLevel(String loggerName, String logLevel) throws InvalidDataException, ReflectiveOperationException {
        Package logbackPackage = Package.getPackage(LOGBACK_CLASSIC);
        if (logbackPackage == null) {
            throw new InvalidDataException("Logback is not in the classpath!");
        }

        // Use ROOT logger if given logger name is blank.
        if (StringUtils.isBlank(loggerName)) {
            loggerName = (String) getFieldVaulue(LOGBACK_CLASSIC_LOGGER, "ROOT_LOGGER_NAME");
        }

        // Obtain logger by the name - TODO check if Package/Class exists because Logback classic does not check such details
        Logger loggerObtained = LoggerFactory.getLogger(loggerName);
        if (loggerObtained == null) {
            log.warn("No logger found for the name: {}", loggerName);
            return false;
        }

        Object logLevelObj = getFieldVaulue(LOGBACK_CLASSIC_LEVEL, logLevel);
        if (logLevelObj == null) {
            log.warn("No such log level: {}", logLevel);
            return false;
        }

        Class<?>[] paramTypes = { logLevelObj.getClass() };
        Object[]   params     = { logLevelObj };

        Class<?> clazz  = Class.forName(LOGBACK_CLASSIC_LOGGER);
        Method   method = clazz.getMethod("setLevel", paramTypes);
        method.invoke(loggerObtained, params);

        log.debug("Log level set to '{}' for the logger '{}'", logLevel, loggerName);
        return true;
    }

    private static Object getFieldVaulue(String fullClassName, String fieldName) {
        try {
            Class<?> clazz = Class.forName(fullClassName);
            Field    field = clazz.getField(fieldName);
            return field.get(null);
        }
        catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
