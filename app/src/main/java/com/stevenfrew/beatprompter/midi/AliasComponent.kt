package com.stevenfrew.beatprompter.midi

interface AliasComponent {
    @Throws(ResolutionException::class)
    fun resolve(aliases: List<Alias>, parameters: ByteArray, channel: Byte): List<OutgoingMessage>
}