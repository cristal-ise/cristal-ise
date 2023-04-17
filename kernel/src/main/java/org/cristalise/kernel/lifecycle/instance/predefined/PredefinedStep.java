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

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.DESCRIPTION;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCHEMA_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCHEMA_VERSION;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.STATE_MACHINE_NAME;
import static org.cristalise.kernel.security.BuiltInAuthc.ADMIN_ROLE;
import static org.cristalise.kernel.security.BuiltInAuthc.SYSTEM_AGENT;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import org.cristalise.kernel.graph.model.BuiltInVertexProperties;
import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.lifecycle.instance.Activity;
import org.cristalise.kernel.lifecycle.instance.predefined.agent.AgentPredefinedStepContainer;
import org.cristalise.kernel.lifecycle.instance.predefined.server.ServerPredefinedStepContainer;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.KeyValuePair;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * PredefinedStep are always Active, and have only one transition. 
 * Subclasses could override this method (if necessary)
 */
@Slf4j
public abstract class PredefinedStep extends Activity {

    public static final int DONE         = 0;
    public static final int AVAILABLE    = 0;

    /**
     * Order is important
     */
    @Getter
    private Map<ItemPath, String> autoUpdates = new  LinkedHashMap<ItemPath, String>();

    public PredefinedStep(String schemaName, String description) {
        super();
        setBuiltInProperty(STATE_MACHINE_NAME, StateMachine.getDefaultStateMachine("Predefined"));

        if (StringUtils.isBlank(schemaName)) schemaName = "PredefinedStepOutcome"; 

        try {
            LocalObjectLoader.getSchema(schemaName, 0); // checks if the Schema is available
            setBuiltInProperty(SCHEMA_NAME, schemaName);
            setBuiltInProperty(SCHEMA_VERSION, "0");
        }
        catch (ObjectNotFoundException | InvalidDataException e) {
            throw new TypeNotPresentException("Cannot find Schema:"+schemaName, e);
        }

        if (StringUtils.isNotBlank(description)) setBuiltInProperty(DESCRIPTION, description);

        setName(this.getClass().getSimpleName());
        setType(this.getClass().getSimpleName());

        setCentrePoint(new GraphPoint());
    }

    public PredefinedStep(String description) {
        this(null, description);
    }

    public PredefinedStep() {
        this(null, null);
    }

    @Override
    public boolean getActive() {
        return true;
    }

    @Override
    public String getErrors() {
        return getName();
    }

    @Override
    public boolean verify() {
        return true;
    }

    @Override
    public String getType() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    public static String getPredefStepSchemaName(String stepName) {
        Activity step = getStepInstance(stepName);
        if (step != null) {
            return (String) step.getBuiltInProperty(SCHEMA_NAME);
        }
        return "PredefinedStepOutcome"; // default to standard if not found - server may be a newer version
    }

    public static PredefinedStep getStepInstance(String stepName) {
        PredefinedStepContainer[] allSteps =
                { new ItemPredefinedStepContainer(), new AgentPredefinedStepContainer(), new ServerPredefinedStepContainer() };

        for (PredefinedStepContainer thisContainer : allSteps) {
            String stepPath = thisContainer.getName() + "/" + stepName;
            PredefinedStep step = (PredefinedStep) thisContainer.search(stepPath);

            if (step != null) return step;
        }
        return null;
    }

    public Node getPredefStepOutcomeNode(Node predefStepNode) throws InvalidDataException {
        final List<Node> found = new ArrayList<>();

        if (log.isDebugEnabled()) log.debug("getPredefStepOutcomeNode() - node:{}", Outcome.serialize(predefStepNode, false));

        Outcome.traverseChildElements(predefStepNode, (outcomeNode) -> {
            String schemaName = (String) getBuiltInProperty(SCHEMA_NAME);
            if (outcomeNode.getNodeName().equals(schemaName)) {
                found.add(outcomeNode);
            }
        });

        if (found.size() == 0) {
            return null;
        }
        else if (found.size() > 1) {
            throw new InvalidDataException("Umbiguious input data found in outcome:"+Outcome.serialize(predefStepNode, false));
        }
        else {
            return found.get(0);
        }
    }

    /**
     * All predefined steps must override this to implement their action
     */
    @Override
    protected abstract String runActivityLogic(AgentPath agent, ItemPath itemPath, int transitionID, String requestData, TransactionKey transactionKey) 
            throws  InvalidDataException,
                    InvalidCollectionModification,
                    ObjectAlreadyExistsException,
                    ObjectCannotBeUpdated,
                    ObjectNotFoundException,
                    PersistencyException,
                    CannotManageException,
                    AccessRightsException;

    /**
     * Generic bundling of parameters. Converts the array of strings to PredefinedStepOutcome XML.
     * Uses CDATA so any of the string could also be an XML.
     * 
     * @param data array of input string for a PredefinedStep
     * @return the result of the PredefienedStep execution
     */
    public static String bundleData(String...data) {
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

    /**
     * Generic bundling of a single parameter. Converts the array of strings to PredefinedStepOutcome XML.
     * Uses CDATA so the string could also be an XML.
     * 
     * @param input string for a PredefinedStep
     * @return the result of the PredefienedStep execution
     */
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
    public static void storeOutcomeEventAndViews(ItemPath itemPath, Outcome newOutcome, TransactionKey transactionKey )
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
    public static void storeOutcomeEventAndViews(ItemPath itemPath, Outcome newOutcome, Integer version, TransactionKey transactionKey)
            throws PersistencyException, ObjectNotFoundException, InvalidDataException
    {
        String viewName = version != null ? String.valueOf(version) : "last";

        log.info("storeOutcomeEventAndViews() - item:{} version:{} schema:{}", 
                itemPath.getItemName(transactionKey), viewName, newOutcome.getSchema().getName());

        History hist = new History(itemPath, transactionKey);

        int eventID = hist.addEvent((AgentPath)SYSTEM_AGENT.getPath(transactionKey),
                ADMIN_ROLE.getName(), "Bootstrap", "Bootstrap", "Bootstrap", newOutcome.getSchema(), 
                LocalObjectLoader.getStateMachine("PredefinedStep", 0, transactionKey), PredefinedStep.DONE, viewName
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
     * Use this method to run a Predefined steps during Bootstrap or during Activity.request()
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
    public String request(AgentPath agent, ItemPath itemPath, String requestData, TransactionKey transactionKey)
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
        log.info("request({}) - class:{}", itemPath.getItemName(transactionKey), getType());
        this.setActive(true);
        return request(agent, itemPath, PredefinedStep.DONE, requestData, null, new byte[0], true, transactionKey);
    }

    /**
     * 
     * @param currentItem
     * @param currentActivity
     * @param inputOutcome
     * @param transactionKey
     */
    public void computeUpdates(ItemPath currentItem, Activity currentActivity, Node outcomeNode, TransactionKey transactionKey)
            throws InvalidDataException, PersistencyException, ObjectNotFoundException, ObjectAlreadyExistsException, InvalidCollectionModification
    {
        getAutoUpdates().put(currentItem, Outcome.serialize(outcomeNode, false));
    };

    public void mergeProperties(CastorHashMap newProps) {
        for (KeyValuePair kvPair : newProps.getKeyValuePairs()) {
            BuiltInVertexProperties key = BuiltInVertexProperties.getValue((String)kvPair.getKey());

            // only check built-in properties
            if (key == null) continue;

            switch (key) {
                case NAME:
                case VERSION:
                case STATE_MACHINE_NAME:
                case STATE_MACHINE_VERSION:
                case SCHEMA_NAME:
                case SCHEMA_VERSION:
                    // do not overwrite existing values for these
                    break;

                default:
                    getProperties().setKeyValuePair(kvPair);
                    break;
            }
        }
    }
}
