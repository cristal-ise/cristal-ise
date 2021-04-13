package org.cristalise.kernel.common;

import org.apache.commons.lang3.exception.ExceptionUtils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceException;

public class VertxException extends Exception {
    private final int failureCode;

    private static final long serialVersionUID = 5606562389374279530L;

    public VertxException(int code) {
        super();
        failureCode = code;
    }

    public VertxException(int code, Exception e) {
        super(e);
        failureCode = code;
    }

    public VertxException(int code, String msg) {
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
}
