package com.stevenfrew.beatprompter.comm.bluetooth.message

import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.graphics.Rect
import com.stevenfrew.beatprompter.song.load.SongChoiceInfo
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * OutgoingMessage that is sent/received when a song is chosen.
 */
class ChooseSongMessage(
	bytes: ByteArray,
	val choiceInfo: SongChoiceInfo
) : BluetoothMessage(bytes) {
	constructor(choiceInfo: SongChoiceInfo) : this(asBytes(choiceInfo), choiceInfo)

	companion object {
		private fun asBytes(choiceInfo: SongChoiceInfo): ByteArray =
			ByteArrayOutputStream().apply {
				write(byteArrayOf(CHOOSE_SONG_MESSAGE_ID), 0, 1)
				ObjectOutputStream(this).apply {
					writeObject(choiceInfo.normalizedTitle)
					writeObject(choiceInfo.normalizedArtist)
					writeObject(choiceInfo.variation)
					writeBoolean(choiceInfo.isBeatScroll)
					writeBoolean(choiceInfo.isSmoothScroll)
					writeInt(choiceInfo.orientation)
					writeFloat(choiceInfo.minFontSize)
					writeFloat(choiceInfo.maxFontSize)
					writeInt(choiceInfo.screenSize.width)
					writeInt(choiceInfo.screenSize.height)
					writeBoolean(choiceInfo.noAudio)
					writeInt(choiceInfo.audioLatency)
					writeInt(choiceInfo.transposeShift)
					flush()
					close()
				}
			}.toByteArray()

		internal fun fromBytes(bytes: ByteArray): ChooseSongMessage {
			try {
				ByteArrayInputStream(bytes).apply {
					val dataRead = read(ByteArray(1))
					if (dataRead == 1) {
						val availableStart = available()
						val songChoiceInfo =
							ObjectInputStream(this).run {
								val title = readObject() as String
								val artist = readObject() as String
								val variation = readObject() as String
								val beatScroll = readBoolean()
								val smoothScroll = readBoolean()
								val orientation = readInt()
								val minFontSize = readFloat()
								val maxFontSize = readFloat()
								val screenWidth = readInt()
								val screenHeight = readInt()
								val noAudio = readBoolean()
								var audioLatency = 0
								var transposeShift = 0
								try {
									audioLatency = readInt()
									transposeShift = readInt()
								} catch (_: Exception) {
									// Old versions will not send these last two items of data.
									// Try to cope.
								}
								SongChoiceInfo(
									title,
									artist,
									variation,
									orientation,
									beatScroll,
									smoothScroll,
									minFontSize,
									maxFontSize,
									Rect(0, 0, screenWidth, screenHeight),
									noAudio,
									audioLatency,
									transposeShift
								)
							}
						val availableEnd = available()
						val messageLength = 1 + (availableStart - availableEnd)
						close()
						Logger.logLoader({ "Received Bluetooth request to load \"${songChoiceInfo.normalizedTitle}\"" })
						return ChooseSongMessage(bytes.copyOfRange(0, messageLength), songChoiceInfo)
					}
				}
			} catch (e: Exception) {
				Logger.logComms({ "Couldn't read ChooseSongMessage data, assuming not enough data" }, e)
			}
			throw NotEnoughDataException()
		}
	}
}
