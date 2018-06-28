package com.stevenfrew.beatprompter.midi;

import java.util.List;

public class RecursiveAliasComponent implements AliasComponent {
    private int mHighestArgumentReference=0;
    private String mReferencedAliasName;
    private List<Value> mArguments;

    public RecursiveAliasComponent(String referencedAliasName,List<Value> arguments)
    {
        mReferencedAliasName=referencedAliasName;
        mArguments=arguments;
        for(Value v:arguments)
            if(v instanceof ArgumentValue)
                mHighestArgumentReference=Math.max(((ArgumentValue) v).mArgumentIndex,mHighestArgumentReference);
    }

    @Override
    public int getHighestArgumentReference() {
        return mHighestArgumentReference;
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
        throw new ResolutionException();
    }
}
