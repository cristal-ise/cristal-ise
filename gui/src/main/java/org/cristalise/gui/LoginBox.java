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

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.process.AbstractMain;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;



//import com.borland.jbcl.layout.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public  class LoginBox extends JFrame {

  public String errorMessage=new String("");
  String title;
  private int maxNumberLogon;
  public boolean action = false;
  public int loginAttemptNumber= 0;
  JLabel passwordLabel = new JLabel();
  JTextField username = new JTextField();
  JButton OK = new JButton();
  JLabel errorLabel = new JLabel();
  JPasswordField password = new JPasswordField();
  JButton Cancel = new JButton();
  JLabel userLabel = new JLabel();
  ImageIcon imageMainHolder = new ImageIcon();
  JLabel pictureLabel = new JLabel();
  GridBagLayout gridBagLayout1 = new GridBagLayout();
  MainFrame mainFrameFather;
  public  static AgentProxy userAgent;
  private boolean logged;
  private boolean errorSet;

  public LoginBox(int attempt,String title,String lastUser,String bottomMessage,
                  javax.swing.ImageIcon imageHolder,MainFrame mainFrame) {
	String iconFile = Gateway.getProperties().getString("AppIcon");
	if (iconFile != null)
		this.setIconImage(ImageLoader.findImage(iconFile).getImage());
    this.errorLabel.setText(bottomMessage);
    if (errorMessage.compareTo("")!=0) this.errorLabel.setText(errorMessage);
    mainFrameFather=mainFrame;
    imageMainHolder=imageHolder;
    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    if (attempt==0) maxNumberLogon=5;
    else maxNumberLogon=attempt;
    if (title == null)
      this.title = "Cristal";
    else
      this.title = title;
    setTitle("Log in to "+title);
    username.setText(lastUser);

  }
//OK button pressed OR Enter Hit
  private void loginClicked(){
    errorSet=false;
    try {
      if (this.getUser().length() > 0 && this.getPassword().length() > 0) {
          userAgent = Gateway.getSecurityManager().authenticate(this.getUser(), this.getPassword(), title, true);
      }

      logged = (userAgent != null);
      Logger.msg(7, "AbstractMain::standardSetUp() - Gateway.connect() OK.");
    }
    catch (Exception ex) {
      String message = ex.getMessage();
      int i = ex.getMessage().indexOf(' ');
      if (i > -1 ) message = message.substring(i);
      //Here use language translate I guess :)
      //if (message.length()>65 && message.substring(1,5).compareTo("User")==0)
      //  message = (message.substring(1,50)+ "... not found" );
      this.errorLabel.setText(message);
      Logger.error(message);
      logged= false;
      errorSet=true;
    }
    if (!logged) {
      Logger.msg("Login attempt "+loginAttemptNumber+" of "+maxNumberLogon+" failed");
      if (loginAttemptNumber>=maxNumberLogon)  {
        dispose();
        Logger.error("Login failure limit reached");
        AbstractMain.shutdown(1);
      }
      if (!errorSet) this.errorLabel.setText("Please enter username & password");
//      int posx=xMov+120;
//      int posy=yMov;
//      if (posy<135) posy=135;
//      float texstSize = errorLabel.getFont().getSize2D();
//      if (posx-xMov<errorLabel.getText().length()*(texstSize/2))
//        posx=errorLabel.getText().length()*(int)(texstSize/2)+xMov;


      // obtain screen dimensions
//    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
//    this.setBounds(screen.width/2-posx/2, screen.height/2-posy/2,posx,posy);
    this.validate();
    }

    else {
      MainFrame.userAgent = userAgent;
      this.setVisible(false);
      mainFrameFather.mainFrameShow();
      Logger.msg(1, "Login attempt "+loginAttemptNumber+" of "+maxNumberLogon+" succeeded.");
      dispose();
    }
  }


  private void jbInit() throws Exception {

    //this.getContentPane().setBackground(SystemColor.control);
    this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    addWindowListener(new java.awt.event.WindowAdapter() {
        @Override
		public void windowClosing(java.awt.event.WindowEvent evt) {
        	AbstractMain.shutdown(0);
        }
    });
    this.setEnabled(true);
    this.setLocale(java.util.Locale.getDefault());
    this.setResizable(false);
    this.setState(Frame.NORMAL);
    this.setTitle("");
    LoginBox_this_keyAdapter submitListener = new LoginBox_this_keyAdapter(this);
    username.addKeyListener(submitListener);
    password.addKeyListener(submitListener);
    this.getContentPane().setLayout(gridBagLayout1);

    passwordLabel.setText("Password:");

    OK.setActionCommand("OK");
    OK.setSelected(true);
    OK.setText("OK");
    OK.addActionListener(new Frame2_OK_actionAdapter(this));
    OK.setPreferredSize(new Dimension(80,30));

    Cancel.setActionCommand("Cancel");
    Cancel.setText("Cancel");
    Cancel.addActionListener(new Frame2_Cancel_actionAdapter(this));
    Cancel.setPreferredSize(new Dimension(80,30));

    userLabel.setText("User:");
    pictureLabel= new JLabel(imageMainHolder);
    pictureLabel.setBorder(new EmptyBorder(0,0,0,5));
    password.setText("");

    username.setText("");

    GridBagConstraints c = new GridBagConstraints();
    initBasicConstraints(c,1,1,1,4);
    c.anchor=GridBagConstraints.CENTER;
    c.fill = GridBagConstraints.NONE;
    c.weightx=0;
    c.weighty=1;
    getContentPane().add(pictureLabel,c);

    initBasicConstraints(c,2,1,1,1);
    c.anchor=GridBagConstraints.SOUTHWEST;
    c.fill = GridBagConstraints.NONE;
    c.weightx=0;
    c.weighty=1;
    getContentPane().add(userLabel,c);
    initBasicConstraints(c,2,2,1,1);
    c.anchor=GridBagConstraints.SOUTHWEST;
    c.fill = GridBagConstraints.NONE;
    c.weightx=0;
    c.weighty=1;
    getContentPane().add(passwordLabel,c);

    initBasicConstraints(c,3,1,1,1);
    c.anchor=GridBagConstraints.SOUTH;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.weightx=1;
    c.weighty=1;
    getContentPane().add(username,c);

    initBasicConstraints(c,3,2,1,1);
    c.anchor=GridBagConstraints.SOUTH;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.weightx=1;
    c.weighty=1;
    getContentPane().add(password,c);

    JPanel buttonPane = new JPanel();
    buttonPane.setLayout(new BoxLayout(buttonPane,BoxLayout.X_AXIS));
    buttonPane.add(Box.createGlue());
    buttonPane.add(OK);
    buttonPane.add(Box.createRigidArea(new Dimension(5,0)));
    buttonPane.add(Cancel);
    buttonPane.add(Box.createGlue());
    buttonPane.setBorder(new EmptyBorder(5,0,0,0));

    initBasicConstraints(c,2,3,2,1);
    c.weightx=0;
    c.weighty=1;
    c.anchor=GridBagConstraints.SOUTH;
    c.fill = GridBagConstraints.BOTH;
    getContentPane().add(buttonPane,c);

    initBasicConstraints(c,2,4,2,1);
    c.weightx=1;
    c.weighty=1;
    c.fill = GridBagConstraints.BOTH;
    c.anchor= GridBagConstraints.SOUTHEAST;
    JPanel msgPane = new JPanel();
    msgPane.setLayout(new BoxLayout(msgPane,BoxLayout.X_AXIS));
    msgPane.add(Box.createGlue());
    msgPane.add(errorLabel);
    msgPane.add(Box.createGlue());
    getContentPane().add(msgPane,c);

    ((JPanel)getContentPane()).setBorder(new EmptyBorder(0,0,0,5));
    pack();
  Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
  setLocation(screen.width/2-getWidth()/2, screen.height/2-getHeight()/2);

  }



  protected void initBasicConstraints(GridBagConstraints constraints,int x,int y,int width,int height)
  {
    constraints.gridx=x;
    constraints.gridy=y;
    constraints.gridwidth=width;
    constraints.gridheight=height;
  }

  public String getUser() {
    return username.getText();
  }

  public String getPassword() {
    return String.valueOf(password.getPassword());
  }

  void Cancel_actionPerformed(ActionEvent e) {
	dispose();
    Logger.msg("User cancelled login.");
    AbstractMain.shutdown(0);
  }

  void OK_actionPerformed(ActionEvent e) {
    try{
      this.loginAttemptNumber++;
      loginClicked();}
    catch (Exception ex){
        Logger.error(ex);
    }
  }

  void this_keyPressed(KeyEvent e) {
    if (e.getKeyCode()==10){
      try{
        this.loginAttemptNumber++;
        loginClicked();
      }
      catch (Exception ex){
          Logger.error(ex);
      }
    }
  }

}

class Frame2_Cancel_actionAdapter implements java.awt.event.ActionListener {
  LoginBox adaptee;

  Frame2_Cancel_actionAdapter(LoginBox adaptee) {
    this.adaptee = adaptee;
  }
  @Override
public void actionPerformed(ActionEvent e) {
    adaptee.Cancel_actionPerformed(e);
  }
}

class Frame2_OK_actionAdapter implements java.awt.event.ActionListener {
  LoginBox adaptee;

  Frame2_OK_actionAdapter(LoginBox adaptee) {
    this.adaptee = adaptee;
  }
  @Override
public void actionPerformed(ActionEvent e) {
    adaptee.OK_actionPerformed(e);
  }
}

class LoginBox_this_keyAdapter extends java.awt.event.KeyAdapter {
  LoginBox adaptee;

  LoginBox_this_keyAdapter(LoginBox adaptee) {
    this.adaptee = adaptee;
  }
  @Override
public void keyPressed(KeyEvent e) {
    adaptee.this_keyPressed(e);
  }
}