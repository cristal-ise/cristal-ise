package org.cristalise.restapi;

import java.io.IOException;
import java.util.LinkedHashMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.map.ObjectMapper;
import org.cristalise.kernel.utils.Logger;

public class RestHandler {

	private ObjectMapper mapper;
	
	public RestHandler() {
		mapper = new ObjectMapper();
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
}
