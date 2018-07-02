package com.stevenfrew.beatprompter.midi;

import java.util.List;

public interface AliasComponent {
    List<OutgoingMessage> resolve(List<Alias> aliases,byte[] arguments, byte channel) throws ResolutionException;
}
