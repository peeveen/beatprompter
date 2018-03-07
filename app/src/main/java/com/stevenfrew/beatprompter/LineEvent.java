package com.stevenfrew.beatprompter;

class LineEvent extends BaseEvent
{
    Line mLine;
    long mDuration;

    LineEvent(long eventTime,long duration)
    {
        super(eventTime);
        mPrevLineEvent=this;
        mDuration=duration;
    }

    protected void offset(long amount)
    {
        super.offset(amount);
        mLine.mYStartScrollTime+=amount;
        mLine.mYStopScrollTime+=amount;
    }


}
