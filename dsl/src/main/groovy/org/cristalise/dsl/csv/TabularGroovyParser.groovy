/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.dsl.csv;

import java.util.List;

import groovy.lang.Closure;
import groovy.transform.CompileStatic

/**
 * Defines the common interface for parsers reading tabular data (i.e. csv, excel)
 */
@CompileStatic
public interface TabularGroovyParser {
    
    public enum ParserTypes {EXCEL, CSV}

    /**
     * Sets the number of rows to be used as header, default is 1
     * @param rowCount number of rows to be used as header (default: 1)
     */
    public void setHeaderRowCount(int rowCount)

    /**
     * Reads the header information of the file. The parser can handle headers with multiple rows. 
     * In that case the header has to be in a specific format ...
     * 
     * @return parsed header rows converted to 2 dimensional array of strings (i.e. Number of columns x Header row count)
     */
    public List<List<String>> getHeader()

    /**
     * Sets the header to be used for files which does not contain header lines
     * @param header to be used
     */
    public void setHeader(List<List<String>> header)

    /**
     * 
     * @param codeBlock
     */
    public void eachRow(Closure codeBlock)
}
