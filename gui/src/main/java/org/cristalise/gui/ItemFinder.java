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
package org.cristalise.gui;

import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.Lookup;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.process.Gateway;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ItemFinder extends Box implements Runnable {
    JTextField inputField;
    JButton findButton;
    JButton findNextButton;
    GridBagLayout gridbag = new GridBagLayout();
    Lookup lookup = Gateway.getLookup();
    DomainKeyConsumer defaultConsumer = null;
    DomainKeyConsumer currentConsumer = null;
    Iterator<?> matches;
    Path rootNode = new DomainPath();

    static protected ImageIcon mFindIcon = null;
    static protected ImageIcon mNextIcon = null;
    static {
        try {
            mNextIcon = ImageLoader.findImage("next.png");
            mFindIcon = ImageLoader.findImage("find.png");
        } catch (Exception e) {
            log.error("Couldn't load images", e);
        }
    }

    public ItemFinder() {
        super(BoxLayout.X_AXIS);
        initPanel();
    }

    public void pushNewKey(String key) {
        inputField.setText(key);
        runSearch();
    }

    public void setDefaultConsumer(DomainKeyConsumer newConsumer) {
        defaultConsumer = newConsumer;
        currentConsumer = newConsumer;
    }

    public DomainKeyConsumer getDefaultConsumer() {
        return defaultConsumer;
    }

    public void setConsumer(DomainKeyConsumer newConsumer, String label) {
        currentConsumer = newConsumer;
        findButton.setText(label);
    }

    public void clearConsumer(DomainKeyConsumer oldConsumer) {
        if (currentConsumer == oldConsumer) {
            currentConsumer = defaultConsumer;
            findButton.setText("");
        }
    }

    private void initPanel() {

        JLabel search = new JLabel("Search:");
        add(search);
        add(Box.createHorizontalStrut(7));

        inputField = new JTextField(20);
        add(inputField);
        add(Box.createHorizontalStrut(5));
        inputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pushNewKey(inputField.getText());
            }
        });

        findButton = new JButton(mFindIcon);// ("Find"));
        findButton.setMargin(new Insets(2, 5, 2, 5));
        findButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pushNewKey(inputField.getText());
            }
        });
        add(findButton);
        add(Box.createHorizontalStrut(5));

        findNextButton = new JButton(mNextIcon);
        findNextButton.setMargin(new Insets(2, 5, 2, 5));
        findNextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                nextMatch();
            }
        });
        findNextButton.setEnabled(false);
        add(findNextButton);
        add(Box.createHorizontalStrut(15));

        // create plugins
        log.debug("creating plugins");
        String requiredListeners = Gateway.getProperties().getString("DomainKeyListeners");
        if (requiredListeners != null) {
            StringTokenizer tok = new StringTokenizer(requiredListeners, ",");
            while (tok.hasMoreTokens()) {
                String listenerName = tok.nextToken();
                log.debug("creating a " + listenerName);
                try {
                    Class<?> listenerClass = Class.forName(listenerName);
                    DomainKeyListener newListener = (DomainKeyListener) listenerClass.newInstance();
                    newListener.init();
                    newListener.setConsumer(this);
                    JToggleButton listenerButton = new JToggleButton(newListener.getIcon(), false);
                    listenerButton.addItemListener(
                            new ListenerButtonListener(newListener, listenerButton));
                    listenerButton.setMargin(new Insets(0, 2, 0, 2));
                    listenerButton.setToolTipText("Enable " + newListener.getDescription());
                    add(listenerButton);
                    add(Box.createHorizontalStrut(7));
                } catch (Exception e) {
                    log.error("ItemFinder() - could not create a " + listenerName + ": ", e);
                }
            }
            add(Box.createHorizontalGlue());
        }
    }

    private void runSearch() {
        Thread searcher = new Thread(this);
        searcher.start();
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Entity Search");
        String searchTerm = inputField.getText();
        if (searchTerm.length() == 0)
            return; // don't allow null searches

        findButton.setEnabled(false);
        findNextButton.setEnabled(false);
        MainFrame.progress.startBouncing("Searching. Please Wait");
        findNextButton.setEnabled(false);
        String term = inputField.getText();
        // The following block does property searching when the field contains a colon, but that
        // returns EntityPaths, which the tree can't handle
        // int colonPos = term.indexOf(':');
        // if (colonPos > 0)
        // matches = lookup.search(rootNode,term.substring(0, colonPos),
        // term.substring(colonPos+1));
        // else
        matches = lookup.search(rootNode, term);
        if (!matches.hasNext()) {
            MainFrame.progress.stopBouncing("No results");
            currentConsumer.push(searchTerm); // for subscribers who don't care if it exists
            findButton.setEnabled(true);
            return;
        }
        MainFrame.progress.stopBouncing("Selecting first match.");
        nextMatch();

    }

    void nextMatch() {
        findButton.setEnabled(false);
        findNextButton.setEnabled(false);
        DomainPath nextMatch = (DomainPath) matches.next();
        try {
            currentConsumer.push(nextMatch);
        } catch (NullPointerException e) {
            // case the item searched is not found !
        }
        findButton.setEnabled(true);
        findNextButton.setToolTipText("Click to show next match");
        if (matches.hasNext())
            findNextButton.setEnabled(true);
    }

    private class ListenerButtonListener implements ItemListener {
        private final DomainKeyListener listener;
        private final JToggleButton listenerButton;

        public ListenerButtonListener(DomainKeyListener newListener, JToggleButton listenerButton) {
            this.listener = newListener;
            this.listenerButton = listenerButton;
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                // Switch on
                try {
                    if (!(listener.enable()))
                        listenerButton.doClick(); // allow plugins to disable themselves
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, ex.getMessage(),
                            "Error initialising " + listener.getDescription(),
                            JOptionPane.ERROR_MESSAGE);
                    listenerButton.doClick();
                }
                listenerButton.setToolTipText("Disable " + listener.getDescription());
            } else {
                // Switch off
                listener.disable();
                listenerButton.setToolTipText("Enable " + listener.getDescription());
            }
        }
    }

}
