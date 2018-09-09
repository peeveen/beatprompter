package com.stevenfrew.beatprompter.midi.alias

import com.stevenfrew.beatprompter.comm.midi.message.outgoing.OutgoingMessage

interface AliasComponent {
    @Throws(ResolutionException::class)
    fun resolve(aliases: List<Alias>, parameters: ByteArray, channel: Byte): List<OutgoingMessage>
}