package org.cristalise.lookup.lite

import org.cristalise.kernel.lookup.Lookup
import org.cristalise.kernel.lookup.LookupManager

class InMemoryLookupFactory implements LookupManager, Lookup {
    @Delegate InMemoryLookupManager lookup = InMemoryLookupManager.instance
}
