package org.cristalise.restapi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.util.Date;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;

import org.codehaus.jackson.map.ObjectMapper;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.SystemKey;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.InvalidAgentPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;

public class RestHandler {

	private ObjectMapper mapper;
	private static SecretKey cookieKey;
	private static Cipher encryptCipher;
	private static Cipher decryptCipher;
	public static final String COOKIENAME = "cauth";
	
	public RestHandler() {
		mapper = new ObjectMapper();
		if (cookieKey == null) try {
			try {
				initKeys(256);
			} catch (InvalidKeyException ex) {
				if (Gateway.getProperties().getBoolean("REST.allowWeakKey", false) == false)
					Logger.die("Weak cookie crypto not allowed, and unlimited strength crypto not installed.");
				Logger.msg("Unlimited crypto not installed. Trying 128-bit key.");
				initKeys(128);
			}
		} catch (Exception e) {
			Logger.error(e);
			throw new WebApplicationException("Error initializing cookie encryption: "+e.getMessage());
		} 
	}
	
	private static void initKeys(int keySize) throws Exception {
		KeyGenerator kgen = KeyGenerator.getInstance("AES");
		kgen.init(keySize);
		cookieKey = kgen.generateKey();
		System.out.println("Cookie key: "+DatatypeConverter.printBase64Binary(cookieKey.getEncoded()));
		encryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		encryptCipher.init(Cipher.ENCRYPT_MODE, cookieKey);
		decryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		decryptCipher.init(Cipher.DECRYPT_MODE, cookieKey, new IvParameterSpec(encryptCipher.getIV()));
	}
	
	protected String makeCookie(AuthData auth) throws IllegalBlockSizeException, BadPaddingException {
		byte[] bytes = encryptCipher.doFinal(auth.getBytes());
		return DatatypeConverter.printBase64Binary(bytes);
	}
	
	private AuthData readCookie(Cookie cookie) throws InvalidAgentPathException, IllegalBlockSizeException, BadPaddingException, InvalidDataException {
		byte[] bytes = DatatypeConverter.parseBase64Binary(cookie.getValue());
		return new AuthData(decryptCipher.doFinal(bytes));
	}
	
	public Response toJSON(Object data) {
		String childPathDataJSON;
		try {
			childPathDataJSON = mapper.writeValueAsString(data);
		} catch (IOException e) {
			Logger.error(e);
			throw new WebApplicationException("Problem building response JSON: "+e.getMessage());
		}
		return Response.ok(childPathDataJSON).build();
	}
	
	public void checkAuth(Cookie authCookie) {
		if (!Gateway.getProperties().getBoolean("REST.requireLoginCookie", true)) return;
		if (authCookie == null) 
			throw new WebApplicationException("Login required", 401);
		try {
			readCookie(authCookie);
			return;
		} catch (InvalidAgentPathException | InvalidDataException e) {
			throw new WebApplicationException("Invalid login", 400);
		} catch (Exception e) {
			Logger.error(e);
			throw new WebApplicationException("Error reading authCookie");
		}
	}
	
	public AgentProxy getAgent(String agentName, Cookie authCookie) {
		AgentPath agentPath = null;
		
		if (authCookie!=null) {
			try {
				AuthData auth = readCookie(authCookie);
				agentPath = auth.agent;
			} catch (Exception e) {
				Logger.error(e);
				throw new WebApplicationException("Bad auth cookie", 400);
			}	
		}
		else if (agentName!=null && !(Gateway.getProperties().getBoolean("REST.requireLoginCookie", true))) {
			try {
				agentPath = Gateway.getLookup().getAgentPath(agentName);
			} catch (ObjectNotFoundException e) {
				Logger.error(e);
				throw new WebApplicationException("Agent '"+agentName+"' not found", 404);
			}
		}
		else
			throw new WebApplicationException(401);

		try {
			return (AgentProxy)Gateway.getProxyManager().getProxy(agentPath);
		} catch (ObjectNotFoundException e) {
			Logger.error(e);
			throw new WebApplicationException("Agent not found", 404);
		}
	}
	
	public class AuthData {
		AgentPath agent;
		Date timestamp;
		
		public AuthData(AgentPath agent) {
			this.agent = agent;
			timestamp = new Date();
		}
		
		public AuthData(byte[] bytes) throws InvalidAgentPathException, InvalidDataException {
			ByteBuffer buf = ByteBuffer.wrap(bytes);
			SystemKey sysKey = new SystemKey(buf.getLong(), buf.getLong());
			agent = new AgentPath(new ItemPath(sysKey));
			timestamp = new Date(buf.getLong());
			int cookieLife = Gateway.getProperties().getInt("REST.loginCookieLife", 0);
			if (cookieLife > 0 && (new Date().getTime()-timestamp.getTime())/1000 > cookieLife)
				throw new InvalidDataException("Cookie too old");
		}
		
		public byte[] getBytes() {
			byte[] bytes = new byte[Long.SIZE*3];
			SystemKey sysKey = agent.getSystemKey();
			ByteBuffer buf = ByteBuffer.wrap(bytes);
			buf.putLong(sysKey.msb);
			buf.putLong(sysKey.lsb);
			buf.putLong(timestamp.getTime());
			return bytes;
		}
	}
}
