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
package org.cristalise.kernel.persistency.outcomeinit;

import org.apache.xmlbeans.SchemaType;
import org.cristalise.kernel.persistency.outcome.OutcomeInitiator;
import org.cristalise.kernel.utils.SystemPropertyOperations;

import lombok.Getter;

/**
 * Defines all SystemProperties that are supported in the kernel to configure the behavior of the
 * application. Due to the limitation of javadoc, the actual usable string cannot be shown easily,
 * therefore replace underscores with dots to get the actual System Property:
 * 
 * <pre>
 *   SimpleType_DefaultValues => SimpleType.DefaultValues
 * </pre>
 * 
 * @see #SimpleType_DefaultValues
 */
@Getter
public enum SystemProperties  implements SystemPropertyOperations{

    /**
     * Very rudimentary configuration of default values used in {@link EmptyOutcomeInitiator}. No default value.
     * It is based on the integer values of the {@link SchemaType} enumeration, 
     * e.g. '12:string' will specify the default value for String. Use comma to specify more then one defaults.
     */
    SimpleType_DefaultValues("SimpleType.DefaultValues"),
    /**
     * Override the default prefix ('/') used to identify Activity Properties that can be used by 
     * this {@link OutcomeInitiator} implementation. Default value is '/'.
     */
    XPathOutcomeInitiator_PropertyNamePrefix("XPathOutcomeInitiator.PropertyNamePrefix", "/");

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
