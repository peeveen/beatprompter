package com.stevenfrew.beatprompter;

class CancelEvent
{
    private boolean mCancelled=false;
    CancelEvent()
    {
    }
    void set()
    {
        mCancelled=true;
    }
    boolean isCancelled()
    {
        return mCancelled;
    }
}
