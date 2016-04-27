/**
 * This file is part of the CRISTAL-iSE LDAP lookup plugin.
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
package org.cristalise.lookup.ldap;

import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lookup.ItemPath;


/** Allows clients to directly load properties and collections from the LDAP
*   so no CORBA calls need to be made during normal browsing
*/

public class LDAPClientReader extends LDAPClusterStorage {
     // return all readwrite support as readonly
     @Override
	public short queryClusterSupport(String clusterType) {
        return (short)(super.queryClusterSupport(clusterType) & READ);
     }


	/**
	 * @see org.cristalise.kernel.persistency.ClusterStorage#delete(Integer, String)
	 */
	@Override
	public void delete(ItemPath itemPath, String path)
		throws PersistencyException {
		throw new PersistencyException("Writing not supported in ClientReader");
	}

	/**
	 * @see org.cristalise.kernel.persistency.ClusterStorage#getName()
	 */
	@Override
	public String getName() {
		return "LDAP Client Cluster Reader";
	}

	/**
	 * @see org.cristalise.kernel.persistency.ClusterStorage#put(Integer, String, C2KLocalObject)
	 */

	public void put(ItemPath itemPath, String path, C2KLocalObject obj)
		throws PersistencyException {
		throw new PersistencyException("Writing not supported in ClientReader");
	}

}
