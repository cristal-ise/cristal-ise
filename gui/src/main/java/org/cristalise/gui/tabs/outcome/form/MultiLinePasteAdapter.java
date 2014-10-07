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
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.StringTokenizer;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;

import org.cristalise.gui.MainFrame;
import org.cristalise.kernel.utils.Logger;

/**
* ExcelAdapter enables Copy-Paste Clipboard functionality on JTables.
* The clipboard data format used by the adapter is compatible with
* the clipboard format used by Excel. This provides for clipboard
* interoperability between enabled JTables and Excel.
*/
public class MultiLinePasteAdapter implements ActionListener {
    private String rowstring, value;
    private Clipboard system;
    private StringSelection stsel;
    private JTable jTable1;
    private Dimension parent;
    /**
     * The Excel Adapter is constructed with a
     * JTable on which it enables Copy-Paste and acts
     * as a Clipboard listener.
     */
    public MultiLinePasteAdapter(JTable myJTable, Dimension parent) {
        jTable1 = myJTable;
        this.parent = parent;
        KeyStroke copy =
            KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK, false);
        // Identifying the copy KeyStroke user can modify this
        // to copy on some other Key combination.
        KeyStroke paste =
            KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK, false);
        // Identifying the Paste KeyStroke user can modify this
        //to copy on some other Key combination.
        jTable1.registerKeyboardAction(
            this,
            "Copy",
            copy,
            JComponent.WHEN_FOCUSED);
        jTable1.registerKeyboardAction(
            this,
            "Paste",
            paste,
            JComponent.WHEN_FOCUSED);
        system = Toolkit.getDefaultToolkit().getSystemClipboard();
    }
    /**
     * Public Accessor methods for the Table on which this adapter acts.
     */
    public JTable getJTable() {
        return jTable1;
    }
    public void setJTable(JTable jTable1) {
        this.jTable1 = jTable1;
    }
    /**
     * This method is activated on the Keystrokes we are listening to
     * in this implementation. Here it listens for Copy and Paste ActionCommands.
     * Selections comprising non-adjacent cells result in invalid selection and
     * then copy action cannot be performed.
     * Paste is done by aligning the upper left corner of the selection with the
     * 1st element in the current selection of the JTable.
     */
    @Override
	public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().compareTo("Copy") == 0) {
            StringBuffer sbf = new StringBuffer();
            // Check to ensure we have selected only a contiguous block of
            // cells
            int numcols = jTable1.getSelectedColumnCount();
            int numrows = jTable1.getSelectedRowCount();
            int[] rowsselected = jTable1.getSelectedRows();
            int[] colsselected = jTable1.getSelectedColumns();
            if (!((numrows - 1
                == rowsselected[rowsselected.length - 1] - rowsselected[0]
                && numrows == rowsselected.length)
                && (numcols - 1
                    == colsselected[colsselected.length - 1] - colsselected[0]
                    && numcols == colsselected.length))) {
                JOptionPane.showMessageDialog(
                    null,
                    "Invalid Copy Selection",
                    "Invalid Copy Selection",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            for (int i = 0; i < numrows; i++) {
                for (int j = 0; j < numcols; j++) {
                    sbf.append(
                        jTable1.getValueAt(rowsselected[i], colsselected[j]));
                    if (j < numcols - 1)
                        sbf.append("\t");
                }
                sbf.append("\n");
            }
            stsel = new StringSelection(sbf.toString());
            system = Toolkit.getDefaultToolkit().getSystemClipboard();
            system.setContents(stsel, stsel);
        }
        if (e.getActionCommand().compareTo("Paste") == 0) {
            Logger.msg(5, "Trying to Paste");
            int startRow = (jTable1.getSelectedRows())[0];
            int startCol = (jTable1.getSelectedColumns())[0];
            try {
                String trstring =
                    (String) (system.getContents(this).getTransferData(DataFlavor.stringFlavor));
                Logger.msg(8, "String is:" + trstring);
                StringTokenizer st1 = new StringTokenizer(trstring, "\n\r");
                for (int i = 0; st1.hasMoreTokens(); i++) {
                    rowstring = st1.nextToken();
                    StringTokenizer st2 = new StringTokenizer(rowstring, "\t");
                    for (int j = 0; st2.hasMoreTokens(); j++) {
                        value = st2.nextToken();
                        if (startRow + i == jTable1.getRowCount())
                            parent.addRow(startRow+i);
                        if (startRow + i < jTable1.getRowCount()
                            && startCol + j < jTable1.getColumnCount())
                            jTable1.setValueAt(
                                value,
                                startRow + i,
                                startCol + j);
                        Logger.msg(5, "Putting "+value+" at row="+(startRow+i)+" column="+(startCol+j));
                    }
                }
            } catch (Exception ex) {
            	MainFrame.exceptionDialog(ex);
            }

        }
    }
}
