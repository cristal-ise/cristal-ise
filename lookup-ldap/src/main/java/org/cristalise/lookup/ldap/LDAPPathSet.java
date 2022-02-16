/**
 * This file is part of the CRISTAL-iSE LDAP lookup plugin.
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
package org.cristalise.lookup.ldap;

import java.util.Iterator;

import org.cristalise.kernel.lookup.Path;

import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchResults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LDAPPathSet implements Iterator<Path> {
    LDAPSearchResults results;
    LDAPEntry nextEntry;
    LDAPLookup ldap;

    public LDAPPathSet(LDAPLookup ldap) { // empty
        this.ldap = ldap;
        results = null;
    }

    public LDAPPathSet(LDAPSearchResults results, LDAPLookup ldap) {
        this.ldap = ldap;
        this.results = results;
    }

    @Override
    public boolean hasNext() {
        if (results == null)
            return false;
        if (nextEntry != null)
            return true;
        if (results.hasMore())
            try {
                nextEntry = results.next();
                return true;
            } catch (LDAPException ex) {
                if (ex.getResultCode() != 32) {// no results
                    log.error("",ex);
                }
            }
        return false;
    }

    @Override
    public Path next() {
        if (results == null)
            return null;
        try {
            if (nextEntry == null)
                nextEntry = results.next();
            Path nextPath = ldap.nodeToPath(nextEntry);
            nextEntry = null;
            return nextPath;
        } catch (Exception ex) {
            log.error("",ex);
            nextEntry = null;
            if (hasNext()) {
                log.error("Skipping to next entry");
                return next();
            } else
                return null;
        }
    }

    @Override
    public void remove() {
        // do nothing

    }
}
