/**
 * This file is part of the CRISTAL-iSE REST API.
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
package org.cristalise.restapi;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.*;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilderException;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.scripting.ScriptErrorException;
import org.cristalise.kernel.scripting.ScriptingEngineException;

import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;

import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Slf4j
public class WebAppExceptionBuilder {

    private String message;
    private Exception exception;
    private Response.Status status;
    private NewCookie newCookie;

    public WebAppExceptionBuilder() {
    }

    /**
     * Creates a WebApplicationException Builder from a simple text message, exception and status
     *
     * @param msg text message
     * @param ex exception
     * @param code HTTP status of the response
     * @param cookie cookie will be added to the response
     * @return WebApplicationException Builder
     */
    public WebAppExceptionBuilder(String msg, Exception ex, Response.Status code, NewCookie cookie) {
        if (ex != null) this.exception(ex);
        if (StringUtils.isNotBlank(msg)) this.message = msg;
        if (code != null) this.status = code;
        this.newCookie = cookie;
    }

    /**
     * Creates a WebApplicationException Builder
     *
     * @param ex
     * @return
     */
    public WebAppExceptionBuilder exception(Exception ex) {
        this.exception = ex;
        if (StringUtils.isBlank(this.message)) this.message = ex.getMessage();

        log.trace("exception()", ex);

        if (ex instanceof OutcomeBuilderException ||
            ex instanceof ObjectAlreadyExistsException ||
            ex instanceof InvalidDataException ||
            ex instanceof ScriptErrorException ||
            ex instanceof ScriptingEngineException ||
            ex instanceof InvalidItemPathException ||
            ex instanceof ClassCastException ||
            ex instanceof InvalidCollectionModification)
        {
            this.status = Response.Status.BAD_REQUEST;
        }
        else if (ex instanceof UnsupportedOperationException) {
            this.status = Response.Status.UNSUPPORTED_MEDIA_TYPE;
        }
        else if (ex instanceof AccessRightsException) {
            this.status = Response.Status.UNAUTHORIZED;
        }
        else if (ex instanceof ObjectNotFoundException) {
            this.status = Response.Status.NOT_FOUND;
        }
        else if (ex instanceof InvalidTransitionException) {
            this.status = Response.Status.CONFLICT;
        }
        else if (ex instanceof NotImplementedException) {
            this.status = Response.Status.NOT_IMPLEMENTED;
        }
        else if (ex instanceof PersistencyException ||
                 ex instanceof MarshalException ||
                 ex instanceof ValidationException ||
                 ex instanceof IOException ||
                 ex instanceof MappingException)
        {
            this.status = Response.Status.INTERNAL_SERVER_ERROR;
        }
        else if (ex instanceof WebApplicationException) {
            log.debug("exception() - DO NOTHING with WebApplicationException: {}", this.message);

//            Response response = ((WebApplicationException) ex).getResponse();
//            this.status = Response.Status.fromStatusCode(response.getStatus());
//            this.message = response.getEntity().toString();
        }
        else {
            log.debug("exception() - Mapping excpetion '{}' to INTERNAL_SERVER_ERROR", ex.getClass().getSimpleName());
            this.status = Response.Status.INTERNAL_SERVER_ERROR;
        }

        return this;
    }

    /**
     * Creates a WebApplicationException Builder
     *
     * @param message
     * @return
     */
    public WebAppExceptionBuilder message(String message) {
        this.message = message;
        return this;
    }

    /**
     * Creates a WebApplicationException Builder
     *
     * @param status
     * @return
     */
    public WebAppExceptionBuilder status(Response.Status status) {
        this.status = status;
        return this;
    }

    /**
     * Creates a WebApplicationException Builder
     *
     * @param newCookie
     * @return
     */
    public WebAppExceptionBuilder newCookie(NewCookie newCookie) {
        this.newCookie = newCookie;
        return this;
    }

    /**
     * Creates a WebApplicationException response from a simple text message, exception and status
     *
     * @return WebApplicationException Response
     */
    public WebApplicationException build() {
        // recapturing WebApplicationException, which mean everything is setup already
        if (exception != null && exception instanceof WebApplicationException) return (WebApplicationException)exception;

        if (status == null) status = Response.Status.INTERNAL_SERVER_ERROR;

        if (StringUtils.isBlank(message)) message = "Application process failed";

        log.debug("build() - msg:{} status:{}", message, status, exception);

        Response.ResponseBuilder responseBuilder;

        if (newCookie != null && status != Response.Status.UNAUTHORIZED) responseBuilder = Response.status(status).cookie(newCookie);
        else                                                             responseBuilder = Response.status(status);

        if (Gateway.getProperties().getBoolean("REST.Debug.errorsWithBody", false)) {
            StringBuffer sb = new StringBuffer("[errorMessage]");
            sb.append(message).append("[/errorMessage]");

            if(exception != null) sb.append(" - Exception:" + exception.getMessage());

            return new WebApplicationException(sb.toString(), responseBuilder.entity(message).build());
        }
        else {
            return new WebApplicationException(message, responseBuilder.build());
        }
    }
}
