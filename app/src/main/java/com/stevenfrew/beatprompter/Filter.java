package com.stevenfrew.beatprompter;

abstract class Filter
{
    boolean mCanSort;
    String mName;

    Filter(String name, boolean canSort)
    {
        mCanSort=canSort;
        mName=name;
    }
}
