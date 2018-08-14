package com.stevenfrew.beatprompter.midi

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R

class RecursiveAliasComponent(private val mReferencedAliasName: String, private val mArguments: List<Value>) : AliasComponent {

    @Throws(ResolutionException::class)
    override fun resolve(aliases: List<Alias>, parameters: ByteArray, channel: Byte): List<OutgoingMessage> {
        try {
            return aliases.first{it.mName.equals(mReferencedAliasName, ignoreCase = true)}.resolve(aliases,mArguments.map{it.resolve(parameters,channel)}.toByteArray(),channel)
        }
        catch(nsee:NoSuchElementException) {
            throw ResolutionException(BeatPrompterApplication.getResourceString(R.string.unknown_midi_directive, mReferencedAliasName))
        }
    }
}
