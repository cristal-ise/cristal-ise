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
package org.cristalise.kernel.entity;


import java.nio.ByteBuffer;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.SystemKey;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class TraceableLocator extends org.omg.PortableServer.ServantLocatorPOA {

    public TraceableLocator() {
    }


    @Override
	public org.omg.PortableServer.Servant preinvoke(
              byte[]                                                    oid,
              org.omg.PortableServer.POA                                poa,
              String                                                    operation,
              org.omg.PortableServer.ServantLocatorPackage.CookieHolder cookie )
    {
        ByteBuffer bb = ByteBuffer.wrap(oid);
        long msb = bb.getLong();
        long lsb = bb.getLong();
        ItemPath syskey = new ItemPath(new SystemKey(msb, lsb));

        log.info("===========================================================");
        log.info("Item called: " + operation + "(" + syskey + ")." );

        try {
            return Gateway.getCorbaServer().getItem(syskey, null);
        }
        catch (ObjectNotFoundException ex) {
            log.error("preinvoke", ex);
            throw new org.omg.CORBA.OBJECT_NOT_EXIST();
        }
    }


    @Override
	public void postinvoke(
               byte[]                           oid,
               org.omg.PortableServer.POA       poa,
               String                           operation,
               java.lang.Object                 the_cookie,
               org.omg.PortableServer.Servant   the_servant )
    { }
}
