package com.stevenfrew.beatprompter.midi;

import java.util.List;

/*
 Represents a set of aliases, read from an alias file.
 */
public class AliasSet
{
    // The name of this set.
    public String mName;
    // The aliases that are contained in this set.
    public List<Alias> mAliases;

    public AliasSet(String aliasSetName, List<Alias> aliases) {
        mName=aliasSetName;
        mAliases=aliases;
    }
}
