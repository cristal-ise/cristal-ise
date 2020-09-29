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
package org.cristalise.kernel.lifecycle.instance.predefined;

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.AGENT_ROLE;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCHEMA_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCHEMA_VERSION;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.STATE_MACHINE_NAME;
import static org.cristalise.kernel.security.BuiltInAuthc.ADMIN_ROLE;
import static org.cristalise.kernel.security.BuiltInAuthc.SYSTEM_AGENT;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.InvalidTransitionException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.events.History;
import org.cristalise.kernel.lifecycle.instance.Activity;
import org.cristalise.kernel.lifecycle.instance.predefined.agent.AgentPredefinedStepContainer;
import org.cristalise.kernel.lifecycle.instance.predefined.item.ItemPredefinedStepContainer;
import org.cristalise.kernel.lifecycle.instance.predefined.server.ServerPredefinedStepContainer;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

import lombok.extern.slf4j.Slf4j;

/**
 * PredefinedStep are always Active, and have only one transition. 
 * Subclasses could override this method (if necessary)
 */
@Slf4j
public abstract class PredefinedStep extends Activity {

    private boolean         isPredefined = false;
    public static final int DONE         = 0;
    public static final int AVAILABLE    = 0;

    public PredefinedStep() {
        super();
        setBuiltInProperty(STATE_MACHINE_NAME, "PredefinedStep");
        setBuiltInProperty(SCHEMA_NAME, "PredefinedStepOutcome");
        setBuiltInProperty(SCHEMA_VERSION, "0");

        addAdminAgentRole();
    }

    @Override
    public boolean getActive() {
        if (isPredefined)
            return true;
        else
            return super.getActive();
    }

    @Override
    public String getErrors() {
        if (isPredefined)
            return getName();
        else
            return super.getErrors();
    }

    @Override
    public boolean verify() {
        if (isPredefined)
            return true;
        else
            return super.verify();
    }

    /**
     * Returns the isPredefined.
     *
     * @return boolean
     */
    public boolean getIsPredefined() {
        return isPredefined;
    }

    /**
     * Sets the isPredefined.
     *
     * @param isPredefined
     *            The isPredefined to set
     */
    public void setIsPredefined(boolean isPredefined) {
        this.isPredefined = isPredefined;
    }

    @Override
    public String getType() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    static public String getPredefStepSchemaName(String stepName) {
        PredefinedStepContainer[] allSteps = 
            { new ItemPredefinedStepContainer(), new AgentPredefinedStepContainer(), new ServerPredefinedStepContainer() };

        for (PredefinedStepContainer thisContainer : allSteps) {
            String stepPath = thisContainer.getName() + "/" + stepName;
            Activity step = (Activity) thisContainer.search(stepPath);

            if (step != null) {
                return (String) step.getBuiltInProperty(SCHEMA_NAME);
            }
        }
        return "PredefinedStepOutcome"; // default to standard if not found - server may be a newer version
    }

    /**
     * All predefined steps must override this to implement their action
     */
    @Override
    protected abstract String runActivityLogic(AgentPath agent, ItemPath itemPath, int transitionID, String requestData, Object locker) 
            throws  InvalidDataException,
                    InvalidCollectionModification,
                    ObjectAlreadyExistsException,
                    ObjectCannotBeUpdated,
                    ObjectNotFoundException,
                    PersistencyException,
                    CannotManageException,
                    AccessRightsException;

    /**
     * Generic bundling of parameters
     * 
     * @param data
     * @return
     */
    static public String bundleData(String...data) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document dom = builder.newDocument();
            Element root = dom.createElement("PredefinedStepOutcome");
            dom.appendChild(root);

            for (String element : data) {
                Element param = dom.createElement("param");
                Text t = dom.createTextNode(element);
                param.appendChild(t);
                root.appendChild(param);
            }
            return Outcome.serialize(dom, false);
        }
        catch (Exception ex) {
            log.error("", ex);
            StringBuffer xmlData = new StringBuffer().append("<PredefinedStepOutcome>");

            for (String element : data)
                xmlData.append("<param><![CDATA[").append(element).append("]]></param>");

            xmlData.append("</PredefinedStepOutcome>");
            return xmlData.toString();
        }
    }

    // generic bundling of single parameter
    static public String bundleData(String data) {
        return bundleData(new String[] { data });
    }

    public static String[] getDataList(String xmlData) {
        try {
            Document scriptDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xmlData)));

            NodeList nodeList = scriptDoc.getElementsByTagName("param");
            String[] result = new String[nodeList.getLength()];

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node n = nodeList.item(i).getFirstChild();

                if      (n instanceof CDATASection) result[i] = ((CDATASection) n).getData();
                else if (n instanceof Text)         result[i] = ((Text) n).getData();
            }
            return result;
        }
        catch (Exception ex) {
            log.error("", ex);
        }
        return null;
    }

    protected void addAdminAgentRole() {
        if (Gateway.getProperties().getBoolean("PredefinedStep.AgentRole.enableAdmin", false)) {
            String extraRoles = Gateway.getProperties().getString("PredefinedStep."+ this.getClass().getSimpleName() +".roles");
            getProperties().setBuiltInProperty(AGENT_ROLE, ADMIN_ROLE.getName() + (StringUtils.isNotBlank(extraRoles) ? ","+extraRoles : ""));
        }
    }


    /********************************
     * Methods migrated from Bootstrap
     ********************************/

    /**
     * TODO Implement Bootstrap predefined step
     * 
     * @param itemPath
     * @param newOutcome
     * @throws PersistencyException
     * @throws ObjectNotFoundException
     * @throws InvalidDataException
     */
    public static void storeOutcomeEventAndViews(ItemPath itemPath, Outcome newOutcome, Object transactionKey )
            throws PersistencyException, ObjectNotFoundException, InvalidDataException
    {
        storeOutcomeEventAndViews(itemPath, newOutcome, null, transactionKey);
    }

    /**
     * TODO Implement Bootstrap predefined step
     * 
     * @param itemPath
     * @param newOutcome
     * @param version
     * @throws PersistencyException
     * @throws ObjectNotFoundException
     * @throws InvalidDataException
     */
    public static void storeOutcomeEventAndViews(ItemPath itemPath, Outcome newOutcome, Integer version, Object transactionKey)
            throws PersistencyException, ObjectNotFoundException, InvalidDataException
    {
        String viewName = "";
        if (version != null) viewName = String.valueOf(version);

        log.info("storeOutcomeEventAndViews() - Schema '{}' of version '{}' to '{}'", 
                newOutcome.getSchema().getName(), version != null ? viewName : "last", itemPath);

        History hist = new History(itemPath, null);

        int eventID = hist.addEvent((AgentPath)SYSTEM_AGENT.getPath(), null,
                ADMIN_ROLE.getName(), "Bootstrap", "Bootstrap", "Bootstrap", newOutcome.getSchema(), 
                LocalObjectLoader.getStateMachine("PredefinedStep", 0), PredefinedStep.DONE, version != null ? viewName : "last"
                ).getID();

        newOutcome.setID(eventID);

        Viewpoint newLastView = new Viewpoint(itemPath, newOutcome.getSchema(), "last", eventID);

        Gateway.getStorage().put(itemPath, newOutcome,  transactionKey);
        Gateway.getStorage().put(itemPath, newLastView, transactionKey);

        if (version != null) {
            Viewpoint newNumberView = new Viewpoint(itemPath, newOutcome.getSchema(), viewName, eventID);
            Gateway.getStorage().put(itemPath, newNumberView, transactionKey);
        }
    }

    /**
     * Use this method to run a Predefined step during bootstrap
     * 
     * @param agent
     * @param itemPath
     * @param requestData
     * @param transactioKey
     * @return
     * @throws AccessRightsException
     * @throws InvalidTransitionException
     * @throws InvalidDataException
     * @throws ObjectNotFoundException
     * @throws PersistencyException
     * @throws ObjectAlreadyExistsException
     * @throws ObjectCannotBeUpdated
     * @throws CannotManageException
     * @throws InvalidCollectionModification
     */
    public String request(AgentPath agent, ItemPath itemPath, String requestData, Object transactionKey)
            throws AccessRightsException, 
            InvalidTransitionException, 
            InvalidDataException, 
            ObjectNotFoundException, 
            PersistencyException,
            ObjectAlreadyExistsException, 
            ObjectCannotBeUpdated, 
            CannotManageException, 
            InvalidCollectionModification
    {
        log.info("request({}) - Type:{}", itemPath, getType());
        this.setActive(true);
        return request(agent, agent, itemPath, PredefinedStep.DONE, requestData, null, new byte[0], true, transactionKey);
    }
}
