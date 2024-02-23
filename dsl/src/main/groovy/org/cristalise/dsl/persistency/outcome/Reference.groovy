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
package org.cristalise.dsl.persistency.outcome;

/**
 * Instructs code generation and persistency layer to update the Collection of the Item associated.
 * (optional)
 */
public class Reference {
    /**
     * Mandatory field that holds information about the referenced Item type, which is either
     * <ul>
     *   <li>String containing the Item type 
     *   <li>Reference of the PropertyDescriptionList object representing the referenced Item type
     * </ul>
     */
    def itemType
    /**
     *  Optional field to identify Collection of the Item when its name cannot be deduced
     *  from the name of the field or from the itemType
     */
    String collectionName
}
