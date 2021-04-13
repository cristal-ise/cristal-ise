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
package org.cristalise.kernel.common;

public final class GTimeStamp {
    public int mYear       = (int) 0;
    public int mMonth      = (int) 0;
    public int mDay        = (int) 0;
    public int mHour       = (int) 0;
    public int mMinute     = (int) 0;
    public int mSecond     = (int) 0;
    public int mTimeOffset = (int) 0;

    public GTimeStamp() {
    } // ctor

    public GTimeStamp(int _mYear, int _mMonth, int _mDay, int _mHour, int _mMinute, int _mSecond, int _mTimeOffset) {
        mYear = _mYear;
        mMonth = _mMonth;
        mDay = _mDay;
        mHour = _mHour;
        mMinute = _mMinute;
        mSecond = _mSecond;
        mTimeOffset = _mTimeOffset;
    } // ctor

} // class GTimeStamp
