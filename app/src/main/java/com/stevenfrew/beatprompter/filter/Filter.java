package com.stevenfrew.beatprompter.filter;

public abstract class Filter
{
    public boolean mCanSort;
    public String mName;

    protected Filter(String name, boolean canSort)
    {
        mCanSort=canSort;
        mName=name;
    }
}
