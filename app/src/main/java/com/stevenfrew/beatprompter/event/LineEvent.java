package com.stevenfrew.beatprompter.event;

import com.stevenfrew.beatprompter.Line;

public class LineEvent extends BaseEvent
{
    public Line mLine;
    public long mDuration;

    public LineEvent(long eventTime,long duration)
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
