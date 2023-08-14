package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.CachedFile
import com.stevenfrew.beatprompter.cache.MIDIAliasFile
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException
import com.stevenfrew.beatprompter.cache.parse.tag.TagParsingUtility
import com.stevenfrew.beatprompter.cache.parse.tag.find.DirectiveFinder
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MIDIAliasInstructionTag
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MIDIAliasNameTag
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MIDIAliasSetNameTag
import com.stevenfrew.beatprompter.midi.alias.Alias
import com.stevenfrew.beatprompter.midi.alias.AliasComponent
import com.stevenfrew.beatprompter.midi.alias.AliasSet
import com.stevenfrew.beatprompter.midi.alias.ChannelValue
import com.stevenfrew.beatprompter.midi.alias.RecursiveAliasComponent
import com.stevenfrew.beatprompter.midi.alias.SimpleAliasComponent
import com.stevenfrew.beatprompter.midi.alias.Value
import com.stevenfrew.beatprompter.util.splitAndTrim

@ParseTags(MIDIAliasSetNameTag::class, MIDIAliasNameTag::class, MIDIAliasInstructionTag::class)
/**
 * Parser for MIDI alias files.
 */
class MIDIAliasFileParser(cachedCloudFile: CachedFile) :
	TextFileParser<MIDIAliasFile>(cachedCloudFile, false, DirectiveFinder) {

	private var mAliasSetName: String? = null
	private var mCurrentAliasName: String? = null
	private var mCurrentAliasComponents = mutableListOf<AliasComponent>()
	private var mAliases = mutableListOf<Alias>()

	override fun parseLine(line: TextFileLine<MIDIAliasFile>) {
		line.mTags.asSequence().apply {
			filterIsInstance<MIDIAliasSetNameTag>()
				.firstOrNull()
				?.also {
					if (mAliasSetName != null)
						mErrors.add(FileParseError(it, R.string.midi_alias_set_name_defined_multiple_times))
					else
						mAliasSetName = it.mAliasSetName
				}

			filterIsInstance<MIDIAliasNameTag>()
				.firstOrNull()
				?.also { startNewAlias(it) }

			filterIsInstance<MIDIAliasInstructionTag>()
				.firstOrNull()
				?.also { addInstructionToCurrentAlias(it) }
		}
	}

	override fun getResult(): MIDIAliasFile {
		return MIDIAliasFile(mCachedCloudFile, getAliasSet(), mErrors)
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
		val name = tag.mName
		val componentArgs = mutableListOf<Value>()
		val paramBits = tag.mInstructions.splitAndTrim(",")
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

	private fun getAliasSet(): AliasSet {
		finishCurrentAlias()
		if (mAliasSetName == null)
			throw InvalidBeatPrompterFileException(
				R.string.not_a_valid_midi_alias_file,
				mCachedCloudFile.mName
			)
		return AliasSet(mAliasSetName!!, mAliases)
	}
}