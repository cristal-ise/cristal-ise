package org.cristalise.kernel.common;


/**
* org/cristalise/kernel/common/GTimeStampHelper.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from /home/cristal-ise/workspace/cristal-ise/idl/src/main/idl/CommonTypes.idl
* Wednesday, 11 December 2019 17:43:31 o'clock CET
*/

abstract public class GTimeStampHelper
{
  private static String  _id = "IDL:org.cristalise.kernel/common/GTimeStamp:1.0";

  public static void insert (org.omg.CORBA.Any a, org.cristalise.kernel.common.GTimeStamp that)
  {
    org.omg.CORBA.portable.OutputStream out = a.create_output_stream ();
    a.type (type ());
    write (out, that);
    a.read_value (out.create_input_stream (), type ());
  }

  public static org.cristalise.kernel.common.GTimeStamp extract (org.omg.CORBA.Any a)
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
          org.omg.CORBA.StructMember[] _members0 = new org.omg.CORBA.StructMember [7];
          org.omg.CORBA.TypeCode _tcOf_members0 = null;
          _tcOf_members0 = org.omg.CORBA.ORB.init ().get_primitive_tc (org.omg.CORBA.TCKind.tk_long);
          _members0[0] = new org.omg.CORBA.StructMember (
            "mYear",
            _tcOf_members0,
            null);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().get_primitive_tc (org.omg.CORBA.TCKind.tk_long);
          _members0[1] = new org.omg.CORBA.StructMember (
            "mMonth",
            _tcOf_members0,
            null);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().get_primitive_tc (org.omg.CORBA.TCKind.tk_long);
          _members0[2] = new org.omg.CORBA.StructMember (
            "mDay",
            _tcOf_members0,
            null);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().get_primitive_tc (org.omg.CORBA.TCKind.tk_long);
          _members0[3] = new org.omg.CORBA.StructMember (
            "mHour",
            _tcOf_members0,
            null);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().get_primitive_tc (org.omg.CORBA.TCKind.tk_long);
          _members0[4] = new org.omg.CORBA.StructMember (
            "mMinute",
            _tcOf_members0,
            null);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().get_primitive_tc (org.omg.CORBA.TCKind.tk_long);
          _members0[5] = new org.omg.CORBA.StructMember (
            "mSecond",
            _tcOf_members0,
            null);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().get_primitive_tc (org.omg.CORBA.TCKind.tk_long);
          _members0[6] = new org.omg.CORBA.StructMember (
            "mTimeOffset",
            _tcOf_members0,
            null);
          __typeCode = org.omg.CORBA.ORB.init ().create_struct_tc (org.cristalise.kernel.common.GTimeStampHelper.id (), "GTimeStamp", _members0);
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

  public static org.cristalise.kernel.common.GTimeStamp read (org.omg.CORBA.portable.InputStream istream)
  {
    org.cristalise.kernel.common.GTimeStamp value = new org.cristalise.kernel.common.GTimeStamp ();
    value.mYear = istream.read_long ();
    value.mMonth = istream.read_long ();
    value.mDay = istream.read_long ();
    value.mHour = istream.read_long ();
    value.mMinute = istream.read_long ();
    value.mSecond = istream.read_long ();
    value.mTimeOffset = istream.read_long ();
    return value;
  }

  public static void write (org.omg.CORBA.portable.OutputStream ostream, org.cristalise.kernel.common.GTimeStamp value)
  {
    ostream.write_long (value.mYear);
    ostream.write_long (value.mMonth);
    ostream.write_long (value.mDay);
    ostream.write_long (value.mHour);
    ostream.write_long (value.mMinute);
    ostream.write_long (value.mSecond);
    ostream.write_long (value.mTimeOffset);
  }

}