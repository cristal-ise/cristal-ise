/**
 * This file is part of the CRISTAL-iSE XPath Outcome Initiator module.
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
package org.cristalise.kernel.persistency.outcomebuilder;

import java.io.IOException;

import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.Logger;
import org.junit.Test;

public class OutcomeBulderTest {

    @Test
    public void test() throws Exception {
        //OutcomeBuilder ob = new OutcomeBuilder( getXSD("Module"), getXML("module"));
        //OutcomeBuilder ob = new OutcomeBuilder( getXSD("PatientDetails"));
        //OutcomeBuilder ob = new OutcomeBuilder( getXSD("Item"));
        //OutcomeBuilder ob = new OutcomeBuilder( getXSD("PatientDetails"));
        OutcomeBuilder ob = new OutcomeBuilder( getXSD("Storage"));

        ob.setSelectedRoot("StorageDetails");
        ob.initialise();
        ob.createNewOutcome();
        Logger.msg(ob.getOutcome());

        /*
        ob.setSelectedRoot("StorageAmount");
        ob.initialise();
        ob.createNewOutcome();
        Logger.msg(ob.getOutcome());

        ob.setSelectedRoot("Storage");
        ob.initialise();
        ob.createNewOutcome();
        Logger.msg(ob.getOutcome());
         */
    }

    public String getXML(String name) throws IOException {
        return FileStringUtility.url2String(OutcomeBulderTest.class.getResource("/"+name+".xml"));
    }

    public String getXSD(String name) throws IOException {
        return FileStringUtility.url2String(OutcomeBulderTest.class.getResource("/"+name+".xsd"));
    }

}
