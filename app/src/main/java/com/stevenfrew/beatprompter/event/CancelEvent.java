package com.stevenfrew.beatprompter.event;

public class CancelEvent
{
    private boolean mCancelled=false;
    public CancelEvent()
    {
    }
    public void set()
    {
        mCancelled=true;
    }
    public boolean isCancelled()
    {
        return mCancelled;
    }
}
