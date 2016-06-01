package org.cristalise.storage

import groovy.transform.CompileStatic

import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.persistency.ClusterStorage
import org.cristalise.kernel.property.Property
import org.cristalise.lookup.lite.InMemoryLookupManager

@CompileStatic
class InMemoryPropertyClusterStorage extends ClusterStorage {
    @Delegate InMemoryLookupManager lookup = InMemoryLookupManager.instance

    @Override
    short queryClusterSupport(String clusterType) {
        if (clusterType.equals(PROPERTY)) return READWRITE;
        else                              return NONE;
    }

    @Override
    String getName() { return 'InMemory Lookup Property Cluster Storage' }

    @Override
    String getId() { return 'InMemory Lookup' }
}
