/**
 * This file is part of the CRISTAL-iSE default user interface.
 * Copyright (c) 2001-2016 The CRISTAL Consortium. All rights reserved.
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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Enumeration;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import org.cristalise.gui.DomainKeyConsumer;
import org.cristalise.gui.MainFrame;
import org.cristalise.gui.tabs.outcome.OutcomeException;
import org.cristalise.gui.tabs.outcome.form.OutcomeStructure;
import org.cristalise.gui.tabs.outcome.form.StructuralException;
import org.cristalise.kernel.lookup.DomainPath;
import org.exolab.castor.types.AnyNode;
import org.exolab.castor.xml.schema.Annotation;
import org.exolab.castor.xml.schema.AppInfo;
import org.exolab.castor.xml.schema.AttributeDecl;
import org.exolab.castor.xml.schema.ElementDecl;
import org.exolab.castor.xml.schema.Facet;
import org.exolab.castor.xml.schema.SimpleType;
import org.exolab.castor.xml.schema.Structure;
import org.exolab.castor.xml.schema.XMLType;
import org.exolab.castor.xml.schema.simpletypes.ListType;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.w3c.dom.Text;


/** Superclass for the entry field for Field and AttributeList.
  */
public class EditField implements FocusListener, DomainKeyConsumer {

    Node data;
    Structure model;
    protected SimpleType content;
    protected JTextComponent field;

    boolean isValid = true;
    boolean editable = true;
    String name;


    public EditField() {
        field = makeTextField();
        if (field != null)
            field.addFocusListener(this);
    }

    private static EditField getFieldForType(SimpleType type) {
        // handle lists special
        if (type instanceof ListType)
            return new ArrayEditField(type.getBuiltInBaseType());

        // is a combobox
        if (type.hasFacet(Facet.ENUMERATION))
            return new ComboField(type, null);
        //find LOVscript TODO: Implement LOV
        Enumeration<Annotation> e = type.getAnnotations();
        while (e.hasMoreElements()) {
            Annotation note = e.nextElement();
            for (Enumeration<AppInfo> f = note.getAppInfo(); f.hasMoreElements();) {
                AppInfo thisAppInfo = f.nextElement();
                for (Enumeration<?> g = thisAppInfo.getObjects(); g.hasMoreElements();) {
                    AnyNode appInfoNode = (AnyNode)g.nextElement();
                    if (appInfoNode.getLocalName().equals("ScriptList")
                            || appInfoNode.getLocalName().equals("LDAPList")) {
                        return new ComboField(type, appInfoNode);
                    }
                }
            }
        }
        // find info on length before we go to the base type
        long length = -1;
        if (type.getLength()!=null) length = type.getLength().longValue();
        else if (type.getMaxLength()!=null) length = type.getMaxLength().longValue();
        else if (type.getMinLength()!=null) length = type.getMinLength().longValue();

        // find base type if derived
        if (!(type.isBuiltInType()))
            type = type.getBuiltInBaseType();
        // else derive the class
        Class<?> contentClass = OutcomeStructure.getJavaClass(type.getTypeCode());
        // disable list edits for the moment
        if (contentClass.equals(Boolean.class))
            return new BooleanEditField();
        else if (contentClass.equals(BigInteger.class))
            return new IntegerEditField();
        else if (contentClass.equals(BigDecimal.class))
            return new DecimalEditField();
        else if (contentClass.equals(ImageIcon.class))
            return new ImageEditField();
        else if (length > 60)
            return new LongStringEditField();
        else return new EditField();
    }

    public static EditField getEditField(AttributeDecl model) throws StructuralException {
    	if (model.isReference()) model = model.getReference();
        EditField newField = getFieldForType(model.getSimpleType());
        newField.setDecl(model);
        return newField;
    }

    public static EditField getEditField(ElementDecl model, HashMap<String, Class<?>> specialControls) throws StructuralException {
        try {
            XMLType baseType = model.getType();
            while (!(baseType instanceof SimpleType))
                baseType = baseType.getBaseType();
            EditField newField;
            if (specialControls.containsKey(model.getName()))
            	newField = (EditField)specialControls.get(model.getName()).newInstance();
            else
                newField = getFieldForType((SimpleType)baseType);
            newField.setDecl(model);
            return newField;
        } catch (Exception ex) {
            throw new StructuralException("No type defined in model");
        }
    }

    public void setDecl(AttributeDecl model) throws StructuralException {
        this.model=model;
        this.content=model.getSimpleType();
        this.name = model.getName();
        if (model.isFixed()) setEditable(false);
    }

    public void setDecl(ElementDecl model) throws StructuralException {
        this.model=model;
        this.name = model.getName();
        XMLType type = model.getType();

        // derive base type
        if (type.isSimpleType())
            this.content = (SimpleType)type;
        else
            this.content = (SimpleType)(type.getBaseType());

        if (this.content == null) throw new StructuralException("No declared base type of element");

        //
        if (model.getFixedValue() != null) setEditable(false);

    }

    public void setData(Attr newData) throws StructuralException {
        if (!(newData.getName().equals(name)))
            throw new StructuralException("Tried to add a "+newData.getName()+" into a "+name+" attribute.");

        this.data = newData;
        setText(newData.getValue());
    }

    public void setData(Text newData) {
        String contents = newData.getData();
        this.data = newData;
        setText(contents);
    }

    public void setData(String newData) throws OutcomeException {
        if (data == null) throw new OutcomeException("No node exists");
        setText(newData);
        updateNode();

    }

    public Structure getModel() {
        return model;
    }

    public String getName() {
        return name;
    }

    public Node getData() {
        return data;
    }

    public String getDefaultValue() {
        return "";
    }

    @Override
	public void focusLost(FocusEvent e) {
        if (MainFrame.itemFinder != null)
            MainFrame.itemFinder.clearConsumer(this);
        updateNode();
    }

    @Override
	public void focusGained(FocusEvent e) {
    	if (editable && MainFrame.itemFinder != null)
      		MainFrame.itemFinder.setConsumer(this, "Insert");
    }

    public void updateNode() {
    	if (data == null) return;
        if (data instanceof Text) {
            ((Text)data).setData(getText());
        }
        else { //attribute
            ((Attr)data).setValue(getText());
        }
    }

    /**
     * Read domkey from barcode input
     */
    @Override
	public void push(DomainPath key) {
        setText(key.getName());
    }

    /**
     * Read string from barcode input
     */
    @Override
	public void push(String key) {
        setText(key);
    }

	public void setEditable(boolean editable) {
		this.editable = editable;
        if (field != null)
            field.setEditable(editable);
	}

    public String getText() {
        return field.getText();
    }

    public void setText(String text) {
        field.setText(text);
    }

    public JTextComponent makeTextField() {
        return new JTextField();
    }

    public Component getControl() {
        return field;
    }

    public void grabFocus() {
        getControl().requestFocus();
    }
}
