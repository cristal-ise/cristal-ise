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
