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
package org.cristalise.gui.tabs.outcome.form;

import javax.swing.JEditorPane;
import javax.swing.text.html.HTMLEditorKit;

import org.cristalise.kernel.utils.Language;


/**************************************************************************
 *
 * $Revision: 1.3 $
 * $Date: 2004/08/24 12:44:02 $
 *
 * Copyright (C) 2003 CERN - European Organization for Nuclear Research
 * All rights reserved.
 **************************************************************************/

public class HelpPane extends JEditorPane {

    public static final String header = "<h2><font color=\"blue\">"+Language.translate("Help")+"</font></h2>";

    public HelpPane() {
        super();
        setEditable(false);
        setEditorKit(new HTMLEditorKit());
        setContentType("text/html");
        setPreferredSize(new java.awt.Dimension(200,400));
    }

    public void setHelp(String title, String helpText) {
        setText(header+"<h3>"+title+"</h3><br>"+toHTML(helpText));
    }


    /**
     * Unfortunately JEditorPane will only display HTML3.2, whereas to embed HTML in an xsd we must
     * use XHTML so it will be valid XML. This method does a quick and dirty removal of stuff that
     * the JEditorPane cannot display
     *
     * @param xhtml
     * @return
     */
    public static String toHTML(String xhtml) {
        int startPos, endPos;
        //remove xml header
        while((startPos = xhtml.indexOf("<?")) != -1 &&
            (endPos = xhtml.indexOf("?>")) != -1) {
            xhtml = xhtml.substring(0,startPos)+xhtml.substring(endPos+2);
        }
        // remove slash in <tags/>
        while ((startPos = xhtml.indexOf("/>")) != -1) {
            xhtml = xhtml.substring(0, startPos)+xhtml.substring(startPos+1);
        }
        return xhtml;
    }
}
