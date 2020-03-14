package org.cristalise.kernel.entity;


/**
* org/cristalise/kernel/entity/AgentHelper.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from /home/vagrant/workspace/cristal-ise/idl/src/main/idl/Entity.idl
* Saturday, March 14, 2020 10:40:43 AM CET
*/


/**************************************************************************
    * Agent is a ManageableEntity that represents an Activity executor in the 
    * system. It holds a job list, which are persistent requests for execution 
    * from waiting activities assigned to a role that has such Job pushing enabled.
    **************************************************************************/
abstract public class AgentHelper
{
  private static String  _id = "IDL:org.cristalise.kernel/entity/Agent:1.0";

  public static void insert (org.omg.CORBA.Any a, org.cristalise.kernel.entity.Agent that)
  {
    org.omg.CORBA.portable.OutputStream out = a.create_output_stream ();
    a.type (type ());
    write (out, that);
    a.read_value (out.create_input_stream (), type ());
  }

  public static org.cristalise.kernel.entity.Agent extract (org.omg.CORBA.Any a)
  {
    return read (a.create_input_stream ());
  }

  private static org.omg.CORBA.TypeCode __typeCode = null;
  synchronized public static org.omg.CORBA.TypeCode type ()
  {
    if (__typeCode == null)
    {
      __typeCode = org.omg.CORBA.ORB.init ().create_interface_tc (org.cristalise.kernel.entity.AgentHelper.id (), "Agent");
    }
    return __typeCode;
  }

  public static String id ()
  {
    return _id;
  }

  public static org.cristalise.kernel.entity.Agent read (org.omg.CORBA.portable.InputStream istream)
  {
    return narrow (istream.read_Object (_AgentStub.class));
  }

  public static void write (org.omg.CORBA.portable.OutputStream ostream, org.cristalise.kernel.entity.Agent value)
  {
    ostream.write_Object ((org.omg.CORBA.Object) value);
  }

  public static org.cristalise.kernel.entity.Agent narrow (org.omg.CORBA.Object obj)
  {
    if (obj == null)
      return null;
    else if (obj instanceof org.cristalise.kernel.entity.Agent)
      return (org.cristalise.kernel.entity.Agent)obj;
    else if (!obj._is_a (id ()))
      throw new org.omg.CORBA.BAD_PARAM ();
    else
    {
      org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl)obj)._get_delegate ();
      org.cristalise.kernel.entity._AgentStub stub = new org.cristalise.kernel.entity._AgentStub ();
      stub._set_delegate(delegate);
      return stub;
    }
  }

  public static org.cristalise.kernel.entity.Agent unchecked_narrow (org.omg.CORBA.Object obj)
  {
    if (obj == null)
      return null;
    else if (obj instanceof org.cristalise.kernel.entity.Agent)
      return (org.cristalise.kernel.entity.Agent)obj;
    else
    {
      org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl)obj)._get_delegate ();
      org.cristalise.kernel.entity._AgentStub stub = new org.cristalise.kernel.entity._AgentStub ();
      stub._set_delegate(delegate);
      return stub;
    }
  }

}
