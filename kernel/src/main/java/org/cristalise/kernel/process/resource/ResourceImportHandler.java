/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.kernel.process.resource;

import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.property.PropertyDescriptionList;


public interface ResourceImportHandler {
    // TODO: Implement REMOVE
    public enum Status {IDENTICAL, NEW, UPDATED, OVERWRITTEN, SKIPPED, REMOVED};

    /**
     * Returns the DomainPath for a specific resource
     * 
     * @param ns - module namespace
     * @param name - resource name
     * @return DomainPath initialised
     */
    public DomainPath getPath(String name, String ns);

    /**
     * Generate the outcome that the resource should contain.
     * 
     * @param name the name of the resource 
     * @param ns the namespace defined in the module
     * @param location the location of the resource file
     * @param version the specified version
     * @return a set of outcomes to be synced with the resource item.
     * @throws InvalidDataException - if the supplied resources are not valid
     * @throws ObjectNotFoundException - data was not found to handle request
     */
    public Outcome getResourceOutcome(String name, String ns, String location, Integer version) 
            throws InvalidDataException, ObjectNotFoundException;

    /** 
     * Gives the CompActDef name to instantiate to provide the workflow for this type of resource. 
     * Should be found in the CA typeroot (/desc/ActivityDesc/)
     * 
     * @return String workflow name
     */
    public String getWorkflowName(); 

    /**
     * Should return all of the Properties the resource Item will have on creation. 
     * The Property 'Name' will be created and populated automatically, even if not declared.
     * 
     * @return a PropertyDescriptionList - an arraylist of PropertyDescriptions
     */
    public PropertyDescriptionList getPropDesc();

    /**
     * The directory context to search for existing resources. The name of the resource must
     * be unique below this point.
     * 
     * @return Root DomainPath
     */
    public DomainPath getTypeRoot();


    /**
     * Returns any collections that this Resource Item should contain.
     * 
     * @param name the name of the Resource Item
     * @param ns the namaspace of the module declaring the Resource Item
     * @param location the location of the XML file on the boot directory
     * @param version the version of the Resource Item
     * @return CollectionArrayList
     * @throws Exception something went wrong
     */
    public CollectionArrayList getCollections(String name, String ns, String location, Integer version, TransactionKey transactionKey) throws Exception;

    /**
     * Returns any collections that this Resource Item stored in the outcome should contain.
     * 
     * @param name the name of the Resource Item
     * @param version the version of the Resource Item
     * @param outcome Outcome containing the XML representing the Resource Item
     * @return CollectionArrayList
     * @throws Exception something went wrong
     */
    public CollectionArrayList getCollections(String name, Integer version, Outcome outcome, TransactionKey transactionKey) throws Exception;

    /**
     * The name of the imported resource managed by the handler
     * 
     * @return The name of the imported resource managed by the handler
     */
    public String getName();

    /**
     * Create resource Item from its Module definition. The item should not exist.
     * 
     * @param ns the NameSpace of the Module
     * @param itemName name of the resource Item
     * @param version version of the resource Item
     * @param outcome the Outcome to be used to create the resource
     * @param reset whether the resources shall be updated even if the current version was modified 
     *        by someone other than the bootstrapper. Use system property 'Module.reset' or 'Module.${namespace}.reset' 
     *        to control if bootstrap should overwrite the resource
     * @return the DomainPath of the resource Item
     * @throws Exception errors that are raised during the process
     */
    public DomainPath createResource(String ns, String itemName, int version, Outcome outcome, boolean reset, TransactionKey transactionKey)
            throws Exception;

    /**
     * Creates or updates resource Item against a Module version, using a ResourceImportHandler configured
     * finding the outcome at the given dataLocation.
     * 
     * @param ns the NameSpace of the Module
     * @param itemName name of the resource Item
     * @param version version of the resource Item
     * @param itemPath the fixed UUID of the Item
     * @param dataLocation the location of the resource xml available from the classpath to be used as Outcome
     * @param reset whether the resources shall be updated even if the current version was modified 
     *        by someone other than the bootstrapper. Use system property 'Module.reset' or 'Module.${namespace}.reset' 
     *        to control if bootstrap should overwrite the resource
     * @return the DomainPath of the resource Item
     * @throws Exception errors that are raised during the process
     */
    public DomainPath importResource(String ns, String itemName, int version, ItemPath itemPath, String dataLocation, boolean reset, TransactionKey transactionKey)
            throws Exception;

    /**
     * Verify a resource Item against a Module version, but supplies the resource outcomes directly
     * instead of through a location lookup.
     * 
     * @param ns the NameSpace of the Module
     * @param itemName name of the resource Item
     * @param version version of the resource Item
     * @param itemPath the fixed UUID of the Item
     * @param the Outcome to be used to import the 
     * @param reset whether the resources shall be updated even if the current version was modified 
     *        by someone other than the bootstrapper. Use system property 'Module.reset' or 'Module.${namespace}.reset' 
     *        to control if bootstrap should overwrite the resource
     * @return the DomainPath of the resource Item
     * @throws Exception errors that are raised during the process
     */
    public DomainPath importResource(String ns, String itemName, int version, ItemPath itemPath, Outcome outcome, boolean reset, TransactionKey transactionKey)
            throws Exception;

    /**
     * Reads the XML string that was created during the createResource() and importResource() methods
     * @return xml string
     */
    public String getResourceChangeDetails();

    public Status getResourceChangeStatus();
}
