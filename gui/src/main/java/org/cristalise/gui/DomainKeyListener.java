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

import java.io.IOException;

import javax.swing.ImageIcon;

/**
 * Interface for external key input classes (e.g. barcode scanner)
 * @version $Revision: 1.5 $ $Date: 2004/10/20 14:10:21 $
 * @author  $Author: abranson $
 */

public interface DomainKeyListener {
    public void init();

    public boolean enable() throws IOException;

    public void setConsumer(ItemFinder consumer);

    public void disable();

    // return 25x25 icon for enable/disable button
    public ImageIcon getIcon();

    // tooltip for the button
    public String getDescription();
}
