package org.cristalise.dev;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lifecycle.ActivityDef;
import org.cristalise.kernel.lifecycle.CompositeActivityDef;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.persistency.outcome.OutcomeInitiator;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.DescriptionObject;
import org.cristalise.kernel.utils.Logger;


public class DevObjectOutcomeInitiator implements OutcomeInitiator {

	public DevObjectOutcomeInitiator() {
	}

	@Override
	public String initOutcome(Job job) throws InvalidDataException {
		String type = job.getActPropString("SchemaType");
		
		// create empty object for activities and state machine
		DescriptionObject emptyObj = null;
		if (type.equals("CompositeActivityDef"))
			emptyObj = new CompositeActivityDef();
		else if (type.equals("ElementaryActivityDef"))
			emptyObj = new ActivityDef();
		else if (type.equals("StateMachine"))
			emptyObj = new StateMachine();
		
		if (emptyObj != null) {
			try {
				emptyObj.setName(job.getItemProxy().getName());
				return Gateway.getMarshaller().marshall(emptyObj);
			} catch (Exception e) {
				Logger.error("Error creating empty "+type);
				Logger.error(e);
				return null;
			}
		}
			
		// else load empty one from factory
		DomainPath factoryPath; String schema;
		if (type.equals("Schema")) {
			factoryPath = new DomainPath("/desc/dev/SchemaFactory");
			schema = "Schema";
		}
		else if (type.equals("Script")) {
			factoryPath = new DomainPath("/desc/dev/ScriptFactory");
			schema = "Script";
		}
		else 
			throw new InvalidDataException("Unknown dev object type: "+type);
		ItemProxy factory;
		Viewpoint newInstance;
		try {
			factory = Gateway.getProxyManager().getProxy(factoryPath);
			newInstance = factory.getViewpoint(schema, "last");
			return newInstance.getOutcome().getData();
		} catch (Exception e) {
			Logger.error(e);
			throw new InvalidDataException("Error loading new "+schema);
		}
			
	}

}
