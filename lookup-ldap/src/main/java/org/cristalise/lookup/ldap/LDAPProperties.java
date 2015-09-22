package org.cristalise.lookup.ldap;

/**
 * Directory Lookup Service
 * 
 * This represent 
 * 
 */

import javax.xml.bind.DatatypeConverter;

import org.cristalise.kernel.utils.ObjectProperties;


/**
 * example:
 * 
 * <pre>
 * # LDAP Lookup config
 * # use the ApacheDS 2.0.0 M15 available using the port 10389
 * LDAP.GlobalPath=dc=cristalosgiglobal
 * LDAP.RootPath=cn=cristalosgiroot
 * LDAP.LocalPath=cn=cristalosgilocal
 * LDAP.port=10389
 * LDAP.host=localhost
 * LDAP.user=uid=admin,ou=system
 * LDAP.password=xxxxxx
 * or
 * LDAP.password64=xxxxxx
 * </pre>
 * 
 * @version $Revision: 1.16 $ $Date: 2005/10/12 12:51:54 $
 * @author $Author: abranson $
 * @author ogattaz
 */
public class LDAPProperties {

	private static final String BAD_PASSWORD_MESSAGE = "bad base64 password value";
	
	public static final String LDAP_PROP_GPATH = "LDAP.GlobalPath";
	public static final String LDAP_PROP_HOST = "LDAP.host";
	public static final String LDAP_PROP_LPATH = "LDAP.LocalPath";
	public static final String LDAP_PROP_PASS = "LDAP.password";
	public static final String LDAP_PROP_PASS64 = "LDAP.password64";
	public static final String LDAP_PROP_PORT = "LDAP.port";
	public static final String LDAP_PROP_RPATH = "LDAP.RootPath";
	public static final String LDAP_PROP_USER = "LDAP.user";
	public static final String LDAP_PROP_TIMEOUT = "LDAP.timeOut";
	public static final String LDAP_PROP_USETLS = "LDAP.useTLS";
	public static final String LDAP_PROP_IGNORECERTERRORS = "LDAP.ignoreCertErrors";
	
	public String mGlobalPath = null; // o=cern.ch
	public String mHost = null;
	public String mLocalPath = null; // cn=lab27
	public String mPassword = null;
	public Integer mPort = null;
	public String mRootPath = null; // cn=cristal2
	public String mUser = null;
	public Integer mTimeOut = null;
	public boolean mUseTLS;
	public boolean mIgnoreCertErrors;

	/**
	 * @param aObjectProps
	 *            an ObjectProperties instance comming from clc file for exemple
	 */
	public LDAPProperties(final ObjectProperties aObjectProps) {

		if (aObjectProps != null) {

			mGlobalPath = aObjectProps.getProperty(LDAP_PROP_GPATH);

			mRootPath = aObjectProps.getProperty(LDAP_PROP_RPATH);
			if (mRootPath != null) {
				mRootPath += "," + mGlobalPath;
			}

			mLocalPath = aObjectProps.getProperty(LDAP_PROP_LPATH);
			if (mLocalPath != null) {
				mLocalPath += "," + mRootPath;
			}

			mPort = aObjectProps.getInt(LDAP_PROP_PORT, 389);
			mHost = aObjectProps.getProperty(LDAP_PROP_HOST);
			mTimeOut = aObjectProps.getInt(LDAP_PROP_TIMEOUT, 0);
			mUser = aObjectProps.getProperty(LDAP_PROP_USER);
			mPassword = aObjectProps.getProperty(LDAP_PROP_PASS);
			mUseTLS = aObjectProps.getBoolean(LDAP_PROP_USETLS, false);
			mIgnoreCertErrors = aObjectProps.getBoolean(LDAP_PROP_IGNORECERTERRORS, false);

			// if raw password not available, try to find base64 one
			if (mPassword == null) {
				mPassword = aObjectProps.getProperty(LDAP_PROP_PASS64);
				// if base64 password available
				if (mPassword != null) {
					mPassword = translateBase64OPassword(mPassword);
				}
			}
		}
	}

	/**
	 * @param aPropertyName
	 *            the name of the property associated to the member
	 * @param aMemberValue
	 *            the value to check
	 * @return true if valid
	 * @throws IllegalArgumentException
	 *             if not valid
	 */
	private boolean checkMemberValidity(final String aPropertyName,
			final String aMemberValue) throws IllegalArgumentException {

		if (isMemberValueValid(aMemberValue))
			return true;

		throw new IllegalArgumentException(String.format(
				"The LDAP property [%s] is not valid. The member value=[%s]",
				aPropertyName, aMemberValue));
	}

	/**
	 * @param aValue
	 *            the value to be checked
	 * @return true if not null and not empty
	 */
	private boolean checkPasswordValidity(final String aPasswordValue)
			throws IllegalArgumentException {

		if (checkMemberValidity(LDAP_PROP_PASS, aPasswordValue)) {

			if (aPasswordValue.contains(BAD_PASSWORD_MESSAGE)) {
				throw new IllegalArgumentException(
						String.format(
								"The LDAP property [%s] is not valid. The member value=[%s]",
								LDAP_PROP_PASS, aPasswordValue));
			}
		}
		return true;
	}

	/**
	 * @return true is valid
	 * @throws IllegalArgumentException
	 *             if one of the members is not valid (null or empty)
	 */
	public boolean checkValidity() throws IllegalArgumentException {

		return checkMemberValidity(LDAP_PROP_GPATH, mGlobalPath)
				&& checkMemberValidity(LDAP_PROP_RPATH, mRootPath)
				&& checkMemberValidity(LDAP_PROP_LPATH, mLocalPath)
				&& checkMemberValidity(LDAP_PROP_HOST, mHost)
				&& checkMemberValidity(LDAP_PROP_USER, mUser)
				&& checkPasswordValidity(mPassword);
	}

	/**
	 * @param aValue
	 *            the value to be checked
	 * @return true if not null and not empty
	 */
	private boolean isMemberValueValid(final String aValue) {

		return (aValue != null && !aValue.isEmpty());
	}

	/**
	 * @return true if the password is not null, not empty and is decoded id the
	 *         passed property is a password64 one
	 */
	public boolean isPasswordValid() {
		try {
			return checkPasswordValidity(mPassword);
		} catch (IllegalArgumentException ex) {
			return false;
		}
	}

	/**
	 * @param aBase6Password
	 *            the encoded password
	 * @return the decodded password or a dummy phrase which cause an explicit
	 *         error when it will be used during the connection
	 */
	private String translateBase64OPassword(final String aBase6Password) {

		try {
			// DatatypeConverter tool class available since java 1.5.
			// Throws IllegalArgumentException if value not conform
			return new String(
					DatatypeConverter.parseBase64Binary(aBase6Password));

		} catch (IllegalArgumentException ex) {
			return String.format("#### %s [%s] ####", BAD_PASSWORD_MESSAGE,
					aBase6Password);
		}
	}
}
