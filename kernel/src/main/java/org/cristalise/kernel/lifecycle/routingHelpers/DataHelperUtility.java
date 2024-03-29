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
package org.cristalise.kernel.lifecycle.routingHelpers;

import static org.cristalise.kernel.SystemProperties.DataHelper_$name;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.TransactionKey;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class to retrieve and resolve DataHelpers
 */
@Slf4j
public class DataHelperUtility {

    /**
     * First checks the configuration properties to instantiate the requested Datahelper.
     * If there is such no property, it uses the given id to instantiate one of these classes:
     * {@link ViewpointDataHelper}, {@link PropertyDataHelper}, {@link ActivityDataHelper}
     *
     * @param id the string used to identify the DataHelper in the cristal-ise configuration
     * @return the DataHelper instance
     * @throws InvalidDataException could not configure DataHelper
     */
    public static DataHelper getDataHelper(String id) throws InvalidDataException {
        Object[] args = new Object[] {id};

        if (DataHelper_$name.getObject(args) != null) {
            try {
                return (DataHelper) DataHelper_$name.getInstance(args);
            }
            catch (ReflectiveOperationException e) {
                throw new InvalidDataException(e);
            }
        }
        else {
            switch (BuiltInDataHelpers.getValue(id)) {
                case VIEWPOINT_DH: return new ViewpointDataHelper();
                case PROPERTY_DH:  return new PropertyDataHelper();
                case ACTIVITY_DH:  return new ActivityDataHelper();

                default: throw new InvalidDataException("UNKOWN DataHelper id:"+id);
            }
        }
    }

    /**
     * If the
     *
     * @param itemPath the actual Item context
     * @param value the value to be evaluated
     * @param actContext activity path
     * @param transactionKey database transaction transactionKey
     * @return String value which was evaluated using {@link DataHelper} implementation
     *
     * @throws InvalidDataException data inconsistency
     * @throws PersistencyException persistency issue
     * @throws ObjectNotFoundException  object was not found
     */
    public static Object evaluateValue(ItemPath itemPath, Object value, String actContext, TransactionKey transactionKey)
            throws InvalidDataException, PersistencyException, ObjectNotFoundException
    {
        if (value == null || !(value instanceof String) || !((String)value).contains("//"))
            return value;

        if(itemPath == null) throw new InvalidDataException("DataHelper must have ItemPath initialised");

        //Finding the first occurrence of '//' because DataHelper uses XPath which can start with '//'
        int i = ((String)value).indexOf("//");

        if (i == -1) throw new InvalidDataException("DataHelperUtility.evaluateValue() - Cannot locate '//' in value:"+value);

        String pathType = ((String)value).substring(0, i);
        String dataPath = ((String)value).substring(i+2);

        log.debug("evaluateValue() - pathType:"+pathType+" dataPath:"+dataPath);

        DataHelper dataHelper = getDataHelper(pathType);

        if (dataHelper != null) return dataHelper.get(itemPath, actContext, dataPath, transactionKey);
        else                    return value;
    }
}
