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

import org.cristalise.kernel.utils.SystemPropertyOperations;

import lombok.Getter;

@Getter
enum SystemProperties  implements SystemPropertyOperations{

    REST_addCorsHeaders("REST.addCorsHeaders", false),
    REST_allowWeakKey("REST.allowWeakKey", false),
    REST_CollectionForm_checkInputs("REST.CollectionForm.checkInputs", false),
    REST_corsAllowCredentials("REST.corsAllowCredentials",  true),
    REST_corsAllowMethods("REST.corsAllowMethods",  "GET, POST, OPTIONS"),
    REST_corsAllowOrigin("REST.corsAllowOrigin",  "*"),
    REST_Debug_errorsWithBody("REST.Debug.errorsWithBody", false),
    REST_DefaultBatchSize("REST.DefaultBatchSize", 75),
    REST_Event_DefaultBatchSize("REST.Event.DefaultBatchSize"),
    REST_loginCookieLife("REST.loginCookieLife", 0),
    REST_Path_DefaultBatchSize("REST.Path.DefaultBatchSize"),
    REST_requireLoginCookie("REST.requireLoginCookie", true),
    REST_Roles_withoutTimeout("REST.Roles.withoutTimeout"),
    REST_URI("REST.URI", "http://localhost:8081/");

    private Object defaultValue;
    private String systemPropertyName;

    private SystemProperties(String name) {
        this(name, null);
    }

    private SystemProperties(String name, Object value) {
        systemPropertyName = name;
        defaultValue = value;
    }

    @Override
    public String toString() {
        return systemPropertyName;
    }
}
