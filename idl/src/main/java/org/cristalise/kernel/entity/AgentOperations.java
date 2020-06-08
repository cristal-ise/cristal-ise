package org.cristalise.kernel.entity;


/**
* org/cristalise/kernel/entity/AgentOperations.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from /home/vagrant/workspace/cristal-ise/idl/src/main/idl/Entity.idl
* Friday, April 10, 2020 7:30:08 PM CEST
*/


/**************************************************************************
    * Agent is a ManageableEntity that represents an Activity executor in the 
    * system. It holds a job list, which are persistent requests for execution 
    * from waiting activities assigned to a role that has such Job pushing enabled.
    **************************************************************************/
public interface AgentOperations  extends org.cristalise.kernel.entity.ItemOperations
{

  /** Supplies the new set of jobs for the given item and activity. The Agent should replace all existing jobs for that activity 
            * with the given set. This method should generally only be called by a workflow while performing an execution.
            *
            * @param itemKey the item which generated the jobs. The Agent should discard any existing jobs for that Item.
            * @param stepPath the Activity within the lifecycle of the item which the jobs relate to
            * @param newJobs an XML marshalled {@link org.cristalise.kernel.entity.agent.JobArrayList JobArrayList} containing the new Jobs
            **/
  void refreshJobList (org.cristalise.kernel.common.SystemKey itemKey, String stepPath, String newJobs);

  /** Add this Agent to the given role
  		  * @param roleName the new role to add
  		  * @throws ObjectNotFoundException when the role doesn't exist
  		  * @throws CannotManageException when an error occurs writing the data to LDAP
  		  **/
  void addRole (String roleName) throws org.cristalise.kernel.common.ObjectNotFoundException, org.cristalise.kernel.common.CannotManageException;

  /** Remove this Agent from the given role
  		  * @param the role name to remove
  		  * @throws CannotManageException when an error occurs writing the data to LDAP
  		  **/
  void removeRole (String roleName) throws org.cristalise.kernel.common.ObjectNotFoundException, org.cristalise.kernel.common.CannotManageException;
} // interface AgentOperations
