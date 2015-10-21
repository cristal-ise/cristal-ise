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
package org.cristalise.gui;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.Logger;

/**
 * @version $Revision: 1.47 $ $Date: 2006/03/03 13:52:21 $
 * @author $Author: abranson $
 */
public class MenuBuilder extends JMenuBar implements ActionListener, ItemListener, HyperlinkListener
{

	protected UIManager.LookAndFeelInfo[] availableViews = UIManager.getInstalledLookAndFeels();
	protected MainFrame myParentFrame;
	protected JMenu fileMenu;
	protected JMenu consoleMenu;
	protected JMenu styleMenu;
	protected JMenu prefMenu;
	protected JMenu helpMenu;
	protected HashMap<String, JMenu> availableMenus = new HashMap<String, JMenu>();

    public MenuBuilder()
    {}

    /** Creates new DynamicMenuBuilder */
    public MenuBuilder(MainFrame parentFrame)
	{
		myParentFrame = parentFrame;
		fileMenu = new JMenu("File");
		consoleMenu = new JMenu("Console");
		styleMenu = new JMenu("Style");
		prefMenu = new JMenu("Preferences");
		helpMenu = new JMenu("Help");
		availableMenus.put("file", fileMenu);
		availableMenus.put("console", consoleMenu);
		availableMenus.put("preferences", prefMenu);
		availableMenus.put("style", styleMenu);
		availableMenus.put("help", helpMenu);

        addMenuItem("Close All", "file", null, 0);
        addMenuItem("Close Others", "file", null, 0);
        fileMenu.insertSeparator(2);
		addMenuItem("Exit", "file", null, 0);

        addMenuItem("Local console", "console", null, 0);
        consoleMenu.insertSeparator(5);
        addServerConsoles();

		ButtonGroup styleButtonGroup = new ButtonGroup();
		for (LookAndFeelInfo availableView : availableViews)
			addMenuItem(availableView.getName(), "style", styleButtonGroup, 0);

		addMenuItem("Tree Browser", "preferences", null, MainFrame.getPref("ShowTree", "true").equals("true")?2:1);
		addMenuItem("Graph Properties", "preferences", null, MainFrame.getPref("ShowProps", "true").equals("true")?2:1);
		addMenuItem("About", "help", null, 0);

		add(fileMenu);
		add(consoleMenu);
		add(styleMenu);
		add(prefMenu);
		add(helpMenu);
	}
	/**
     *
     */
    private void addServerConsoles() {
        Iterator<?> servers = Gateway.getLookup().search(new DomainPath("/servers"), new Property("Type", "Server", false));
        while(servers.hasNext()) {
            Path thisServerPath = (Path)servers.next();
            try {
                ItemPath serverItemPath = thisServerPath.getItemPath();
                String serverName = ((Property)Gateway.getStorage().get(serverItemPath, ClusterStorage.PROPERTY+"/Name", null)).getValue();
                String portStr = ((Property)Gateway.getStorage().get(serverItemPath, ClusterStorage.PROPERTY+"/ConsolePort", null)).getValue();
                addMenuItem(serverName+":"+portStr, "console", null, 0);
            } catch (Exception ex) {
                Logger.error("Exception retrieving proxy server connection data for "+thisServerPath);
                Logger.error(ex);
            }
        }

    }

    /**
	 * Adds a menu item to a menu. Adds an action listener to the menu item.
	 */
	public void addMenuItem(String itemName, String menuName, ButtonGroup bg, int checkBox)
	{
		//checks to see if the menu to add the item to exists
		if (availableMenus.containsKey(menuName))
		{
			JMenuItem myItem = new JMenuItem(itemName);
			if (bg != null)
			{
				//if the menu item equals the current style, set it selected
				myItem = new JRadioButtonMenuItem(itemName, UIManager.getLookAndFeel().getName().equals(itemName));
				bg.add(myItem);
			}
			if (checkBox != 0)
			{
				myItem = new JCheckBoxMenuItem(itemName, checkBox == 2);
			}
			myItem.addActionListener(this);
			JMenu myMenu = availableMenus.get(menuName);
			myMenu.add(myItem);
		}
	}
	//checks to see if the event dispatched is one of the
	//styles that belong to the UIManager
	public int isStyleChange(String style)
	{
		for (int i = 0; i < availableViews.length; i++)
		{
			if (style.equals(availableViews[i].getName()))
				return i;
		}
		return -1;
	}
	//listens for events performed on the menu items
	@Override
	public void actionPerformed(java.awt.event.ActionEvent e)
	{
		String s = e.getActionCommand();
		int i = isStyleChange(s);
		if (s.equals("Close All") || s.equals("Close Others")) {
			MainFrame.myDesktopManager.closeAll(s.equals("Close Others"));
		}
		else if (s.equals("Exit"))
			myParentFrame.exitForm();
		else if (s.equals("About"))
			showAboutWindow();
		else if (i >= 0)
		{
			try
			{
				UIManager.setLookAndFeel(availableViews[i].getClassName());
				SwingUtilities.updateComponentTreeUI(myParentFrame);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
		else if (s.equals("Tree Browser"))
		{
			myParentFrame.toggleTree();
		}
        else if (s.indexOf(":")>0) { // server console
            try
            {
                String[] serverDetails = s.split(":");
                new Console(serverDetails[0], Integer.parseInt(serverDetails[1])).setVisible(true);
            }
            catch (Exception ex)
            {
                Logger.error(ex);
            }
        }
        else if (s.equals("Local console")) {
            try
            {
                new Console("localhost", Logger.getConsolePort()).setVisible(true);
            }
            catch (Exception ex)
            {
                Logger.error(ex);
            }
        }
        else if (s.equals("Graph Properties")) {
        	MainFrame.setPref("ShowProps", String.valueOf(!MainFrame.getPref("ShowProps", "true").equals("true")));
        }
		else
			Logger.msg(1, "MenuBuilder.actionPerformed() - No action associated with the event received");
	}
	//constructs an about dialog
	public void showAboutWindow()
	{
		JOptionPane myPane = new JOptionPane();
        Box about = Box.createVerticalBox();

		String aboutInfo;
		try
		{
			aboutInfo = FileStringUtility.file2String(Gateway.getProperties().getString("about"));
		}
		catch (Exception e)
		{
			aboutInfo = "CRISTAL Item Browser";
		}
        JLabel title = new JLabel(aboutInfo);
        about.add(title);

        about.add(new JLabel("Kernel version: "+Gateway.getKernelVersion()));
        about.add(new JLabel("Modules loaded: "+Gateway.getModuleManager().getModuleVersions()));
        // get license info

        StringBuffer lictxt = new StringBuffer();
        try {
			lictxt.append(Gateway.getResource().getTextResource(null, "textFiles/license.html"));
		} catch (ObjectNotFoundException e) { } // no kernel license found
        for (String ns : Gateway.getResource().getModuleBaseURLs().keySet()) {
        	String domlictxt;
			try {
				domlictxt = Gateway.getResource().getTextResource(ns, "license.html");
				lictxt.append(domlictxt).append("\n");
			} catch (ObjectNotFoundException e) { }
                
		}
        

        JEditorPane license = new JEditorPane();
        license.setEditable(false);
        license.setEditorKit(new HTMLEditorKit());
        license.setContentType("text/html");
        license.addHyperlinkListener(this);
        license.setText(lictxt.toString());
        JScrollPane scroll = new JScrollPane(license);
        scroll.setPreferredSize(new Dimension(300,200));
        license.setCaretPosition(0);

        about.add(scroll);
		myPane.setMessage(about);
		myPane.setMessageType(JOptionPane.INFORMATION_MESSAGE);
		JDialog dialog = myPane.createDialog(null, "About");
		dialog.setResizable(false);
		Icon icon = ImageLoader.findImage(Gateway.getProperties().getString("banner"));
		myPane.setIcon(icon);
        dialog.pack();
        dialog.setVisible(true);
	}

   @Override
public void hyperlinkUpdate(HyperlinkEvent e) {
       try {
           if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
               Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler "+e.getURL().toString());
       } catch (Exception ex) {
    	   MainFrame.exceptionDialog(ex);
       }
   }

	@Override
	public void itemStateChanged(java.awt.event.ItemEvent e)
	{
	}
}
