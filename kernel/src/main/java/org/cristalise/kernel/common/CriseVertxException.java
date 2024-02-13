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
package org.cristalise.kernel.common;

import static org.cristalise.kernel.common.CriseVertxException.FailureCodes.InternalServerError;

import java.io.Serial;
import java.util.concurrent.ExecutionException;

import lombok.Getter;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.scripting.ScriptErrorException;

import io.vertx.core.AsyncResult;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceException;

public class CriseVertxException extends Exception {
    @Serial
    private static final long serialVersionUID = 5606562389374279530L;
    
    @Getter
    public enum FailureCodes {
        /**
         * Generic code for errors not covered by specific exception
         */
        InternalServerError(999),
        AccessRightsError(101),
        CannotManage(102),
        InvalidCollectionModification(103),
        InvalidData(104),
        InvalidTransition(105),
        ObjectAlreadyExists(106),
        ObjectCannotBeUpdated(107),
        ObjectNotFound(108),
        PersistencyError(109),
        InvalidItemPath(110),
        ScriptError(111);

        private int id;

        private FailureCodes(final int code) {
            id = code;
        }

        public static FailureCodes getValue(int id) {
            for (FailureCodes code : FailureCodes.values()) {
                if(code.getId() == id) return code;
            }
            return null;
        }
    }

    /**
     * 
     */
    private final int failureCode;

    public CriseVertxException() {
        super();
        failureCode = InternalServerError.getId();
    }

    public CriseVertxException(String msg) {
        super(msg);
        failureCode = InternalServerError.getId();
    }

    public CriseVertxException(Throwable cause) {
        super(cause.getMessage(), cause);
        failureCode = InternalServerError.getId();
    }

    public CriseVertxException(String msg, Throwable cause) {
        super(msg, cause);
        failureCode = InternalServerError.getId();
    }

    public CriseVertxException(FailureCodes code) {
        super();
        failureCode = code.getId();
    }

    public CriseVertxException(FailureCodes code, String msg) {
        super(msg);
        failureCode = code.getId();
    }

    public CriseVertxException(FailureCodes code, Throwable cause) {
        super(cause.getMessage(), cause);
        failureCode = code.getId();
    }

    public CriseVertxException(FailureCodes code, String msg, Throwable cause) {
        super(msg, cause);
        failureCode = code.getId();
    }

    public <T> AsyncResult<T> failService() {
        return ServiceException.fail(failureCode, getMessage(), getDebugInfo());
    }

    public static <T> AsyncResult<T> failService(Throwable ex) {
        JsonObject debugInfo = convertToDebugInfo(ex);
        return ServiceException.fail(InternalServerError.getId(), ex.getMessage(), debugInfo);
    }

    public static JsonObject convertToDebugInfo(Throwable ex) {
        if (ex == null) return null;

        JsonObject debugInfo = new JsonObject();
        debugInfo.put("type", ex.getClass().getSimpleName());

        JsonArray stackTrace = new JsonArray();
        debugInfo.put("stackTrace", stackTrace);

        for (String frame : ExceptionUtils.getStackFrames(ex)) stackTrace.add(frame);

        return debugInfo;
    }

    public JsonObject getDebugInfo() {
        return convertToDebugInfo(this);
    }

    public JsonObject getServiceDebugInfo() {
        Throwable cause = getCause();

        if (cause instanceof ServiceException) return ((ServiceException)cause).getDebugInfo();
        else                                   return null;
    }

    public static CriseVertxException convertThrowable(Throwable ex) {
        if (ex == null) return null;

        if (ex instanceof CriseVertxException) {
            return (CriseVertxException)ex;
        }
        else if (ex instanceof ServiceException serviceEx) {
            int failureCode = serviceEx.failureCode();
            return switch (FailureCodes.getValue(failureCode)) {
                case AccessRightsError              -> new AccessRightsException(ex);
                case CannotManage                   -> new CannotManageException(ex);
                case InvalidCollectionModification  -> new InvalidCollectionModification(ex);
                case InvalidData                    -> new InvalidDataException(ex);
                case InvalidItemPath                -> new InvalidItemPathException(ex);
                case InvalidTransition              -> new InvalidTransitionException(ex);
                case ObjectAlreadyExists            -> new ObjectAlreadyExistsException(ex);
                case ObjectCannotBeUpdated          -> new ObjectCannotBeUpdated(ex);
                case ObjectNotFound                 -> new ObjectNotFoundException(ex);
                case PersistencyError               -> new PersistencyException(ex);
                case ScriptError                    -> new ScriptErrorException(ex);
                case null, default                  -> null;
            };
        }
        return null;
    }

    public static CriseVertxException convertFutureException(ExecutionException futureException) {
        CriseVertxException result = convertThrowable(futureException.getCause());

        if (result == null) result = new CriseVertxException(futureException);

        return result;
    }
}
