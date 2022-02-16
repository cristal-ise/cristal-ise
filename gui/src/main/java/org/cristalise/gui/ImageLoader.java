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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.swing.ImageIcon;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.process.Gateway;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ImageLoader {

    static private Hashtable<String, ImageIcon> imgCache = new Hashtable<String, ImageIcon>();
    static public final ImageIcon nullImg = new ImageIcon(new byte[] {0});
    static private final ArrayList<String> reportedMissingIcons = new ArrayList<String>();

    /**
     * Gets an image from the resource directories
     *
     * @param resName - filename after resources/images
     * @return
     */
    static public ImageIcon findImage(String resName) {
        try {
            for (String ns : Gateway.getResource().getModuleBaseURLs().keySet()) {
                try {
                    return getImage(ns, resName);
                } catch (ObjectNotFoundException ex) {
                }
            }
            return getImage(null, resName);
        } catch (ObjectNotFoundException ex) {
            if (!reportedMissingIcons.contains(resName)) {
                log.warn("Image '" + resName + "' not found. Using null icon");
                reportedMissingIcons.add(resName);
            }
            return nullImg;
        }
    }

    static public ImageIcon getImage(String ns, String resName) throws ObjectNotFoundException {
        if (resName == null)
            return nullImg;

        if (imgCache.containsKey(ns + '/' + resName)) {
            return imgCache.get(ns + '/' + resName);
        }

        URL imgLocation = null;
        if (ns == null)
            try {
                imgLocation = Gateway.getResource().getKernelResourceURL("images/" + resName);
            } catch (MalformedURLException ex) {
            }
        else
            try {
                imgLocation = Gateway.getResource().getModuleResourceURL(ns, "images/" + resName);
            } catch (MalformedURLException ex) {
            }

        if (imgLocation != null) {
            ImageIcon newImg = new ImageIcon(imgLocation);

            if (newImg.getIconHeight() > -1) {
                imgCache.put(ns + '/' + resName, newImg);
                log.info("Loaded " + resName + " " + newImg.getIconWidth() + "x"
                        + newImg.getIconHeight());
                return newImg;
            }
        }
        throw new ObjectNotFoundException();
    }

}
