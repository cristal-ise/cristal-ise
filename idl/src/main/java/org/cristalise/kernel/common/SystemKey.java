package org.cristalise.kernel.common;


/**
* org/cristalise/kernel/common/SystemKey.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from /home/vagrant/workspace/cristal-ise/idl/src/main/idl/CommonTypes.idl
* Saturday, March 14, 2020 10:40:43 AM CET
*/

public final class SystemKey implements org.omg.CORBA.portable.IDLEntity
{
  public long msb = (long)0;
  public long lsb = (long)0;

  public SystemKey ()
  {
  } // ctor

  public SystemKey (long _msb, long _lsb)
  {
    msb = _msb;
    lsb = _lsb;
  } // ctor

} // class SystemKey
