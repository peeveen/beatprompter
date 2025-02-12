package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.CachedFile
import com.stevenfrew.beatprompter.cache.MIDIAliasFile
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException
import com.stevenfrew.beatprompter.cache.parse.tag.TagParsingUtility
import com.stevenfrew.beatprompter.cache.parse.tag.find.DirectiveFinder
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MidiAliasChannelTag
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MidiAliasInstructionTag
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MidiAliasNameTag
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MidiAliasSetNameTag
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.WithMidi
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.WithMidiContinueTag
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.WithMidiStartTag
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.WithMidiStopTag
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.WithMidiTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.MidiEventTag
import com.stevenfrew.beatprompter.midi.alias.Alias
import com.stevenfrew.beatprompter.midi.alias.AliasComponent
import com.stevenfrew.beatprompter.midi.alias.AliasSet
import com.stevenfrew.beatprompter.midi.alias.ChannelValue
import com.stevenfrew.beatprompter.midi.alias.RecursiveAliasComponent
import com.stevenfrew.beatprompter.midi.alias.SimpleAliasComponent
import com.stevenfrew.beatprompter.midi.alias.Value
import com.stevenfrew.beatprompter.util.splitAndTrim

@ParseTags(
	MidiAliasSetNameTag::class,
	MidiAliasNameTag::class,
	MidiAliasInstructionTag::class,
	WithMidiStartTag::class,
	WithMidiContinueTag::class,
	WithMidiStopTag::class,
	MidiAliasChannelTag::class
)
/**
 * Parser for MIDI alias files.
 */
class MidiAliasFileParser(cachedCloudFile: CachedFile) :
	TextFileParser<MIDIAliasFile>(cachedCloudFile, false, false, false, DirectiveFinder) {

	private var aliasSetName: String? = null
	private var currentAliasName: String? = null
	private var defaultChannel: ChannelValue? = null
	private var currentAliasComponents = mutableListOf<AliasComponent>()
	private var aliases = mutableListOf<Alias>()
	private var withMidiStart = false
	private var withMidiContinue = false
	private var withMidiStop = false

	private val withMidiSet
		get() = withMidiStart || withMidiContinue || withMidiStop

	override fun parseLine(line: TextFileLine<MIDIAliasFile>): Boolean {
		line.tags.asSequence().apply {
			filterIsInstance<MidiAliasChannelTag>()
				.firstOrNull()
				?.also {
					defaultChannel = ChannelValue(it.channel.toByte())
				}

			filterIsInstance<MidiAliasSetNameTag>()
				.firstOrNull()
				?.also {
					if (aliasSetName != null)
						addError(FileParseError(it, R.string.midi_alias_set_name_defined_multiple_times))
					else
						aliasSetName = it.aliasSetName
				}

			filterIsInstance<MidiAliasNameTag>()
				.firstOrNull()
				?.also { startNewAlias(it) }

			filterIsInstance<MidiAliasInstructionTag>()
				.firstOrNull()
				?.also { addInstructionToCurrentAlias(it) }

			filterIsInstance<WithMidiTag>()
				.firstOrNull()
				?.also { flagCurrentAlias(it.with) }
		}
		return true
	}

	override fun getResult(): MIDIAliasFile = MIDIAliasFile(cachedCloudFile, getAliasSet(), errors)

	private fun startNewAlias(aliasNameTag: MidiAliasNameTag) {
		if (aliasSetName.isNullOrBlank())
			addError(FileParseError(aliasNameTag, R.string.no_midi_alias_set_name_defined))
		else if (currentAliasName == null)
			currentAliasName = aliasNameTag.aliasName
		else {
			finishCurrentAlias()
			currentAliasName = aliasNameTag.aliasName
			if (currentAliasName.isNullOrBlank()) {
				addError(FileParseError(aliasNameTag, R.string.midi_alias_without_a_name))
				currentAliasName = null
			}
		}
	}

	private fun addInstructionToCurrentAlias(instructionTag: MidiAliasInstructionTag) {
		if (aliasSetName.isNullOrBlank())
			addError(FileParseError(instructionTag, R.string.no_midi_alias_set_name_defined))
		else if (currentAliasName == null)
			addError(FileParseError(instructionTag, R.string.no_midi_alias_name_defined))
		else
			currentAliasComponents.add(createAliasComponent(instructionTag))
	}

	private fun flagCurrentAlias(with: WithMidi) =
		when (with) {
			WithMidi.Start -> withMidiStart = true
			WithMidi.Continue -> withMidiContinue = true
			WithMidi.Stop -> withMidiStop = true
		}

	private fun finishCurrentAlias() =
		currentAliasName?.also {
			if (currentAliasComponents.isNotEmpty()) {
				val hasArguments = currentAliasComponents.any { it.parameterCount > 0 }
				if (hasArguments && withMidiSet)
					addError(FileParseError(R.string.cannot_use_with_midi_with_parameters))
				aliases.add(
					Alias(
						it,
						currentAliasComponents,
						withMidiStart,
						withMidiContinue,
						withMidiStop
					)
				)
				currentAliasComponents = mutableListOf()
				withMidiStart = false
				withMidiContinue = false
				withMidiStop = false
			} else
				addError(FileParseError(R.string.midi_alias_has_no_components, it))
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
				addError(FileParseError(tag, mte))
			}
		}
		val channelArgs = componentArgs.filterIsInstance<ChannelValue>()
		val channelArg = when (channelArgs.size) {
			0 -> defaultChannel
			1 -> channelArgs.first().also {
				if (componentArgs.last() != it)
					addError(FileParseError(tag, R.string.channel_must_be_last_parameter))
				componentArgs.remove(it)
			}

			else -> {
				addError(FileParseError(tag, R.string.multiple_channel_args))
				defaultChannel
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