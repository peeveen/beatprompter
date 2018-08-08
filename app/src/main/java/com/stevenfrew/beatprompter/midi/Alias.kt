package com.stevenfrew.beatprompter.midi

class Alias constructor(name:String,components:List<AliasComponent>)
{
    @JvmField val mName=name;
    private val mComponents=components;

    @Throws(ResolutionException::class)
    fun resolve(aliases: List<Alias>, arguments: ByteArray, channel: Byte): List<OutgoingMessage> {
        return mComponents.flatMap{it.resolve(aliases, arguments, channel)}
    }
}