package org.cristalise.dsl.persistency.outcome;

public class Additional {
    def fields = [:]
    def propertyMissing(String name, Object value) { fields[name] = value }
    def propertyMissing(String name) { fields[name] }
}
