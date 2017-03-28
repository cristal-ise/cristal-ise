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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.cristalise.gui.tabs.execution.DefaultExecutor;
import org.cristalise.gui.tabs.execution.Executor;
import org.cristalise.gui.tree.NodeContext;
import org.cristalise.gui.tree.NodeRole;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.process.AbstractMain;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;
import org.omg.CORBA.UserException;

/**
 * @version $Revision: 1.83 $ $Date: 2005/09/12 14:56:19 $
 * @author  $Author: abranson $
 */
public class MainFrame extends javax.swing.JFrame {
    public static TreeBrowser domBrowser;
    public static TreeBrowser roleBrowser;
    public static ItemTabManager myDesktopManager;
    public static ItemFinder itemFinder;
    protected MenuBuilder menuBuilder;
    protected org.omg.CORBA.ORB orb;
    public static Properties prefs = new Properties();
    
    public static ProgressReporter progress;
    public String logoURL;
    public static AgentProxy userAgent;
    protected JSplitPane splitPane;
    private JTabbedPane treePanel;
    public static boolean isAdmin;
    int splitPanePos;
    public static final JFileChooser xmlChooser;

    static {
        xmlChooser = new JFileChooser();
        xmlChooser.addChoosableFileFilter(
            new javax.swing.filechooser.FileFilter() {
                @Override
				public String getDescription() {
                    return "XML Files";
                }
                @Override
				public boolean accept(File f) {
                    if (f.isDirectory() || (f.isFile() && f.getName().endsWith(".xml"))) {
                        return true;
                    }
                    return false;
                }
            });
    }
    /** Creates new gui client for Cristal2 */

    public MainFrame() {

        // Load gui preferences
        try {
            FileInputStream prefsfile =
                new FileInputStream("cristal.preferences");
            prefs.load(prefsfile);
            prefsfile.close();
        } catch (IOException e) {
            Logger.msg(2, "Creating new preference file");
        }

        // set look & feel from pref
        try {
            String lf = getPref("Style", null);
            if (lf == null)
                lf = UIManager.getCrossPlatformLookAndFeelClassName();
            UIManager.setLookAndFeel(lf);
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showLogin() {
        // Log in
        logoURL = Gateway.getProperties().getString("Logo");
        URL pictureUrl;
        String bottomMessage = "Please enter username & password";
        ImageIcon imageHolder = new ImageIcon("");
        try {
            pictureUrl = new URL(logoURL);
            imageHolder = new ImageIcon(pictureUrl);
        } catch (java.net.MalformedURLException m) {
            imageHolder = ImageLoader.findImage(logoURL);
        }

        LoginBox login =
            new LoginBox(
                5,
                Gateway.getProperties().getString("Name"),
                getPref("lastUser."+Gateway.getCentreId(), null),
                bottomMessage,
                imageHolder, this);

        login.setVisible(true);
    }

    public void mainFrameShow() {
        prefs.setProperty("lastUser."+Gateway.getCentreId(), userAgent.getName());
        isAdmin = userAgent.getPath().hasRole("Admin");
        GridBagLayout gridbag = new GridBagLayout();
        getContentPane().setLayout(gridbag);

        this.setTitle(
                userAgent.getName()+"@"+Gateway.getProperties().getString("Name") + " - Cristal");

        String iconFile = Gateway.getProperties().getString("AppIcon");
        if (iconFile != null)
            this.setIconImage(ImageLoader.findImage(iconFile).getImage());

        //preload loading image
        ImageLoader.findImage("loading.gif");
        // close listener
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
			public void windowClosing(java.awt.event.WindowEvent evt) {
                exitForm();
            }
        });
        // initialise the desktop manager
        myDesktopManager = new ItemTabManager();

        //get the menu bar and add it to the frame
        menuBuilder = new MenuBuilder(this);
        setJMenuBar(menuBuilder);

        // set the menu builder in the window manager
        myDesktopManager.setMenuBuilder(menuBuilder);

        treePanel = new JTabbedPane(JTabbedPane.TOP);
        NodeContext userNode = new NodeContext(new DomainPath(""), myDesktopManager);
        domBrowser = new TreeBrowser(myDesktopManager, userNode);
        NodeRole roleNode = new NodeRole(new RolePath(), MainFrame.myDesktopManager);
        roleBrowser = new TreeBrowser(MainFrame.myDesktopManager, roleNode);
        treePanel.add("Domain", domBrowser);
        treePanel.add("Roles", roleBrowser);
        treePanel.setVisible(getPref("ShowTree", "true").equals("true"));

        // add search box
        itemFinder = new ItemFinder();
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        gridbag.setConstraints(itemFinder, c);
        getContentPane().add(itemFinder);
        // register the browser as the key consumer
        itemFinder.setDefaultConsumer(domBrowser);

        c.gridy++;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        gridbag.setConstraints(getSplitPanel(), c);
        getContentPane().add(getSplitPanel());

        progress = new ProgressReporter();
        c.gridy++;
        c.weighty = 0.0;
        gridbag.setConstraints(progress, c);
        getContentPane().add(progress);
        pack();
        
        String paneSize = getPref("WindowSize", null);
        if (paneSize != null) {
            StringTokenizer tok = new StringTokenizer(paneSize, ",");
            Dimension window = new Dimension();
            window.setSize(
                Integer.parseInt(tok.nextToken()),
                Integer.parseInt(tok.nextToken()));
            this.setSize(window);
        }
        String panePos = getPref("WindowPosition", null);
        if (panePos != null) {
            StringTokenizer tok = new StringTokenizer(panePos, ",");
            Point window =
                new Point(
                    Integer.parseInt(tok.nextToken()),
                    Integer.parseInt(tok.nextToken()));
            this.setLocation(window);
        }
        super.toFront();
        this.validate();
        this.setVisible(true);
    }
    public static String getPref(String name, String defaultValue) {
        return prefs.getProperty(name, defaultValue);
    }
    public static void setPref(String name, String value) {
        prefs.setProperty(name, value);
    }
    // things to do on exit
    public void exitForm() {
        // save window sizing
        setPref(
            "WindowSize",
            (int) (this.getSize().getWidth())
                + ","
                + (int) (this.getSize().getHeight()));
        setPref(
            "WindowPosition",
            (int) (this.getLocation().getX())
                + ","
                + (int) (this.getLocation().getY()));
        setPref(
            "Style",
            UIManager.getLookAndFeel().getClass().getName());
        setPref(
            "SplitPanePosition",
            String.valueOf(splitPane.getDividerLocation()));
        // save preferences file
        try {
            FileOutputStream prefsfile =
                new FileOutputStream("cristal.preferences", false);
            prefs.store(prefsfile, "Cristal 2");
            prefsfile.close();
        } catch (Exception e) {
            Logger.warning(
                "Could not write to preferences file. Preferences have not been updated.");
        }
        this.dispose();
        AbstractMain.shutdown(0);
    }

    public void toggleTree() {
    	boolean showTree = getPref("ShowTree", "true").equals("false");
        setPref("ShowTree", String.valueOf(showTree));
        if (!showTree) splitPanePos = splitPane.getDividerLocation();
        getSplitPanel().getLeftComponent().setVisible(showTree);
        if (showTree) getSplitPanel().setDividerLocation(splitPanePos);
        getSplitPanel().validate();
    }

    public static JComboBox<Executor> getExecutionPlugins() {
        JComboBox<Executor> plugins = new JComboBox<Executor>();
        // create execution selector
        Executor defaultExecutor = new DefaultExecutor();
        plugins.addItem(defaultExecutor);
        plugins.setSelectedIndex(0);

        // load execution plugins
        String pluginList = Gateway.getProperties().getString("Executors");
        if (pluginList != null) {
            StringTokenizer tok = new StringTokenizer(pluginList, ",");
            while (tok.hasMoreTokens()) {
                String pluginName = tok.nextToken();
                try {
                    Class<?> pluginClass = Class.forName(pluginName);
                    Executor domainExecutor = (Executor)pluginClass.newInstance();
                    plugins.addItem(domainExecutor);
                } catch (Exception ex) {
                    Logger.error("Could not load the executor plugin "+pluginName);
                }
            }
        }
        return plugins;
    }
    protected JSplitPane getSplitPanel()
    {
        //  create the split pane, and add the Tree to it.
        if (splitPane == null)
        {
            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, treePanel, myDesktopManager);
            splitPane.setDividerSize(5);
            splitPanePos = Integer.parseInt(getPref("SplitPanePosition", "200"));
            splitPane.setDividerLocation(splitPanePos);
        }
        return splitPane;
    }

	static public void exceptionDialog(Exception ex)
	{
        if (Logger.doLog(8)) Logger.error(ex);

		String className = ex.getClass().getSimpleName();
		String error = ex.getMessage();
		if (ex instanceof UserException)
			error = error.substring(error.indexOf(' ') + 1);
		JOptionPane.showMessageDialog(null, error, className, JOptionPane.ERROR_MESSAGE);
	}

}
