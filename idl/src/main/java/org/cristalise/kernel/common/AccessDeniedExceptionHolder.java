package org.cristalise.kernel.common;

/**
* org/cristalise/kernel/common/AccessDeniedExceptionHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from /home/dev/workspace/cristal-ise/idl/src/main/idl/CommonExceptions.idl
* Friday, June 11, 2021 7:33:26 PM PHT
*/


/**************************************************************************
    * The given agent is not permitted to perform the requested action
    **************************************************************************/
public final class AccessDeniedExceptionHolder implements org.omg.CORBA.portable.Streamable
{
  public org.cristalise.kernel.common.AccessDeniedException value = null;

  public AccessDeniedExceptionHolder ()
  {
  }

  public AccessDeniedExceptionHolder (org.cristalise.kernel.common.AccessDeniedException initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = org.cristalise.kernel.common.AccessDeniedExceptionHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    org.cristalise.kernel.common.AccessDeniedExceptionHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return org.cristalise.kernel.common.AccessDeniedExceptionHelper.type ();
  }

}