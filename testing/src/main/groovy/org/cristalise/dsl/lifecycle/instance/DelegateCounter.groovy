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

package org.cristalise.dsl.lifecycle.instance

import static org.cristalise.kernel.lifecycle.instance.WfVertex.Types.*

import org.cristalise.kernel.lifecycle.instance.WfVertex.Types


/**
 * 
 *
 */
//@CompileStatic
class DelegateCounter {

    private static Map<Types, Integer> counter = [:]

    public static void reset() {
        counter = [
            (Atomic):    -1,
            (Composite): -1,
            (OrSplit):   -1,
            (XOrSplit):  -1,
            (AndSplit):  -1,
            (LoopSplit): -1]
    }

    public static int getNextCount(Types t) {
        return ++counter[t]
    }

}
