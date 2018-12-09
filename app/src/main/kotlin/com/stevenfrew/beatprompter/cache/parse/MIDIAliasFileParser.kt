package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.CachedFileDescriptor
import com.stevenfrew.beatprompter.cache.MIDIAliasFile
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException
import com.stevenfrew.beatprompter.cache.parse.tag.TagParsingUtility
import com.stevenfrew.beatprompter.cache.parse.tag.find.DirectiveFinder
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MIDIAliasInstructionTag
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MIDIAliasNameTag
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MIDIAliasSetNameTag
import com.stevenfrew.beatprompter.midi.alias.*
import com.stevenfrew.beatprompter.util.splitAndTrim

@ParseTags(MIDIAliasSetNameTag::class, MIDIAliasNameTag::class, MIDIAliasInstructionTag::class)
/**
 * Parser for MIDI alias files.
 */
class MIDIAliasFileParser constructor(cachedCloudFileDescriptor: CachedFileDescriptor)
    : TextFileParser<MIDIAliasFile>(cachedCloudFileDescriptor, false, DirectiveFinder) {

    private var mAliasSetName: String? = null
    private var mCurrentAliasName: String? = null
    private var mCurrentAliasComponents = mutableListOf<AliasComponent>()
    private var mAliases = mutableListOf<Alias>()

    override fun parseLine(line: TextFileLine<MIDIAliasFile>) {
        val tagSequence = line.mTags.asSequence()
        val midiAliasSetNameTag = tagSequence.filterIsInstance<MIDIAliasSetNameTag>().firstOrNull()
        if (midiAliasSetNameTag != null)
            if (mAliasSetName != null)
                mErrors.add(FileParseError(midiAliasSetNameTag, R.string.midi_alias_set_name_defined_multiple_times))
            else
                mAliasSetName = midiAliasSetNameTag.mAliasSetName

        val midiAliasNameTag = tagSequence.filterIsInstance<MIDIAliasNameTag>().firstOrNull()
        if (midiAliasNameTag != null)
            startNewAlias(midiAliasNameTag)

        val midiAliasInstructionTag = tagSequence.filterIsInstance<MIDIAliasInstructionTag>().firstOrNull()
        if (midiAliasInstructionTag != null)
            addInstructionToCurrentAlias(midiAliasInstructionTag)
    }

    override fun getResult(): MIDIAliasFile {
        return MIDIAliasFile(mCachedCloudFileDescriptor, getAliasSet(), mErrors)
    }

    private fun startNewAlias(aliasNameTag: MIDIAliasNameTag) {
        if (mAliasSetName.isNullOrBlank())
            mErrors.add(FileParseError(aliasNameTag, R.string.no_midi_alias_set_name_defined))
        else {
            if (mCurrentAliasName == null)
                mCurrentAliasName = aliasNameTag.mAliasName
            else {
                if (mCurrentAliasComponents.isNotEmpty()) {
                    mAliases.add(Alias(mCurrentAliasName!!, mCurrentAliasComponents))
                    mCurrentAliasComponents = mutableListOf()
                    mCurrentAliasName = aliasNameTag.mAliasName
                    if (mCurrentAliasName.isNullOrBlank()) {
                        mErrors.add(FileParseError(aliasNameTag, R.string.midi_alias_without_a_name))
                        mCurrentAliasName = null
                    }
                } else
                    mErrors.add(FileParseError(aliasNameTag, R.string.midi_alias_has_no_components))
            }
        }
    }

    private fun addInstructionToCurrentAlias(instructionTag: MIDIAliasInstructionTag) {
        if (mAliasSetName.isNullOrBlank())
            mErrors.add(FileParseError(instructionTag, R.string.no_midi_alias_set_name_defined))
        else {
            if (mCurrentAliasName == null)
                mErrors.add(FileParseError(instructionTag, R.string.no_midi_alias_name_defined))
            else
                mCurrentAliasComponents.add(createAliasComponent(instructionTag))
        }
    }

    private fun finishCurrentAlias() {
        if (mCurrentAliasName != null && mCurrentAliasComponents.isNotEmpty())
            mAliases.add(Alias(mCurrentAliasName!!, mCurrentAliasComponents))
    }

    private fun createAliasComponent(tag: MIDIAliasInstructionTag): AliasComponent {
        val instructions = tag.mInstructions
        val name = tag.mName
        val componentArgs = mutableListOf<Value>()
        val paramBits = instructions.splitAndTrim(",")
        for ((paramCounter, paramBit) in paramBits.withIndex()) {
            try {
                val aliasValue = TagParsingUtility.parseMIDIValue(paramBit, paramCounter, paramBits.size)
                componentArgs.add(aliasValue)
            } catch (mte: MalformedTagException) {
                mErrors.add(FileParseError(tag, mte))
            }
        }
        val channelArgs = componentArgs.filterIsInstance<ChannelValue>()
        val channelArg = when (channelArgs.size) {
            0 -> null
            1 -> channelArgs.first().also {
                if (componentArgs.last() != it)
                    mErrors.add(FileParseError(tag, R.string.channel_must_be_last_parameter))
                componentArgs.remove(it)
            }
            else -> {
                mErrors.add(FileParseError(tag, R.string.multiple_channel_args))
                null
            }
        }
        return if (name.equals("midi_send", ignoreCase = true))
            SimpleAliasComponent(componentArgs, channelArg)
        else
            RecursiveAliasComponent(name, componentArgs, channelArg)
    }

    @Throws(InvalidBeatPrompterFileException::class)
    private fun getAliasSet(): AliasSet {
        finishCurrentAlias()
        if (mAliasSetName == null)
            throw InvalidBeatPrompterFileException(R.string.not_a_valid_midi_alias_file, mCachedCloudFileDescriptor.mName)
        return AliasSet(mAliasSetName!!, mAliases)
    }
}