package org.cristalise.kernel.entity.ItemPackage;


/**
* org/cristalise/kernel/entity/ItemPackage/OctetSequenceHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from /home/vagrant/workspace/cristal-ise/idl/src/main/idl/Entity.idl
* Saturday, March 14, 2020 10:40:43 AM CET
*/

public final class OctetSequenceHolder implements org.omg.CORBA.portable.Streamable
{
  public byte value[] = null;

  public OctetSequenceHolder ()
  {
  }

  public OctetSequenceHolder (byte[] initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = org.cristalise.kernel.entity.ItemPackage.OctetSequenceHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    org.cristalise.kernel.entity.ItemPackage.OctetSequenceHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return org.cristalise.kernel.entity.ItemPackage.OctetSequenceHelper.type ();
  }

}
