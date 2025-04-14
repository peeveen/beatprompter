package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.CachedFile
import com.stevenfrew.beatprompter.cache.MidiAliasFile
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException
import com.stevenfrew.beatprompter.cache.parse.tag.TagParsingUtility
import com.stevenfrew.beatprompter.cache.parse.tag.find.DirectiveFinder
import com.stevenfrew.beatprompter.cache.parse.tag.midi.alias.MidiAliasChannelTag
import com.stevenfrew.beatprompter.cache.parse.tag.midi.alias.MidiAliasInstructionTag
import com.stevenfrew.beatprompter.cache.parse.tag.midi.alias.MidiAliasNameTag
import com.stevenfrew.beatprompter.cache.parse.tag.midi.alias.MidiAliasSetNameTag
import com.stevenfrew.beatprompter.cache.parse.tag.midi.alias.MidiInitTag
import com.stevenfrew.beatprompter.cache.parse.tag.midi.alias.WithMidi
import com.stevenfrew.beatprompter.cache.parse.tag.midi.alias.WithMidiContinueTag
import com.stevenfrew.beatprompter.cache.parse.tag.midi.alias.WithMidiStartTag
import com.stevenfrew.beatprompter.cache.parse.tag.midi.alias.WithMidiStopTag
import com.stevenfrew.beatprompter.cache.parse.tag.midi.alias.WithMidiTag
import com.stevenfrew.beatprompter.cache.parse.tag.midi.trigger.MidiControlChangeTriggerTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.MidiEventTag
import com.stevenfrew.beatprompter.midi.MidiTrigger
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
	MidiAliasChannelTag::class,
	MidiInitTag::class,
	MidiControlChangeTriggerTag::class
)
/**
 * Parser for MIDI alias files.
 */
class MidiAliasFileParser(private val cachedCloudFile: CachedFile) :
	TextContentParser<MidiAliasFile>(cachedCloudFile, false, false, false, DirectiveFinder) {

	private var aliasSetName: String? = null
	private val currentAliasComponents = mutableListOf<AliasComponent>()
	private val triggers = mutableListOf<MidiTrigger>()
	private var commandName: String? = null
	private var useByDefault: Boolean = true
	private var currentAliasName: String? = null
	private var defaultChannel: ChannelValue? = null
	private var aliases = mutableListOf<Alias>()
	private var withMidiStart = false
	private var withMidiContinue = false
	private var withMidiStop = false
	private var withSongLoad = false
	private var withSongLoadOrder = 0

	private val withMidiSet
		get() = withMidiStart || withMidiContinue || withMidiStop

	override fun parseLine(line: TextContentLine<MidiAliasFile>): Boolean {
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
						addError(ContentParsingError(it, R.string.midi_alias_set_name_defined_multiple_times))
					else {
						aliasSetName = it.aliasSetName
						useByDefault = it.useByDefault
					}
				}

			filterIsInstance<MidiAliasNameTag>()
				.firstOrNull()
				?.also { startNewAlias(it) }

			filterIsInstance<MidiControlChangeTriggerTag>()
				.firstOrNull()
				?.also { triggers.add(it.trigger) }

			filterIsInstance<MidiAliasInstructionTag>()
				.firstOrNull()
				?.also { addInstructionToCurrentAlias(it) }

			filterIsInstance<WithMidiTag>()
				.firstOrNull()
				?.also { flagCurrentAlias(it.with) }

			filterIsInstance<MidiInitTag>()
				.firstOrNull()
				?.also {
					flagCurrentAlias(WithMidi.SongLoad)
					withSongLoadOrder = it.order
				}
		}
		return true
	}

	override fun getResult(): MidiAliasFile = MidiAliasFile(cachedCloudFile, getAliasSet(), errors)

	private fun setNewAliasProperties(aliasNameTag: MidiAliasNameTag) {
		commandName = aliasNameTag.commandName
		currentAliasName = aliasNameTag.aliasName
		if (currentAliasName.isNullOrBlank()) {
			addError(ContentParsingError(aliasNameTag, R.string.midi_alias_without_a_name))
			currentAliasName = null
			commandName = null
		}
	}

	private fun startNewAlias(aliasNameTag: MidiAliasNameTag) {
		if (aliasSetName.isNullOrBlank())
			addError(ContentParsingError(aliasNameTag, R.string.no_midi_alias_set_name_defined))
		else if (currentAliasName == null)
			setNewAliasProperties(aliasNameTag)
		else {
			finishCurrentAlias()
			setNewAliasProperties(aliasNameTag)
		}
	}

	private fun addInstructionToCurrentAlias(instructionTag: MidiAliasInstructionTag) {
		if (aliasSetName.isNullOrBlank())
			addError(ContentParsingError(instructionTag, R.string.no_midi_alias_set_name_defined))
		else if (currentAliasName == null)
			addError(ContentParsingError(instructionTag, R.string.no_midi_alias_name_defined))
		else
			currentAliasComponents.add(createAliasComponent(instructionTag))
	}

	private fun flagCurrentAlias(with: WithMidi) =
		when (with) {
			WithMidi.Start -> withMidiStart = true
			WithMidi.Continue -> withMidiContinue = true
			WithMidi.Stop -> withMidiStop = true
			WithMidi.SongLoad -> withSongLoad = true
		}

	private fun finishCurrentAlias() =
		currentAliasName?.also {
			if (currentAliasComponents.isNotEmpty()) {
				val hasArguments = currentAliasComponents.any { component -> component.parameterCount > 0 }
				if (hasArguments && (withMidiSet || withSongLoad)) {
					addError(
						ContentParsingError(
							R.string.cannot_use_with_midi_with_parameters,
							if (withMidiSet) "with_midi_*" else "midi_init"
						)
					)
					withMidiStart = false
					withMidiContinue = false
					withMidiStop = false
					withSongLoad = false
				}
				if (hasArguments && commandName !== null) {
					addError(ContentParsingError(R.string.cannot_use_midi_command_with_parameters))
					commandName = null
				}
				if (triggers.any() && hasArguments) {
					addError(ContentParsingError(R.string.cannot_use_triggers_in_non_command_aliases))
					triggers.clear()
				}
				aliases.add(
					Alias(
						it,
						currentAliasComponents.toList(),
						triggers.toList(),
						withMidiStart,
						withMidiContinue,
						withMidiStop,
						withSongLoad,
						withSongLoadOrder,
						commandName,
					)
				)
				currentAliasComponents.clear()
				triggers.clear()
				withMidiStart = false
				withMidiContinue = false
				withMidiStop = false
				withSongLoad = false
			} else
				addError(ContentParsingError(R.string.midi_alias_has_no_components, it))
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
				addError(ContentParsingError(tag, mte))
			}
		}
		val channelArgs = componentArgs.filterIsInstance<ChannelValue>()
		val channelArg = when (channelArgs.size) {
			0 -> defaultChannel
			1 -> channelArgs.first().also {
				if (componentArgs.last() != it)
					addError(ContentParsingError(tag, R.string.channel_must_be_last_parameter))
				componentArgs.remove(it)
			}

			else -> {
				addError(ContentParsingError(tag, R.string.multiple_channel_args))
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
			AliasSet(it, aliases, useByDefault)
		} ?: throw InvalidBeatPrompterFileException(
			R.string.not_a_valid_midi_alias_file,
			cachedCloudFile.name
		)
	}
}