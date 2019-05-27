package org.cristalise.dsl.persistency.outcome

import org.codehaus.groovy.runtime.InvokerHelper
import org.cristalise.kernel.utils.Logger

import groovy.util.ObjectGraphBuilder.ChildPropertySetter
import groovy.util.ObjectGraphBuilder.DefaultChildPropertySetter

class DSLPropertySetter implements ChildPropertySetter {

    @Override
    public void setChild(Object parent, Object child, String parentName, String propertyName) {
        try {
            Object property = InvokerHelper.getProperty(parent, propertyName);

            if (property != null) {
                if (Collection.class.isAssignableFrom(property.getClass())) {
                    ((Collection) property).add(child);
                }
                else if (Map.class.isAssignableFrom(property.getClass())) {
                    ((Map) property).put(child.name, child);
                    parent.orderOfElements.add(child.name)
                }
                else  {
                    InvokerHelper.setProperty(parent, propertyName, child);
                }
            }
            else {
                InvokerHelper.setProperty(parent, propertyName, child);
            }
        }
        catch (MissingPropertyException mpe) {
            Logger.msg(5, "DSLPropertySetter.setChild($parentName, $propertyName) - MissingPropertyException:" + mpe.message)
        }
    }
}
