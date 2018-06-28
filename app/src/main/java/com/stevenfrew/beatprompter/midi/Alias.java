package com.stevenfrew.beatprompter.midi;

import java.util.ArrayList;
import java.util.List;

public class Alias {
    public String mName;
    private List<AliasComponent> mComponents;

    public Alias(String name)
    {
        mName=name;
        mComponents=new ArrayList<>();
    }
    public Alias(String name,List<AliasComponent> components)
    {
        this(name);
        mComponents=components;
    }

    public List<OutgoingMessage> resolve(List<Alias> aliases,byte[] arguments, byte channel) throws ResolutionException
    {
        ArrayList<OutgoingMessage> outMessages=new ArrayList<>();
        for(AliasComponent component:mComponents)
            outMessages.addAll(component.resolve(aliases,arguments,channel));
        return outMessages;
    }
}
