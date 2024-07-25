package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.CachedFile
import com.stevenfrew.beatprompter.cache.MIDIAliasFile
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException
import com.stevenfrew.beatprompter.cache.parse.tag.TagParsingUtility
import com.stevenfrew.beatprompter.cache.parse.tag.find.DirectiveFinder
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MidiAliasInstructionTag
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MidiAliasNameTag
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MidiAliasSetNameTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.MidiEventTag
import com.stevenfrew.beatprompter.midi.alias.Alias
import com.stevenfrew.beatprompter.midi.alias.AliasComponent
import com.stevenfrew.beatprompter.midi.alias.AliasSet
import com.stevenfrew.beatprompter.midi.alias.ChannelValue
import com.stevenfrew.beatprompter.midi.alias.RecursiveAliasComponent
import com.stevenfrew.beatprompter.midi.alias.SimpleAliasComponent
import com.stevenfrew.beatprompter.midi.alias.Value
import com.stevenfrew.beatprompter.util.splitAndTrim

@ParseTags(MidiAliasSetNameTag::class, MidiAliasNameTag::class, MidiAliasInstructionTag::class)
/**
 * Parser for MIDI alias files.
 */
class MidiAliasFileParser(cachedCloudFile: CachedFile) :
	TextFileParser<MIDIAliasFile>(cachedCloudFile, false, DirectiveFinder) {

	private var aliasSetName: String? = null
	private var currentAliasName: String? = null
	private var currentAliasComponents = mutableListOf<AliasComponent>()
	private var aliases = mutableListOf<Alias>()

	override fun parseLine(line: TextFileLine<MIDIAliasFile>) {
		line.tags.asSequence().apply {
			filterIsInstance<MidiAliasSetNameTag>()
				.firstOrNull()
				?.also {
					if (aliasSetName != null)
						errors.add(FileParseError(it, R.string.midi_alias_set_name_defined_multiple_times))
					else
						aliasSetName = it.aliasSetName
				}

			filterIsInstance<MidiAliasNameTag>()
				.firstOrNull()
				?.also { startNewAlias(it) }

			filterIsInstance<MidiAliasInstructionTag>()
				.firstOrNull()
				?.also { addInstructionToCurrentAlias(it) }
		}
	}

	override fun getResult(): MIDIAliasFile = MIDIAliasFile(cachedCloudFile, getAliasSet(), errors)

	private fun startNewAlias(aliasNameTag: MidiAliasNameTag) {
		if (aliasSetName.isNullOrBlank())
			errors.add(FileParseError(aliasNameTag, R.string.no_midi_alias_set_name_defined))
		else
			if (currentAliasName == null)
				currentAliasName = aliasNameTag.aliasName
			else
				if (currentAliasComponents.isNotEmpty()) {
					aliases.add(Alias(currentAliasName!!, currentAliasComponents))
					currentAliasComponents = mutableListOf()
					currentAliasName = aliasNameTag.aliasName
					if (currentAliasName.isNullOrBlank()) {
						errors.add(FileParseError(aliasNameTag, R.string.midi_alias_without_a_name))
						currentAliasName = null
					}
				} else
					errors.add(FileParseError(aliasNameTag, R.string.midi_alias_has_no_components))
	}

	private fun addInstructionToCurrentAlias(instructionTag: MidiAliasInstructionTag) {
		if (aliasSetName.isNullOrBlank())
			errors.add(FileParseError(instructionTag, R.string.no_midi_alias_set_name_defined))
		else {
			if (currentAliasName == null)
				errors.add(FileParseError(instructionTag, R.string.no_midi_alias_name_defined))
			else
				currentAliasComponents.add(createAliasComponent(instructionTag))
		}
	}

	private fun finishCurrentAlias() {
		if (currentAliasName != null && currentAliasComponents.isNotEmpty())
			aliases.add(Alias(currentAliasName!!, currentAliasComponents))
	}

	private fun createAliasComponent(tag: MidiAliasInstructionTag): AliasComponent {
		val name = tag.name
		val componentArgs = mutableListOf<Value>()
		val paramBits = tag.instructions.splitAndTrim(",")
		paramBits.forEachIndexed { paramCounter, paramBit ->
			try {
				val aliasValue = TagParsingUtility.parseMIDIValue(paramBit, paramCounter, paramBits.size)
				componentArgs.add(aliasValue)
			} catch (mte: MalformedTagException) {
				errors.add(FileParseError(tag, mte))
			}
		}
		val channelArgs = componentArgs.filterIsInstance<ChannelValue>()
		val channelArg = when (channelArgs.size) {
			0 -> null
			1 -> channelArgs.first().also {
				if (componentArgs.last() != it)
					errors.add(FileParseError(tag, R.string.channel_must_be_last_parameter))
				componentArgs.remove(it)
			}

			else -> {
				errors.add(FileParseError(tag, R.string.multiple_channel_args))
				null
			}
		}
		return if (name.equals(MidiEventTag.MIDI_SEND_TAG, ignoreCase = true))
			SimpleAliasComponent(componentArgs, channelArg)
		else
			RecursiveAliasComponent(name, componentArgs, channelArg)
	}

	private fun getAliasSet(): AliasSet {
		finishCurrentAlias()
		return aliasSetName?.let {
			AliasSet(it, aliases)
		} ?: throw InvalidBeatPrompterFileException(
			R.string.not_a_valid_midi_alias_file,
			cachedCloudFile.name
		)
	}
}