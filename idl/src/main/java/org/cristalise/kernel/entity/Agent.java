package org.cristalise.kernel.entity;


/**
* org/cristalise/kernel/entity/Agent.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from /home/vagrant/workspace/cristal-ise/idl/src/main/idl/Entity.idl
* Saturday, March 14, 2020 10:40:43 AM CET
*/


/**************************************************************************
    * Agent is a ManageableEntity that represents an Activity executor in the 
    * system. It holds a job list, which are persistent requests for execution 
    * from waiting activities assigned to a role that has such Job pushing enabled.
    **************************************************************************/
public interface Agent extends AgentOperations, org.cristalise.kernel.entity.Item, org.omg.CORBA.portable.IDLEntity 
{
} // interface Agent
