package org.cristalise.dsl.entity

import org.cristalise.kernel.entity.DomainContext
import org.cristalise.kernel.entity.imports.ImportItem
import org.cristalise.kernel.lookup.AgentPath
import org.cristalise.kernel.lookup.DomainPath
import groovy.transform.CompileStatic

@CompileStatic
class DomainContextBuilder {
    public static List<DomainContext> build(Map<String, Object> attrs, @DelegatesTo(DomainContextDelegate) Closure cl) {
        assert attrs, "cannot work with empty attributes (Map)"

        def delegate = new DomainContextDelegate(attrs)
        delegate.processClosure(cl)

        return delegate.newContexts
    }

    public static List<DomainContext> build(String ns, Integer version = 0, @DelegatesTo(DomainContextDelegate) Closure cl) {
        return build(['ns': ns, 'version': version], cl)
    }
}
