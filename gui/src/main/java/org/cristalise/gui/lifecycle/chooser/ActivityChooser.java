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
package org.cristalise.gui.lifecycle.chooser;

import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.cristalise.gui.ImageLoader;
import org.cristalise.gui.MainFrame;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ActivityChooser extends JFrame {
    private LDAPFileChooser mLDAPFileChooserActivity = null;

    private JButton mButtonOK = null;

    private JButton mButtonCancel = null;

    private JPanel mJPanelVertical = null;

    private JPanel mJPanelHorizontal = null;

    private WorkflowDialogue mParent = null;

    private JLabel label = null;

    HashMap<String, Object> mhashmap = null;

    public ActivityChooser(String type, Image img, WorkflowDialogue parent,
            HashMap<String, Object> hashmap) {
        super("Select " + type + " activity definition");
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        img = ImageLoader.findImage("graph/newvertex_large.png").getImage();
        setIconImage(img);
        mParent = parent;
        mhashmap = hashmap;
        initialize(type);
    }

    private JButton getJButtonOK() {
        if (mButtonOK == null)
            mButtonOK = new JButton("OK");
        return mButtonOK;
    }

    private JButton getJButtonCancel() {
        if (mButtonCancel == null)
            mButtonCancel = new JButton("Cancel");
        return mButtonCancel;
    }

    private LDAPFileChooser getLDAPFileChooserActivity(String type) {
        if (mLDAPFileChooserActivity == null) {
            try {
                mLDAPFileChooserActivity = new LDAPFileChooser(type);
                mLDAPFileChooserActivity.setName("LDAPFileChooserRouting");
            } catch (Exception mExc) {
                log.error("",mExc);
            }
        }
        return mLDAPFileChooserActivity;
    }

    private void initialize(String type) {
        getJButtonOK().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                log.debug("mLDAPFileChooserActivity.getEntryName()"
                        + mLDAPFileChooserActivity.getEntryName());
                setCursor(new Cursor(Cursor.WAIT_CURSOR));
                try {
                    mParent.loadThisWorkflow(mLDAPFileChooserActivity.getEntryName(),
                            mLDAPFileChooserActivity.getEntryVersion(), mhashmap);
                } catch (Exception ex) {
                    log.error("",ex);
                    MainFrame.exceptionDialog(ex);
                }
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                close();
            }
        });
        getJButtonCancel().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        // getContentPane().add(getJPanelVertical());
        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.add(getJPanelVertical(type));
        contentPane.add(getJPanelHorizontal());
        contentPane.add(Box.createGlue());
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((d.width - getWidth()) / 2, (d.height - getHeight()) / 2);
        setResizable(true);
        pack();
        setSize(new Dimension(getWidth(), getJButtonCancel().getHeight()
                + getLDAPFileChooserActivity(type).getHeight() + label.getHeight() + 100));
        setVisible(true);
    }

    private void close() {
        mParent = null;
        this.setEnabled(false);
        this.setVisible(false);
    }

    private JPanel getJPanelVertical(String type) {
        if (mJPanelVertical == null) {
            try {
                mJPanelVertical = new JPanel();
                mJPanelVertical.setName("JPanelV");
                mJPanelVertical.setLayout(new BoxLayout(mJPanelVertical, BoxLayout.Y_AXIS));
                label = new JLabel("Select item, then numeric version");
                JPanel labelP = new JPanel();
                labelP.setLayout(new BoxLayout(labelP, BoxLayout.X_AXIS));
                labelP.add(label);
                labelP.add(Box.createGlue());
                mJPanelVertical.add(labelP);
                mJPanelVertical.add(Box.createRigidArea(new Dimension(0, 5)));
                mJPanelVertical.add(getLDAPFileChooserActivity(type));
                // mJPanelVertical.add(Box.createRigidArea(new Dimension(0,
                // 10)));
                mJPanelVertical.add(Box.createGlue());
                mJPanelVertical.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                mJPanelVertical.setVisible(true);
            } catch (java.lang.Throwable mExc) {
                // handleException(mExc);
            }
        }
        return mJPanelVertical;
    }

    private JPanel getJPanelHorizontal() {
        if (mJPanelHorizontal == null) {
            try {
                mJPanelHorizontal = new JPanel();
                mJPanelHorizontal.setName("JPanelH");
                mJPanelHorizontal.setLayout(new BoxLayout(mJPanelHorizontal, BoxLayout.X_AXIS));
                mJPanelHorizontal.add(getJButtonOK(), getJButtonOK().getName());
                mJPanelHorizontal.add(Box.createRigidArea(new Dimension(10, 0)));
                mJPanelHorizontal.add(getJButtonCancel(), getJButtonCancel().getName());
                mJPanelHorizontal.setVisible(true);
            } catch (java.lang.Throwable mExc) {
                // handleException(mExc);
            }
        }
        return mJPanelHorizontal;
    }
}
