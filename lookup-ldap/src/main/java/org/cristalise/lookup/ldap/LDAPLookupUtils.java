/*
 * Lookup helper class.
 */

package org.cristalise.lookup.ldap;

//import netscape.ldap.*;
//import netscape.ldap.util.*;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

import org.apache.xerces.impl.dv.util.Base64;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.utils.Logger;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPDN;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPModification;
import com.novell.ldap.LDAPSearchConstraints;
import com.novell.ldap.LDAPSearchResults;

/**
 * @version $Revision: 1.74 $ $Date: 2006/03/03 13:52:21 $
 * @author  $Author: abranson $
 */

final public class LDAPLookupUtils
{
    static final char[] META_CHARS = {'+', '=', '"', ',', '<', '>', ';', '/'};
    static final String[] META_ESCAPED = {"2B", "3D", "22", "2C", "3C", "3E", "3B", "2F"};
	private static final Random RANDOM = new SecureRandom();
	
    static public LDAPEntry getEntry(LDAPConnection ld, String dn,int dereference)
        throws ObjectNotFoundException
    {
        try {
			LDAPSearchConstraints searchCons = new LDAPSearchConstraints();
	        searchCons.setBatchSize(0);
    	    searchCons.setDereference(dereference);
            LDAPEntry thisEntry = ld.read(dn,searchCons);
            if (thisEntry != null) return thisEntry;
        } catch (LDAPException ex) {
            throw new ObjectNotFoundException("LDAP Exception for dn:"+dn+": \n"+ex.getMessage());
        }
        throw new ObjectNotFoundException(dn+" does not exist");

    }

    public static String generateUserPassword(String pass) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA");
        md.reset();
        String salt = generateSalt(16);
        md.update((pass+salt).getBytes());
        byte[] hash = md.digest();
        byte[] saltBytes = salt.getBytes();
        byte[] allBytes = new byte[hash.length+saltBytes.length];
        System.arraycopy(hash, 0, allBytes, 0, hash.length);
        System.arraycopy(saltBytes, 0, allBytes, hash.length, saltBytes.length);
        
        StringBuffer encPassword = new StringBuffer("{SSHA}");
        encPassword.append(Base64.encode(allBytes));
        return encPassword.toString();
    }
    
    public static String generateSalt(int size) {
    	byte[] salt = new byte[size];
    	RANDOM.nextBytes(salt);
    	return String.valueOf(salt);
    }
    
    /**
     * Utility method to connect to an LDAP server
     * @param lp LDAP properties to connect with
     * @return a novell LDAPConnection object
     * @throws LDAPException when the connection was unsuccessful
     */
    public static LDAPConnection createConnection(LDAPProperties lp) throws LDAPException {
        LDAPConnection ld;
        if (lp.mTimeOut == 0) ld = new LDAPConnection();
        else ld = new LDAPConnection(lp.mTimeOut);

        Logger.msg(3, "LDAPLookup - connecting to " + lp.mHost);
        ld.connect(lp.mHost, Integer.valueOf(lp.mPort).intValue());

        Logger.msg(3, "LDAPLookup - authenticating user:" + lp.mUser);
        ld.bind( LDAPConnection.LDAP_V3, lp.mUser,
                 String.valueOf(lp.mPassword).getBytes());

        Logger.msg(3, "LDAPLookup - authentication successful");
        LDAPSearchConstraints searchCons = new LDAPSearchConstraints();
        searchCons.setMaxResults(0);
        ld.setConstraints(searchCons);

        return ld;
    }

    //Given a DN, return an LDAP Entry
    static public LDAPEntry getEntry(LDAPConnection ld, String dn)
        throws ObjectNotFoundException
    {
        return getEntry(ld, dn, LDAPSearchConstraints.DEREF_NEVER);
    }

    static public String getFirstAttributeValue(LDAPEntry anEntry, String attribute) throws ObjectNotFoundException
    {
        LDAPAttribute attr = anEntry.getAttribute(attribute);
        if (attr==null)
            throw new ObjectNotFoundException("No attributes named '"+attribute+"'");
        return (String)attr.getStringValues().nextElement();
    }

    static public String[] getAllAttributeValues(LDAPEntry anEntry, String attribute)
    {
        LDAPAttribute attr = anEntry.getAttribute(attribute);
        if (attr!=null)
            return attr.getStringValueArray();

        return new String[0];

    }

    static public boolean existsAttributeValue(LDAPEntry anEntry, String attribute, String value)
    {
        LDAPAttribute attr = anEntry.getAttribute(attribute);
        if (attr!=null)
        {
            String[] attrValues = new String[attr.size()];
            attrValues = attr.getStringValueArray();
            for (int i=0;i<attr.size();i++)
                if (attrValues[i].equalsIgnoreCase(value))
                    return true;
        }
        return false;
    }
	static public boolean hasOneAttributeValue(LDAPEntry anEntry, String attribute) throws ObjectNotFoundException
	{
		int j =0;
		LDAPAttribute attr = anEntry.getAttribute(attribute);
		if (attr==null)
            throw new ObjectNotFoundException("No attributes named '"+attribute+"'");
		j=attr.size();
		return j==1;
	}

	//this is for a single-valued attribute
    static public void setAttributeValue(LDAPConnection ld, LDAPEntry anEntry, String attribute, String newValue)
    throws ObjectNotFoundException, ObjectCannotBeUpdated
    {
        try {
            if (!hasOneAttributeValue(anEntry, attribute))
                throw new ObjectCannotBeUpdated("Attribute "+attribute + " of entry " + anEntry.getDN()+" has more than one value");
        } catch (ObjectNotFoundException ex) {
            addAttributeValue(ld, anEntry, attribute, newValue);
        }
        try
        {
       		ld.modify(anEntry.getDN(),new LDAPModification(LDAPModification.REPLACE,new LDAPAttribute(attribute,newValue)));
        }
        catch (LDAPException ex)
        {
            Logger.error(ex);
            throw new ObjectCannotBeUpdated("Attribute " + attribute + " of entry " + anEntry.getDN() + " could not be modified");
        }
    }


	//this is for a multi-valued attribute eg uniqueMember
    static public void addAttributeValue(LDAPConnection ld, LDAPEntry anEntry, String attribute, String value)
    throws ObjectCannotBeUpdated
    {
        try
        {
            ld.modify(anEntry.getDN(),new LDAPModification(LDAPModification.ADD, new LDAPAttribute(attribute,value)));
        }
        catch (LDAPException ex)
        {
            Logger.error(ex);
            throw new ObjectCannotBeUpdated("Attribute " + attribute + " of entry " + anEntry.getDN() + " could not be added.");
        }
    }

    //this is for a multi-valued attribute eg uniqueMember
    static public void removeAttributeValue(LDAPConnection ld, LDAPEntry anEntry, String attribute, String value)
    throws ObjectCannotBeUpdated
    {
            try
            {
       			ld.modify(anEntry.getDN(),new LDAPModification(LDAPModification.DELETE,new LDAPAttribute(attribute,value)));
			}
            catch (LDAPException ex)
            {
                Logger.error(ex);
                throw new ObjectCannotBeUpdated("Attribute " + attribute + " of entry " + anEntry.getDN() + " could not be deleted");
            }
    }

    static public boolean exists(LDAPConnection ld, String name)
    {
        try {
            String[] attr = { LDAPConnection.NO_ATTRS };
            LDAPEntry anEntry=ld.read(name,attr);
            if (anEntry!=null)
                return true;
        } catch (LDAPException ex)
        {
            Logger.debug(9, "LDAPLookupUtils.exists("+name+": "+ex.getMessage());
            return false;
        }
        return false;
    }

    static public void addEntry(LDAPConnection ld,LDAPEntry myEntry)
    throws ObjectAlreadyExistsException, LDAPException
    {
        try
        {
            ld.add( myEntry );
        }
        catch( LDAPException ex ) {
            if (ex.getResultCode() == LDAPException.ENTRY_ALREADY_EXISTS)
                throw new ObjectAlreadyExistsException("Entry already present." + myEntry.getDN());
            throw ex;
        }
    }

    static public boolean hasChildren(LDAPConnection ld, String dn, String filter)
    {
        String[] attr = { LDAPConnection.NO_ATTRS };
        LDAPSearchConstraints searchCons = new LDAPSearchConstraints();
        searchCons.setBatchSize(0);
        searchCons.setDereference(LDAPSearchConstraints.DEREF_NEVER);

        try
        {
            LDAPSearchResults res = ld.search(dn,LDAPConnection.SCOPE_ONE,filter,attr,false,searchCons);
            if (res.hasMore())
                return true;
        }
        catch (LDAPException ex)
        {
            Logger.error(ex);
        }
        return false;
    }

    //returns list of dns
    static public String[] getChildrenDNs(LDAPConnection ld, String dn, String filter)
    {
        String[] result = null;
        String[] attr = { LDAPConnection.NO_ATTRS };
        LDAPSearchConstraints searchCons = new LDAPSearchConstraints();
        searchCons.setBatchSize(0);
        searchCons.setDereference(LDAPSearchConstraints.DEREF_NEVER);

        try
        {
            LDAPSearchResults res = ld.search(dn,LDAPConnection.SCOPE_ONE,filter,attr,false,searchCons);
            result = new String[res.getCount()];
            int i=0;
            while (res.hasMore())
            {
                LDAPEntry findEntry=res.next();
                if (findEntry!=null)
                {
                    result[i++] = new String(findEntry.getDN());
                }
            }
        }
        catch (Exception ex)
        {
            Logger.error(ex);
        }
        return result;
    }

    static public void delete(LDAPConnection ld, String dn)
    throws LDAPException
    {
        try
        {
            Logger.msg(7, "LDAPLookupUtils.delete() - "+dn);
            ld.delete(dn);
        }
        catch (LDAPException ex)
        {
            Logger.error("LDAPLookupUtils.remove() - Cannot remove "+dn+": " + ex.getMessage());
            throw ex;
        }
    }

    //param dn is the DN of the name
    //param name is the name of the node (also the RDN)
    //example: cn=lab27,o=cern.ch  lab27
    //example: cn=product, cn=domain, cn=lab27, cn= cristal2, o=cern.ch  product
    static public void createCristalContext(LDAPConnection ld, String dn)
    {
    	if (LDAPLookupUtils.exists(ld,dn))
    		return;
        try
        {
            String name = LDAPDN.explodeDN(dn,true)[0];
            LDAPAttributeSet attrs = new LDAPAttributeSet();
            attrs.add(new LDAPAttribute("cn",name));
            String objectclass_values[] = new String[1];
            objectclass_values[0] = "cristalcontext";
            if (name.equals("last"))
            	attrs.add(new LDAPAttribute("intsyskey", "0"));

    		attrs.add(new LDAPAttribute("objectclass",objectclass_values));

            LDAPLookupUtils.addEntry(ld,new LDAPEntry(dn,attrs));
        }
        catch (Exception ex)
        {
            Logger.error("LDAPLookupUtils.createCristalContext() " + ex.toString());
        }
    }

    static public void createOrganizationContext(LDAPConnection ld, String dn)
    {
    	if (LDAPLookupUtils.exists(ld,dn))
    		return;

        try
        {
            String name = LDAPDN.explodeDN(dn,true)[0];
            LDAPAttributeSet attrs = new LDAPAttributeSet();
            //No idea why this worked, or why it suddenly stopped working when we moved to maven
            //attrs.add(new LDAPAttribute("objectclass","top"));
            attrs.add(new LDAPAttribute("objectclass","organization"));
            attrs.add(new LDAPAttribute("o",name));
            LDAPLookupUtils.addEntry(ld,new LDAPEntry(dn,attrs));
        }
        catch (Exception ex)
        {
            Logger.msg(ex.toString());
        }
    }
    
    public static String escapeDN (String name) {
        //From RFC 2253 and the / character for JNDI
        if (name == null) return null;
        String escapedStr = new String(name);

        //Backslash is both a Java and an LDAP escape character, so escape it first
        escapedStr = escapedStr.replaceAll("\\\\","\\\\");

        //Positional characters - see RFC 2253
        escapedStr = escapedStr.replaceAll("^#","\\\\23");  // TODO: active directory requires hash to be escaped everywhere
        escapedStr = escapedStr.replaceAll("^ | $","\\\\20");

        for (int i=0; i<META_CHARS.length; i++) {
            escapedStr = escapedStr.replaceAll("\\"+META_CHARS[i],"\\\\"+ META_ESCAPED[i]);
        }
        if (!name.equals(escapedStr)) Logger.msg(3, "LDAP DN "+name+" escaped to "+escapedStr);
        return escapedStr;
    }
    
    public static String unescapeDN (String dn) {
        //From RFC 2253 and the / character for JNDI
        String unescapedStr = new String(dn);

        //Positional characters - see RFC 2253
        unescapedStr = unescapedStr.replaceAll("^\\\\23", "#");  // TODO: active directory requires hash to be escaped everywhere
        unescapedStr = unescapedStr.replaceAll("^\\\\20|\\\\20$", " ");

        for (int i=0; i<META_CHARS.length; i++) {
        	unescapedStr = unescapedStr.replaceAll("\\\\" + META_ESCAPED[i], ""+META_CHARS[i]);
        }

        //Any remaining escaped backslashes
        unescapedStr = unescapedStr.replaceAll("\\\\","\\");
        
        if (!dn.equals(unescapedStr)) Logger.msg(3, "LDAP DN "+dn+" unescaped to "+unescapedStr);
        return unescapedStr;
    }

    public static String escapeSearchFilter (String filter) {
        //From RFC 2254
        String escapedStr = new String(filter);

        escapedStr = escapedStr.replaceAll("\\\\","\\\\5c");
        //escapedStr = escapedStr.replaceAll("\\*","\\\\2a"); // we need stars for searching
        escapedStr = escapedStr.replaceAll("\\(","\\\\28");
        escapedStr = escapedStr.replaceAll("\\)","\\\\29");
        if (!filter.equals(escapedStr)) Logger.msg(3, "LDAP Search Filter "+filter+" escaped to "+escapedStr);
        return escapedStr;
    }
}
