module cristalise.kernel {
    requires jdk.xml.dom;
    requires java.xml;
    requires java.scripting;
    requires java.desktop;
    requires java.sql;

//    requires castor.core;
    requires castor.xml;
    requires castor.xml.schema;

    requires static lombok;
    requires org.xmlunit;
    requires slf4j.api;
    requires shiro.core;
    requires mvel2;

    requires org.apache.commons.lang3;
    requires commons.beanutils;

    requires com.google.common;
    requires com.google.errorprone.annotations;
    
    requires com.fasterxml.jackson.core;
    requires io.vertx.core;
    uses io.vertx.codegen;
    requires io.vertx.shell;
    requires io.vertx.serviceproxy;
    requires io.vertx.auth.common;
    requires io.vertx.eventbusbridge.common;
    requires io.vertx.clustermanager.hazelcast;
    requires com.hazelcast.core;
//    requires vertx.codegen;
}
