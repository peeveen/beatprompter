package com.stevenfrew.beatprompter.midi;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple sequence of MIDI bytes.
 */
public class SimpleAliasComponent implements AliasComponent {
    private List<Value> mValues;

    public SimpleAliasComponent(List<Value> values)
    {
        mValues=values;
    }

    @Override
    public int getHighestArgumentReference() {
        return 0;
    }

    @Override
    public List<OutgoingMessage> resolve(List<Alias> aliases, byte[] arguments, byte channel) throws ResolutionException {
        List<OutgoingMessage> messages=new ArrayList<>();
        byte[] componentBytes=new byte[mValues.size()];
        int counter=0;
        for(Value v:mValues)
            componentBytes[counter++]=v.resolve(arguments,channel);
        messages.add(new OutgoingMessage(componentBytes));
        return messages;
    }
}
