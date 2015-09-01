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
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Array;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;

import org.castor.core.util.Base64Decoder;
import org.castor.core.util.Base64Encoder;
import org.cristalise.gui.MainFrame;


public class ImageEditField extends StringEditField {

	JLabel imageLabel;

	Box imagePanel;

	JButton browseButton;

	String encodedImage;

	static JFileChooser chooser = new JFileChooser();
	static {
		chooser.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {
					@Override
					public String getDescription() {
						return "Image Files";
					}

					@Override
					public boolean accept(File f) {
						return (f.isDirectory() || (f.isFile() && (f.getName()
								.endsWith(".gif")
								|| f.getName().endsWith(".jpg")
								|| f.getName().endsWith(".jpeg")
								|| f.getName().endsWith(".png"))));
					}
				});
	}

	public ImageEditField() {
		super();
		imageLabel = new JLabel();
		imagePanel = Box.createVerticalBox();
		browseButton = new JButton("Browse");
		browseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int returnVal = chooser.showOpenDialog(null);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = chooser.getSelectedFile();
					try {
						FileInputStream fis = new FileInputStream(file);
						byte[] bArray = (byte[]) Array.newInstance(byte.class,
								(int) file.length());
						fis.read(bArray, 0, (int) file.length());
						fis.close();

						ImageIcon newImage = new ImageIcon(Toolkit
								.getDefaultToolkit().createImage(bArray));
						imageLabel.setIcon(newImage);
						encodedImage = String.valueOf(Base64Encoder.encode(bArray));
					} catch (Exception ex) {
						MainFrame.exceptionDialog(ex);
					}
				}
			}
		});
		imagePanel.add(imageLabel);
		imagePanel.add(Box.createVerticalStrut(5));
		imagePanel.add(browseButton);
	}

	@Override
	public String getDefaultValue() {
		return "";
	}

	@Override
	public Component getControl() {
		return imagePanel;
	}

	@Override
	public String getText() {
		return encodedImage == null ? "" : encodedImage;
	}

	@Override
	public void setText(String text) {
		encodedImage = text;
		if (text != null && text.length() > 0) {
			byte[] decodedImage = Base64Decoder.decode(encodedImage);
			imageLabel.setIcon(new ImageIcon(Toolkit.getDefaultToolkit()
					.createImage(decodedImage)));
		}
	}

	@Override
	public void setEditable(boolean editable) {
		browseButton.setVisible(false);
	}
}
