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
