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
package org.cristalise.kernel.lifecycle;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Vector;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.graph.model.GraphableVertex;
import org.cristalise.kernel.lifecycle.instance.WfVertex;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.utils.KeyValuePair;

/**
 * 
 */
public abstract class WfVertexDef extends GraphableVertex {
    public Vector<String> mErrors;

    protected boolean loopTested;

    /**
     * 
     */
    public WfVertexDef() {
        mErrors = new Vector<String>(0, 1);
        setIsLayoutable(true);
    }

    public abstract WfVertex instantiate(TransactionKey transactionKey) throws ObjectNotFoundException, InvalidDataException;

    /**
     * Copies Properties from vertex definition to vertex instance, and also set the Edges
     * 
     * @param newVertex the vertex instance to be configured
     * @throws InvalidDataException inconsistent data
     * @throws ObjectNotFoundException data was not found
     */
    public void configureInstance(WfVertex newVertex, TransactionKey transactionKey) throws InvalidDataException, ObjectNotFoundException {
        for (KeyValuePair element : getProperties().getKeyValuePairs()) {
            newVertex.getProperties().put(element.getKey(), element.getValue(), element.isAbstract());
        }

        newVertex.setID(getID());

        if (getIsLayoutable()) {
            newVertex.setInEdgeIds(getInEdgeIds());
            newVertex.setOutEdgeIds(getOutEdgeIds());
            newVertex.setCentrePoint(getCentrePoint());
            newVertex.setOutlinePoints(getOutlinePoints());
        }
    }

    /**
     * Method verify.
     *
     * @return boolean
     */
    public abstract boolean verify();

    /**
     * Method getErrors.
     *
     * @return String
     */
    public String getErrors() {
        if (mErrors.size() == 0) return "No error";
        else if (mErrors.size() == 1) return mErrors.elementAt(0);
        else {
            StringBuffer errorBuffer = new StringBuffer();
            for (String error : mErrors) {
                if (errorBuffer.length() > 0) errorBuffer.append(", ");
                errorBuffer.append(error);
            }
            return errorBuffer.toString();
        }
    }

    /**
     * Method loop.
     *
     * @return boolean
     */
    public boolean loop() {
        boolean loop2 = false;
        if (!loopTested) {
            loopTested = true;
            if (getOutGraphables().length != 0) loop2 = ((WfVertexDef) getOutGraphables()[0]).loop();
        }
        loopTested = false;
        return loop2;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"("+(isNotBlank(getName()) ? "name:"+getName() : "id:"+getID())+")";
    }
}