package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.CachedCloudFile
import com.stevenfrew.beatprompter.cache.parse.tag.MIDITag
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MIDIAliasInstructionTag
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MIDIAliasNameTag
import com.stevenfrew.beatprompter.midi.*
import java.util.ArrayList

class MIDIAliasParsingState(sourceFile: CachedCloudFile):FileParsingState(sourceFile) {
    var mAliasSetName:String?=null
    private var mCurrentAliasName:String?=null
    private var mCurrentAliasComponents= mutableListOf<AliasComponent>()
    private var mAliases= mutableListOf<Alias>()

    fun startNewAlias(aliasNameTag:MIDIAliasNameTag)
    {
        if(mAliasSetName.isNullOrBlank())
            mErrors.add(FileParseError(aliasNameTag,BeatPrompterApplication.getResourceString(R.string.no_midi_alias_set_name_defined)))
        else {
            if (mCurrentAliasName == null)
                mCurrentAliasName = aliasNameTag.mAliasName
            else {
                if (mCurrentAliasComponents.isNotEmpty()) {
                    mAliases.add(Alias(mCurrentAliasName!!, mCurrentAliasComponents))
                    mCurrentAliasComponents.clear()
                    mCurrentAliasName = aliasNameTag.mAliasName
                } else
                    mErrors.add(FileParseError(aliasNameTag, BeatPrompterApplication.getResourceString(R.string.midi_alias_has_no_components)))
            }
        }
    }

    fun addInstructionToCurrentAlias(instructionTag:MIDIAliasInstructionTag)
    {
        if(mAliasSetName.isNullOrBlank())
            mErrors.add(FileParseError(instructionTag,BeatPrompterApplication.getResourceString(R.string.no_midi_alias_set_name_defined)))
        else {
            if (mCurrentAliasName == null)
                mErrors.add(FileParseError(instructionTag, BeatPrompterApplication.getResourceString(R.string.no_midi_alias_name_defined)))
            else
                mCurrentAliasComponents.add(createAliasComponent(instructionTag))
        }
    }

    fun finishCurrentAlias() {
        if (mCurrentAliasName != null && mCurrentAliasComponents.isNotEmpty())
            mAliases.add(Alias(mCurrentAliasName!!, mCurrentAliasComponents))
    }

    @Throws(MalformedTagException::class)
    fun createAliasComponent(tag: MIDIAliasInstructionTag):AliasComponent
    {
        val instructions=tag.mInstructions
        val name=tag.mName
        val componentArgs = ArrayList<Value>()
        val paramBits = instructions.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for ((paramCounter, paramBit) in paramBits.withIndex()) {
            val aliasValue = MIDITag.parseValue(paramBit, paramCounter, paramBits.size)
            componentArgs.add(aliasValue)
        }
        return if (name.equals("midi_send", ignoreCase = true))
            SimpleAliasComponent(componentArgs)
        else
            RecursiveAliasComponent(name, componentArgs)
    }

    @Throws(InvalidBeatPrompterFileException::class)
    fun getAliasSet():AliasSet
    {
        if(mAliasSetName!=null)
            return AliasSet(mAliasSetName!!, mAliases)
        else
            throw InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.not_a_valid_midi_alias_file, mSourceFile.mName))
    }

}