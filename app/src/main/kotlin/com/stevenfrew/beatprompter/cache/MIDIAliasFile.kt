package com.stevenfrew.beatprompter.cache

import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.FileLine
import com.stevenfrew.beatprompter.cache.parse.FileParseError
import com.stevenfrew.beatprompter.cache.parse.InvalidBeatPrompterFileException
import com.stevenfrew.beatprompter.cloud.SuccessfulCloudDownloadResult
import com.stevenfrew.beatprompter.midi.*
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.*
import java.util.ArrayList

class MIDIAliasFile : CachedCloudFile {

    var mAliasSet: AliasSet
    // The errors that were found in the file.
    var mErrors = ArrayList<FileParseError>()

    @Throws(InvalidBeatPrompterFileException::class)
    internal constructor(result: SuccessfulCloudDownloadResult) : super(result.mDownloadedFile, result.mCloudFileInfo) {
        mAliasSet = readAliasFile(mFile, mID, mErrors)
    }

    @Throws(InvalidBeatPrompterFileException::class)
    internal constructor(element: Element) : super(element) {
        mAliasSet = readAliasFile(mFile, mID, mErrors)
    }

    override fun writeToXML(d: Document, element: Element) {
        val aliasFileElement = d.createElement(MIDIALIASFILE_ELEMENT_TAG_NAME)
        super.writeToXML(aliasFileElement)
        element.appendChild(aliasFileElement)
    }

    companion object {
        const val MIDIALIASFILE_ELEMENT_TAG_NAME = "midialiases"

        @Throws(InvalidBeatPrompterFileException::class)
        private fun readAliasFile(file: File, storageName: String, midiParsingErrors: MutableList<FileParseError>): AliasSet {
            try {
                return readAliasFile(BufferedReader(InputStreamReader(FileInputStream(file))), storageName, midiParsingErrors)
            } catch (ioe: IOException) {
                throw InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.not_a_valid_midi_alias_file, storageName))
            }
        }

        @Throws(InvalidBeatPrompterFileException::class)
        private fun readAliasFile(br: BufferedReader, filename: String, midiParsingErrors: MutableList<FileParseError>): AliasSet {
            try {
                var line: String?
                var lineNumber = 0
                var currentAliasName: String? = null
                var currentAliasComponents: MutableList<AliasComponent> = ArrayList()
                var aliasFilename: String? = null
                var isMidiAliasFile = false
                val aliases = ArrayList<Alias>()
                do {
                    line = br.readLine()
                    if(line!=null) {
                        val fileLine= FileLine(line, ++lineNumber, midiParsingErrors)
                        if(fileLine.isComment)
                            continue

                        if (fileLine.isEmpty) {
                            if (currentAliasName != null) {
                                aliases.add(Alias(currentAliasName, currentAliasComponents))
                                currentAliasName = null
                                currentAliasComponents = ArrayList()
                            }
                        }
                        if (!isMidiAliasFile) {
                            aliasFilename = fileLine.getTokenValue("midi_aliases")
                            if (aliasFilename == null || aliasFilename.trim().isEmpty()) {
                                throw InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.not_a_valid_midi_alias_file, filename))
                            }
                            isMidiAliasFile = true
                        } else {
                            // OK, we have a line of content, and we're definitely in a MIDI alias file.
                            if (currentAliasName != null) {
                                val component = parseAliasComponent(line, lineNumber, midiParsingErrors)
                                if (component != null)
                                    currentAliasComponents.add(component)
                            } else {
                                currentAliasName = fileLine.getTokenValue("midi_alias")
                                if (currentAliasName != null) {
                                    if (currentAliasName.contains(":"))
                                        midiParsingErrors.add(FileParseError(lineNumber, BeatPrompterApplication.getResourceString(R.string.midi_alias_name_contains_more_than_two_parts)))
                                    if (currentAliasName.isEmpty()) {
                                        midiParsingErrors.add(FileParseError(lineNumber, BeatPrompterApplication.getResourceString(R.string.midi_alias_without_a_name)))
                                        currentAliasName = null
                                    }
                                }
                            }
                        }
                    }
                } while(line!=null)
                if (currentAliasName != null)
                    aliases.add(Alias(currentAliasName, currentAliasComponents))
                return if (aliasFilename != null)
                    AliasSet(aliasFilename, aliases)
                else
                    throw InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.not_a_valid_midi_alias_file, filename))
            } catch (ioe: IOException) {
                throw InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.not_a_valid_midi_alias_file, filename))
            } finally {
                try {
                    br.close()
                } catch (ioe: IOException) {
                    Log.e(BeatPrompterApplication.TAG, "Failed to close set list file", ioe)
                }
            }
        }

        private fun parseAliasComponent(line: String, lineNumber: Int, errors: MutableList<FileParseError>): AliasComponent? {
            val bracketStart = line.indexOf("{")
            if (bracketStart != -1) {
                val bracketEnd = line.indexOf("}", bracketStart)
                if (bracketEnd != -1) {
                    val contents = line.substring(bracketStart + 1, bracketEnd - bracketStart).trim().toLowerCase()
                    if (contents.isNotEmpty()) {
                        val bits = contents.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val componentArgs = ArrayList<Value>()
                        val tagName = bits[0].trim()
                        if (bits.size > 2)
                            errors.add(FileParseError(lineNumber, BeatPrompterApplication.getResourceString(R.string.midi_alias_message_contains_more_than_two_parts)))
                        else if (bits.size > 1) {
                            val params = bits[1].trim()
                            val paramBits = params.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                            for ((paramCounter, paramBit) in paramBits.withIndex()) {
                                val aliasValue = Value.parseValue(paramBit, lineNumber, paramCounter, paramBits.size, errors)
                                componentArgs.add(aliasValue)
                            }
                        }

                        return if (tagName.equals("midi_send", ignoreCase = true))
                            SimpleAliasComponent(componentArgs)
                        else
                            RecursiveAliasComponent(tagName, componentArgs)
                    } else
                        errors.add(FileParseError(lineNumber, BeatPrompterApplication.getResourceString(R.string.empty_tag)))
                } else
                    errors.add(FileParseError(lineNumber, BeatPrompterApplication.getResourceString(R.string.badly_formed_tag)))
            } else
                errors.add(FileParseError(lineNumber, BeatPrompterApplication.getResourceString(R.string.badly_formed_tag)))
            return null
        }
    }
}
