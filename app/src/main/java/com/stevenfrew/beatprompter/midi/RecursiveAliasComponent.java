package com.stevenfrew.beatprompter.midi;

import com.stevenfrew.beatprompter.BeatPrompterApplication;
import com.stevenfrew.beatprompter.R;

import java.util.List;

public class RecursiveAliasComponent implements AliasComponent {
    private String mReferencedAliasName;
    private List<Value> mArguments;

    public RecursiveAliasComponent(String referencedAliasName,List<Value> arguments)
    {
        mReferencedAliasName=referencedAliasName;
        mArguments=arguments;
    }

    @Override
    public List<OutgoingMessage> resolve(List<Alias> aliases, byte[] parameters, byte channel) throws ResolutionException {
        for(Alias alias:aliases)
            if(alias.mName.equalsIgnoreCase(mReferencedAliasName))
            {
                byte[] newArgs=new byte[mArguments.size()];
                int counter=0;
                for(Value arg:mArguments)
                    newArgs[counter++]=arg.resolve(parameters,channel);
                return alias.resolve(aliases,newArgs,channel);
            }
        throw new ResolutionException(BeatPrompterApplication.getResourceString(R.string.unknown_midi_directive,mReferencedAliasName));
    }
}
