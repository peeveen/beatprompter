package com.stevenfrew.beatprompter;

public enum PlayState {
    AtTitleScreen(0),
    Paused(1),
    Playing(2);

    private int mValue;

    PlayState(int val)
    {
        mValue=val;
    }

    public int asValue()
    {
        return mValue;
    }

    public static PlayState fromValue(int val)
    {
        switch(val)
        {
            case 0:
                return AtTitleScreen;
            case 1:
                return Paused;
            default:
                return Playing;
        }
    }

    static PlayState increase(PlayState state)
    {
        if(state==AtTitleScreen)
            return Paused;
        return Playing;
    }

    static PlayState reduce(PlayState state)
    {
        if(state==Playing)
            return Paused;
        return AtTitleScreen;
    }
}
