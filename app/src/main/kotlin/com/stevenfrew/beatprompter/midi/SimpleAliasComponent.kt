package com.stevenfrew.beatprompter.midi

/**
 * A simple sequence of MIDI bytes.
 */
class SimpleAliasComponent(private val mValues: List<Value>) : AliasComponent {

    @Throws(ResolutionException::class)
    override fun resolve(aliases: List<Alias>, parameters: ByteArray, channel: Byte): List<OutgoingMessage> {
        return listOf(OutgoingMessage(mValues.map{it.resolve(parameters,channel)}.toByteArray()))
    }
}
