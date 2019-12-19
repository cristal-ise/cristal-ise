package org.cristalise.kernel.common;


/**
* org/cristalise/kernel/common/InvalidDataException.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from /home/cristal-ise/workspace/cristal-ise/idl/src/main/idl/CommonExceptions.idl
* Wednesday, 11 December 2019 17:43:31 o'clock CET
*/


/**************************************************************************
    * Either the supplied data, or the relevant description, was invalid.
    **************************************************************************/
public final class InvalidDataException extends org.omg.CORBA.UserException
{

  public InvalidDataException ()
  {
    super(InvalidDataExceptionHelper.id());
  } // ctor


  public InvalidDataException (String $reason)
  {
    super(InvalidDataExceptionHelper.id() + "  " + $reason);
  } // ctor

} // class InvalidDataException
