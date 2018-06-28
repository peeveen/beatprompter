package com.stevenfrew.beatprompter.midi;

import com.stevenfrew.beatprompter.R;
import com.stevenfrew.beatprompter.SongList;

import java.util.ArrayList;
import java.util.List;

public class Alias {
    public String mName;
    private int mParamCount;
    private List<AliasComponent> mComponents;

    public Alias(String name)
    {
        mParamCount=0;
        mName=name;
        mComponents=new ArrayList<>();
    }
    public Alias(String name,List<AliasComponent> components)
    {
        this(name);
        for(AliasComponent component:components)
            mParamCount = Math.max(mParamCount, component.getHighestArgumentReference());
        mComponents=components;
    }

    public List<OutgoingMessage> resolve(List<Alias> aliases,byte[] arguments, byte channel) throws ResolutionException
    {
        if(arguments.length<mParamCount)
            //SongList.mSongListInstance.getString(R.string.not_enough_parameters_supplied)
            throw new ResolutionException();
        ArrayList<OutgoingMessage> outMessages=new ArrayList<>();
        for(AliasComponent component:mComponents)
            outMessages.addAll(component.resolve(aliases,arguments,channel));
        return outMessages;
    }
}
