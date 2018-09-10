package com.stevenfrew.beatprompter.midi.alias

import com.stevenfrew.beatprompter.comm.midi.message.OutgoingMessage

class Alias constructor(name:String,components:List<AliasComponent>)
{
    val mName=name
    private val mComponents=components

    @Throws(ResolutionException::class)
    fun resolve(aliases: List<Alias>, arguments: ByteArray, channel: Byte): List<OutgoingMessage> {
        return mComponents.flatMap{it.resolve(aliases, arguments, channel)}
    }
}