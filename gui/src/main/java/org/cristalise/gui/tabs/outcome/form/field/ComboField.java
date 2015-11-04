/**
 * This file is part of the CRISTAL-iSE default user interface.
 * Copyright (c) 2001-2014 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.gui.tabs.outcome.form.field;

import java.awt.Component;
import java.util.Enumeration;
import java.util.StringTokenizer;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.text.JTextComponent;

import org.cristalise.gui.MainFrame;
import org.cristalise.gui.tabs.outcome.form.StructuralException;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.types.AnyNode;
import org.exolab.castor.xml.schema.Annotation;
import org.exolab.castor.xml.schema.AttributeDecl;
import org.exolab.castor.xml.schema.Documentation;
import org.exolab.castor.xml.schema.ElementDecl;
import org.exolab.castor.xml.schema.Facet;
import org.exolab.castor.xml.schema.SimpleType;


/*******************************************************************************
 *
 * $Revision: 1.4 $ $Date: 2005/08/16 13:59:56 $
 *
 * Copyright (C) 2003 CERN - European Organization for Nuclear Research All
 * rights reserved.
 ******************************************************************************/

public class ComboField extends EditField {

    JComboBox comboField;
    ListOfValues vals;
    DefaultComboBoxModel comboModel;
    AnyNode listNode;

    public ComboField(SimpleType type, AnyNode listNode) {
        super();
        comboField = new JComboBox();
        content = type;
        this.listNode = listNode;
        createLOV();
    }

    @Override
	public String getDefaultValue() {
        if (vals.getDefaultKey() != null)
            return vals.get(vals.getDefaultKey()).toString();
        else
            return "";
    }
    
    public void setDefaultValue(String defaultVal) {
    	vals.setDefaultValue(defaultVal);
    	comboModel.setSelectedItem(vals.getDefaultKey());
    }

    @Override
	public String getText() {
        return vals.get(comboModel.getSelectedItem()).toString();
    }

    @Override
	public JTextComponent makeTextField() {
        // not used by this control
        return null;
    }

    @Override
	public void setText(String text) {
        if (vals.containsValue(text)) {
        	comboModel.setSelectedItem(vals.findKey(text));
        }
        else
        	Logger.error("Illegal value for ComboField "+getName()+": "+text);
    }

    @Override
	public Component getControl() {
        return comboField;
    }

    private void createLOV() {
        vals = new ListOfValues();

        if (listNode != null) { // schema instructions for list building
            String lovType = listNode.getLocalName();
            String param = listNode.getFirstChild().getStringValue();
            if (lovType.equals("ScriptList"))
                populateLOVFromScript(param);
            if (lovType.equals("PathList"))
                populateLOVFromLDAP(param);
        }

        // handle enumerations
        // TODO: should be ANDed with above results
        if (content.hasFacet(Facet.ENUMERATION)) {
            //ListOfValues andList = new ListOfValues();
            Enumeration<Facet> enums = content.getFacets(Facet.ENUMERATION);
            while (enums.hasMoreElements()) {
                Facet thisEnum = enums.nextElement();
                String desc = thisEnum.getValue();
                Enumeration<Annotation> annos = thisEnum.getAnnotations();
                if (annos.hasMoreElements()) {
                	Annotation thisAnno = annos.nextElement();
                	Enumeration<Documentation> docs = thisAnno.getDocumentation();
                	if (docs.hasMoreElements()) 
                		desc = docs.nextElement().getContent();
                }
                vals.put(desc, thisEnum.getValue(), false);
             }
        }

        comboModel = new DefaultComboBoxModel(vals.getKeyArray());
        //if (vals.getDefaultKey() != null) comboModel.setSelectedItem(vals.getDefaultKey());
        comboField.setModel(comboModel);
    }

    /**
     * @param param
     */
    private void populateLOVFromLDAP(String param) {
        // TODO List of Values from LDAP properties, eg '/root/path;prop=val;prop=val'


    }

    private void populateLOVFromScript(String scriptName) {
        try {
            StringTokenizer tok = new StringTokenizer(scriptName, "_");
            if (tok.countTokens() != 2)
                throw new Exception("Invalid LOVScript name");
            Script lovscript = LocalObjectLoader.getScript(tok.nextToken(), Integer.parseInt(tok.nextToken()));
            lovscript.setInputParamValue("LOV", vals);
            lovscript.execute();
        } catch (Exception ex) {
        	MainFrame.exceptionDialog(ex);
        }
    }

    @Override
	public void setDecl(AttributeDecl model) throws StructuralException {
        super.setDecl(model);
        setDefaultValue(model.getDefaultValue());
    }

    @Override
	public void setDecl(ElementDecl model) throws StructuralException {
        super.setDecl(model);
        setDefaultValue(model.getDefaultValue());
    }

    /**
     *
     */

    @Override
	public void setEditable(boolean editable) {
        comboField.setEditable(editable);
    }
}