package com.stevenfrew.beatprompter.comm.bluetooth.message

import android.graphics.Rect
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.song.load.SongChoiceInfo
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * OutgoingMessage that is sent/received when a song is chosen.
 */
class ChooseSongMessage(
	val bytes: ByteArray,
	val mChoiceInfo: SongChoiceInfo
) : BluetoothMessage(bytes) {
	constructor(choiceInfo: SongChoiceInfo) : this(asBytes(choiceInfo), choiceInfo)

	companion object {
		private fun asBytes(choiceInfo: SongChoiceInfo): ByteArray =
			ByteArrayOutputStream().apply {
				write(byteArrayOf(CHOOSE_SONG_MESSAGE_ID), 0, 1)
				ObjectOutputStream(this).apply {
					writeObject(choiceInfo.mNormalizedTitle)
					writeObject(choiceInfo.mNormalizedArtist)
					writeObject(choiceInfo.mVariation)
					writeBoolean(choiceInfo.mBeatScroll)
					writeBoolean(choiceInfo.mSmoothScroll)
					writeInt(choiceInfo.mOrientation)
					writeFloat(choiceInfo.mMinFontSize)
					writeFloat(choiceInfo.mMaxFontSize)
					writeInt(choiceInfo.mScreenSize.width())
					writeInt(choiceInfo.mScreenSize.height())
					writeBoolean(choiceInfo.mNoAudio)
					writeInt(choiceInfo.mAudioLatency)
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
								val track = readObject() as String
								val beatScroll = readBoolean()
								val smoothScroll = readBoolean()
								val orientation = readInt()
								val minFontSize = readFloat()
								val maxFontSize = readFloat()
								val screenWidth = readInt()
								val screenHeight = readInt()
								val noAudio = readBoolean()
								val audioLatency = readInt()
								SongChoiceInfo(
									title,
									artist,
									track,
									orientation,
									beatScroll,
									smoothScroll,
									minFontSize,
									maxFontSize,
									Rect(0, 0, screenWidth, screenHeight),
									noAudio,
									audioLatency
								)
							}
						val availableEnd = available()
						val messageLength = 1 + (availableStart - availableEnd)
						close()
						Logger.logLoader { "Received Bluetooth request to load \"${songChoiceInfo.mNormalizedTitle}\"" }
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
