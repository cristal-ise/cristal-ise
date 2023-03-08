package org.cristalise.storage

import org.cristalise.kernel.persistency.ClusterStorage
import org.cristalise.kernel.persistency.ClusterType
import org.cristalise.lookup.lite.InMemoryLookupManager

//@CompileStatic
class InMemoryPropertyClusterStorage extends ClusterStorage {
    @Delegate InMemoryLookupManager lookup = InMemoryLookupManager.instance

    @Override
    short queryClusterSupport(ClusterType type) {
        if (type == ClusterType.PROPERTY) return READWRITE;
        else                              return NONE;
    }

    @Override
    String getName() { return 'InMemory Lookup Property Cluster Storage' }

    @Override
    String getId() { return 'InMemory Lookup' }
}
