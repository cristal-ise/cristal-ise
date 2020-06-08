package org.cristalise.kernel.common;


/**
* org/cristalise/kernel/common/PersistencyExceptionHelper.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from /home/vagrant/workspace/cristal-ise/idl/src/main/idl/CommonExceptions.idl
* Friday, April 10, 2020 7:30:08 PM CEST
*/


/**************************************************************************
    * Error during storing/retrieving objects
    **************************************************************************/
abstract public class PersistencyExceptionHelper
{
  private static String  _id = "IDL:org.cristalise.kernel/common/PersistencyException:1.0";

  public static void insert (org.omg.CORBA.Any a, org.cristalise.kernel.common.PersistencyException that)
  {
    org.omg.CORBA.portable.OutputStream out = a.create_output_stream ();
    a.type (type ());
    write (out, that);
    a.read_value (out.create_input_stream (), type ());
  }

  public static org.cristalise.kernel.common.PersistencyException extract (org.omg.CORBA.Any a)
  {
    return read (a.create_input_stream ());
  }

  private static org.omg.CORBA.TypeCode __typeCode = null;
  private static boolean __active = false;
  synchronized public static org.omg.CORBA.TypeCode type ()
  {
    if (__typeCode == null)
    {
      synchronized (org.omg.CORBA.TypeCode.class)
      {
        if (__typeCode == null)
        {
          if (__active)
          {
            return org.omg.CORBA.ORB.init().create_recursive_tc ( _id );
          }
          __active = true;
          org.omg.CORBA.StructMember[] _members0 = new org.omg.CORBA.StructMember [0];
          org.omg.CORBA.TypeCode _tcOf_members0 = null;
          __typeCode = org.omg.CORBA.ORB.init ().create_exception_tc (org.cristalise.kernel.common.PersistencyExceptionHelper.id (), "PersistencyException", _members0);
          __active = false;
        }
      }
    }
    return __typeCode;
  }

  public static String id ()
  {
    return _id;
  }

  public static org.cristalise.kernel.common.PersistencyException read (org.omg.CORBA.portable.InputStream istream)
  {
    org.cristalise.kernel.common.PersistencyException value = new org.cristalise.kernel.common.PersistencyException ();
    // read and discard the repository ID
    istream.read_string ();
    return value;
  }

  public static void write (org.omg.CORBA.portable.OutputStream ostream, org.cristalise.kernel.common.PersistencyException value)
  {
    // write the repository ID
    ostream.write_string (id ());
  }

}
