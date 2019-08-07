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

import org.cristalise.kernel.common.*;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilderException;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.scripting.ScriptErrorException;
import org.cristalise.kernel.scripting.ScriptingEngineException;
import org.cristalise.kernel.utils.Logger;

import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.io.IOException;

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
     * @param message text message
     * @param exception exception
     * @param status HTTP status of the response
     * @param newCookie cookie will be added to the response
     * @return WebApplicationException Builder
     */
    public WebAppExceptionBuilder(String message, Exception exception, Response.Status status, NewCookie newCookie) {
        this.message = message;
        this.exception = exception;
        this.status = status;
        this.newCookie = newCookie;
    }

    /**
     * Creates a WebApplicationException Builder
     *
     * @param exception
     * @return
     */
    public WebAppExceptionBuilder exception(Exception exception) {
        this.exception = exception;
        this.message = exception.getMessage();

        if ( exception instanceof OutcomeBuilderException ||
             exception instanceof ObjectAlreadyExistsException ||
             exception instanceof InvalidDataException ||
             exception instanceof ScriptErrorException ||
             exception instanceof ScriptingEngineException ||
             exception instanceof InvalidItemPathException ||
             exception instanceof ClassCastException ||
             exception instanceof InvalidCollectionModification ) {
            this.status = Response.Status.BAD_REQUEST;
        } else if ( exception instanceof UnsupportedOperationException ) {
            this.status = Response.Status.UNSUPPORTED_MEDIA_TYPE;
        } else if ( exception instanceof AccessRightsException ) {
            this.status = Response.Status.UNAUTHORIZED;
        } else if ( exception instanceof ObjectNotFoundException ) {
            this.status = Response.Status.NOT_FOUND;
        } else if ( exception instanceof InvalidTransitionException ) {
            this.status = Response.Status.CONFLICT;
        } else if ( exception instanceof PersistencyException ||
                    exception instanceof MarshalException ||
                    exception instanceof ValidationException ||
                    exception instanceof IOException ||
                    exception instanceof MappingException ) {
            this.status = Response.Status.INTERNAL_SERVER_ERROR;
        } else if ( exception instanceof WebApplicationException ) {
            Response response = ((WebApplicationException) exception).getResponse();
            int statusCode = response.getStatus();
            this.exception = null;
            this.message = response.getEntity().toString();
            this.status = Response.Status.fromStatusCode(statusCode);
        } else {
            Logger.error(exception);
            this.status = Response.Status.INTERNAL_SERVER_ERROR;
            this.exception = null;
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
    public WebApplicationException build () {

        if ( status == null ) {
            status = Response.Status.INTERNAL_SERVER_ERROR;
        }

        if ( message == null || message.equals("") ) {
            message = "Application process failed";
        }

        Logger.debug(8, "ItemUtils.createWebAppException() - msg:"+ message + " status:" + status);
        int defaultLogLevel = Gateway.getProperties().getInt("LOGGER.defaultLevel", 9);
        if (exception != null && Logger.doLog(defaultLogLevel)) {
            Logger.error(exception);
        }

        Response.ResponseBuilder responseBuilder;
        if ( newCookie != null && status != Response.Status.UNAUTHORIZED) {
            responseBuilder = Response.status(status).cookie(newCookie);
        } else {
            responseBuilder = Response.status(status);
        }

        if (Gateway.getProperties().getBoolean("REST.Debug.errorsWithBody", false)) {
            StringBuffer sb = new StringBuffer("[errorMessage]");
            sb.append(message).append("[/errorMessage]");

            if(exception != null) {
                sb.append(" - Exception:" + exception.getMessage());
            }

            return new WebApplicationException(sb.toString(), responseBuilder.entity(message).build());
        }
        else {
            return new WebApplicationException(message, responseBuilder.build() );
        }
    }

}
