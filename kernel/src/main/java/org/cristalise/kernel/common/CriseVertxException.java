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

import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.exception.ExceptionUtils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceException;

public class CriseVertxException extends Exception {
    private final int failureCode;

    private static final long serialVersionUID = 5606562389374279530L;

    public CriseVertxException(int code) {
        super();
        failureCode = code;
    }

    public CriseVertxException(int code, Exception e) {
        super(e);
        failureCode = code;
    }

    public CriseVertxException(int code, String msg) {
        super(msg);
        failureCode = code;
    }

    public <T> AsyncResult<T> fail() {
        return Future.failedFuture(new ServiceException(failureCode, getMessage(), convertToDebugInfo(this)));
    }

    public static JsonObject convertToDebugInfo(Exception ex) {
        JsonObject debugInfo = new JsonObject();
        debugInfo.put("type", ex.getClass().getSimpleName());

        JsonArray stackTrace = new JsonArray();
        debugInfo.put("stackTrace", stackTrace);

        for (String frame : ExceptionUtils.getStackFrames(ex)) stackTrace.add(frame);

        return debugInfo;
    }

    public static CriseVertxException convert(ExecutionException futureException) {
        Exception cause = (Exception) futureException.getCause();

        if (cause instanceof CriseVertxException) return (CriseVertxException)cause;
        else                                      return new CriseVertxException(999, cause);
    }
}
