package org.cristalise.kernel.entity;


/**
* org/cristalise/kernel/entity/AgentPOATie.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from /home/vagrant/workspace/cristal-ise/idl/src/main/idl/Entity.idl
* Saturday, March 14, 2020 10:40:43 AM CET
*/


/**************************************************************************
    * Agent is a ManageableEntity that represents an Activity executor in the 
    * system. It holds a job list, which are persistent requests for execution 
    * from waiting activities assigned to a role that has such Job pushing enabled.
    **************************************************************************/
public class AgentPOATie extends AgentPOA
{

  // Constructors

  public AgentPOATie ( org.cristalise.kernel.entity.AgentOperations delegate ) {
      this._impl = delegate;
  }
  public AgentPOATie ( org.cristalise.kernel.entity.AgentOperations delegate , org.omg.PortableServer.POA poa ) {
      this._impl = delegate;
      this._poa      = poa;
  }
  public org.cristalise.kernel.entity.AgentOperations _delegate() {
      return this._impl;
  }
  public void _delegate (org.cristalise.kernel.entity.AgentOperations delegate ) {
      this._impl = delegate;
  }
  public org.omg.PortableServer.POA _default_POA() {
      if(_poa != null) {
          return _poa;
      }
      else {
          return super._default_POA();
      }
  }

  /** Supplies the new set of jobs for the given item and activity. The Agent should replace all existing jobs for that activity 
            * with the given set. This method should generally only be called by a workflow while performing an execution.
            *
            * @param itemKey the item which generated the jobs. The Agent should discard any existing jobs for that Item.
            * @param stepPath the Activity within the lifecycle of the item which the jobs relate to
            * @param newJobs an XML marshalled {@link org.cristalise.kernel.entity.agent.JobArrayList JobArrayList} containing the new Jobs
            **/
  public void refreshJobList (org.cristalise.kernel.common.SystemKey itemKey, String stepPath, String newJobs)
  {
    _impl.refreshJobList(itemKey, stepPath, newJobs);
  } // refreshJobList


  /** Add this Agent to the given role
  		  * @param roleName the new role to add
  		  * @throws ObjectNotFoundException when the role doesn't exist
  		  * @throws CannotManageException when an error occurs writing the data to LDAP
  		  **/
  public void addRole (String roleName) throws org.cristalise.kernel.common.ObjectNotFoundException, org.cristalise.kernel.common.CannotManageException
  {
    _impl.addRole(roleName);
  } // addRole


  /** Remove this Agent from the given role
  		  * @param the role name to remove
  		  * @throws CannotManageException when an error occurs writing the data to LDAP
  		  **/
  public void removeRole (String roleName) throws org.cristalise.kernel.common.ObjectNotFoundException, org.cristalise.kernel.common.CannotManageException
  {
    _impl.removeRole(roleName);
  } // removeRole


  /**
          * System generated unique key of the Entity. It is a 128 bit UUID, expressed as two 64 bit longs in the IDLs, but as a UUID object in the Java kernel. The ItemPath is used as the Item identifier in the kernel and its API, 
          which can be derived from either a UUID object or a SystemKey structure.
          **/
  public org.cristalise.kernel.common.SystemKey getSystemKey ()
  {
    return _impl.getSystemKey();
  } // getSystemKey


  /** Initialises a new Item. Initial properties and the lifecycle are supplied. They should come from the Item's description.
          *
          * @param agentKey the Agent doing the initialisation
          * @param itemProps The XML marshalled {@link org.cristalise.kernel.Property.PropertyArrayList PropertyArrayList} containing the initial
      	* Property objects of the Item
          * @param workflow The XML marshalled new lifecycle of the Item
          * @param collection The XML marshalled CollectionArrayList of the initial state of the Item's collections
          * @param viewpoint the XML marshalled Viewpoint to be stored to find the Outcome 
          * @param outcome the XML data to be stored 
          * @exception ObjectNotFoundException
          **/
  public void initialise (org.cristalise.kernel.common.SystemKey agentKey, String itemProps, String workflow, String collections, String viewpoint, String outcome) throws org.cristalise.kernel.common.AccessRightsException, org.cristalise.kernel.common.InvalidDataException, org.cristalise.kernel.common.PersistencyException, org.cristalise.kernel.common.ObjectNotFoundException, org.cristalise.kernel.common.InvalidCollectionModification
  {
    _impl.initialise(agentKey, itemProps, workflow, collections, viewpoint, outcome);
  } // initialise


  /**
          * Returns a chunk of XML which may be a serialized C2KLocalObject, or in the case of Outcomes is merely a fragment of XML.
          * 
          * @param path - All Entity data is arranged in a tree structure which uniquely identifies that object within the Entity it is contained, according to the following scheme:
          * <ul><li><code>LifeCycle/workflow</code> <i>(Items only)</i>: The Workflow object for this Item, containing the graph of activities defining the Item's lifecycle, and the Predefined Step container for data modification</li>
          * <li><code>Collection/{Name}</code> <i>(Items only)</i>: Collection objects defining links between Items</li>
          * <li><code>Property/{Name}</code>: Name value pairs to idenfity this Entity, define its type, and hold any other oft-changing indicators that would be heavy to extract from Outcomes</li>
  		* <li><code>AuditTrail/{ID}</code> <i>(Items only)</i>: Events describing all activity state changes in this Item.</li>
  		* <li><code>Outcome/{Schema name}/{Schema version}/{Event ID}</code> <i>(Items only)</i>: XML fragments resulting from the execution of an Activity, validated against the XML Schema defined by that activity.</li>
  		* <li><code>ViewPoint/{Schema name}/{Name}</code> <i>(Items only)</i>: A named pointer to the latest version of an Outcome, defined by the Activity.</li>
  		* <li><code>Job/{ID}</code> <i>(Agents only)</i>: A persistent Job, reflecting a request for execution of an Activity to this Agent. Not all roles create persistent Jobs like this, only those specifically flagged to do so.</li> 
  		*  
  		* @see org.cristalise.kernel.persistency.ClusterStorage#getPath
  		* 
          * @return The XML string of the data. All fragments except Outcomes will deserialize into objects with the kernel CastorXMLUtility available in the Gateway. 
          * 
          * @exception ObjectNotFoundException when the path is not present in this Entity
          * @exception AccessRightsException <i>Not currently implemented</i>
          * @exception PersistencyException when the path could not be loaded because of a problem with the storage subsystem.
          **/
  public String queryData (String path) throws org.cristalise.kernel.common.AccessRightsException, org.cristalise.kernel.common.ObjectNotFoundException, org.cristalise.kernel.common.PersistencyException
  {
    return _impl.queryData(path);
  } // queryData


  /**
          * Requests a transition of an Activity in this Item's workflow. If possible and permitted, an Event is 
          * generated and stored, the Activity's state is updated, which may cause the Workflow to proceed. If 
          * this transition requires Outcome data, this is supplied and stored, and a Viewpoint will be created 
          * or updated to point to this latest version. In the case of PredefinedSteps, additional data changes 
          * may be performed in the server data.
          * 
          * This method should not be called directly, as there is a large client side to activity execution 
          * implemented in the Proxy objects, such as script execution and schema validation.
          *
          * @param agentKey The SystemKey of the Agent. Some activities may be restricted in which roles may execute them.
          * Some transitions cause the activity to be assigned to the executing Agent.
          *
          * @param stepPath The path in the Workflow to the desired Activity
          *
          * @param transitionID The transition to be performed
          *
          * @param requestData The XML Outcome of the work defined by the Activity. Must be valid to the XML Schema,
          * though this is not verified on the server, rather in the AgentProxy in the Client API.
          *
          * @param attachmentType the MimeType of the attachment (can be empty)
          *
          * @param attachment binary data associated with the Outcome (can be empty)
          *
          * @throws AccessRightsException The Agent is not permitted to perform the operation. Either it does not 
          * have the correct role, or the Activity is reserved by another Agent. Also thrown when the given Agent ID doesn't exist.
          * @throws InvalidTransitionException The Activity is not in the correct state to make the requested transition.
          * @throws ObjectNotFoundException The Activity or a container of it does not exist.
          * @throws InvalidDataException An activity property for the requested Activity was invalid e.g. SchemaVersion was not a number. 
          Also thrown when an uncaught Java exception or error occurred.
          * @throws PersistencyException There was a problem committing the changes to storage.
          * @throws ObjectAlreadyExistsException Not normally thrown, but reserved for PredefinedSteps to throw if they need to.
          **/
  public String requestAction (org.cristalise.kernel.common.SystemKey agentKey, String stepPath, int transitionID, String requestData, String attachmentType, byte[] attachment) throws org.cristalise.kernel.common.AccessRightsException, org.cristalise.kernel.common.InvalidTransitionException, org.cristalise.kernel.common.ObjectNotFoundException, org.cristalise.kernel.common.InvalidDataException, org.cristalise.kernel.common.PersistencyException, org.cristalise.kernel.common.ObjectAlreadyExistsException, org.cristalise.kernel.common.InvalidCollectionModification
  {
    return _impl.requestAction(agentKey, stepPath, transitionID, requestData, attachmentType, attachment);
  } // requestAction

  public String delegatedAction (org.cristalise.kernel.common.SystemKey agentKey, org.cristalise.kernel.common.SystemKey delegateAgentKey, String stepPath, int transitionID, String requestData, String attachmentType, byte[] attachment) throws org.cristalise.kernel.common.AccessRightsException, org.cristalise.kernel.common.InvalidTransitionException, org.cristalise.kernel.common.ObjectNotFoundException, org.cristalise.kernel.common.InvalidDataException, org.cristalise.kernel.common.PersistencyException, org.cristalise.kernel.common.ObjectAlreadyExistsException, org.cristalise.kernel.common.InvalidCollectionModification
  {
    return _impl.delegatedAction(agentKey, delegateAgentKey, stepPath, transitionID, requestData, attachmentType, attachment);
  } // delegatedAction


  /**
          * Requests a transition of an Activity in this Item's workflow. If possible and permitted, an Event is 
          * generated and stored, the Activity's state is updated, which may cause the Workflow to proceed. If 
          * this transition requires Outcome data, this is supplied and stored, and a Viewpoint will be created 
          * or updated to point to this latest version. In the case of PredefinedSteps, additional data changes 
          * may be performed in the server data.
          * 
          * This method should not be called directly, as there is a large client side to activity execution 
          * implemented in the Proxy objects, such as script execution and schema validation.
          *
          * @param agentKey The SystemKey of the Agent. Some activities may be restricted in which roles may execute them.
          * Some transitions cause the activity to be assigned to the executing Agent.
          *
          * @param stepPath The path in the Workflow to the desired Activity
          *
          * @param transitionID The transition to be performed
          *
          * @param requestData The XML Outcome of the work defined by the Activity. Must be valid to the XML Schema,
          * though this is not verified on the server, rather in the AgentProxy in the Client API.
          *
          * @param attachmentType the MimeType of the attachment (can be empty)
          *
          * @param attachment binary data associated with the Outcome (can be empty)
          *
          * @throws AccessRightsException The Agent is not permitted to perform the operation. Either it does not 
          * have the correct role, or the Activity is reserved by another Agent. Also thrown when the given Agent ID doesn't exist.
          * @throws InvalidTransitionException The Activity is not in the correct state to make the requested transition.
          * @throws ObjectNotFoundException The Activity or a container of it does not exist.
          * @throws InvalidDataException An activity property for the requested Activity was invalid e.g. SchemaVersion was not a number. 
          Also thrown when an uncaught Java exception or error occurred.
          * @throws PersistencyException There was a problem committing the changes to storage.
          * @throws ObjectAlreadyExistsException Not normally thrown, but reserved for PredefinedSteps to throw if they need to.
          **/
  public String requestActionWithScript (org.cristalise.kernel.common.SystemKey agentKey, String stepPath, int transitionID, String requestData, String attachmentType, byte[] attachment) throws org.cristalise.kernel.common.AccessRightsException, org.cristalise.kernel.common.InvalidTransitionException, org.cristalise.kernel.common.ObjectNotFoundException, org.cristalise.kernel.common.InvalidDataException, org.cristalise.kernel.common.PersistencyException, org.cristalise.kernel.common.ObjectAlreadyExistsException, org.cristalise.kernel.common.InvalidCollectionModification
  {
    return _impl.requestActionWithScript(agentKey, stepPath, transitionID, requestData, attachmentType, attachment);
  } // requestActionWithScript


  /**
          * Returns a set of Jobs for this Agent on this Item. Each Job represents a possible transition
          * of a particular Activity in the Item's lifecycle. The list may be filtered to only refer to 
          * currently active activities. 
          *
          * @param agentKey The system key of the Agent requesting Jobs.
          * @param filter If true, then only Activities which are currently active will be included.
          * @return An XML marshalled {@link org.cristalise.kernel.entity.agent.JobArrayList JobArrayList}
          * @throws AccessRightsException - when the Agent doesn't exist
          * @throws ObjectNotFoundException - when the Item doesn't have a lifecycle
          * @throws PersistencyException - when there was a storage or other unknown error
          **/
  public String queryLifeCycle (org.cristalise.kernel.common.SystemKey agentKey, boolean filter) throws org.cristalise.kernel.common.AccessRightsException, org.cristalise.kernel.common.ObjectNotFoundException, org.cristalise.kernel.common.PersistencyException
  {
    return _impl.queryLifeCycle(agentKey, filter);
  } // queryLifeCycle

  private org.cristalise.kernel.entity.AgentOperations _impl;
  private org.omg.PortableServer.POA _poa;

} // class AgentPOATie
