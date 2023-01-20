package org.cristalise.dsl.entity

import org.cristalise.kernel.entity.DomainContext
import groovy.transform.CompileStatic

@CompileStatic
class DomainContextDelegate {
    String namespace
    public List<DomainContext> newContexts = new ArrayList<>()

    public DomainContextDelegate(Map<String, Object> args) {
        assert args
        namespace = args.ns
    }

    public void processClosure(Closure cl) {
        assert cl

        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()
    }

    public void DomainContext(String path, Integer version = 0, Closure cl = null) {
        def dc = new DomainContext(path, namespace, version)
        newContexts.add(dc)
    }
}
