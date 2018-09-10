package com.stevenfrew.beatprompter.comm.bluetooth.message

import android.graphics.Rect
import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.songload.SongChoiceInfo
import com.stevenfrew.beatprompter.comm.OutgoingMessage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * OutgoingMessage that is sent/received when a song is chosen.
 */
class ChooseSongMessage constructor(val bytes:ByteArray,val mChoiceInfo: SongChoiceInfo): OutgoingMessage(bytes) {
    constructor(choiceInfo: SongChoiceInfo):this(asBytes(choiceInfo),choiceInfo)
    companion object {
        internal const val CHOOSE_SONG_MESSAGE_ID: Byte = 0

        private fun asBytes(choiceInfo: SongChoiceInfo): ByteArray {
            return ByteArrayOutputStream().apply {
                write(byteArrayOf(CHOOSE_SONG_MESSAGE_ID), 0, 1)
                ObjectOutputStream(this).apply {
                    writeObject(choiceInfo.mNormalizedTitle)
                    writeObject(choiceInfo.mNormalizedArtist)
                    writeObject(choiceInfo.mTrack)
                    writeBoolean(choiceInfo.mBeatScroll)
                    writeBoolean(choiceInfo.mSmoothScroll)
                    writeInt(choiceInfo.mOrientation)
                    writeFloat(choiceInfo.mMinFontSize)
                    writeFloat(choiceInfo.mMaxFontSize)
                    writeInt(choiceInfo.mScreenSize.width())
                    writeInt(choiceInfo.mScreenSize.height())
                    flush()
                    close()
                }
            }.toByteArray()
        }

        @Throws(NotEnoughDataException::class)
        internal fun fromBytes(bytes: ByteArray): ChooseSongMessage {
            try {
                ByteArrayInputStream(bytes).apply {
                    val dataRead = read(ByteArray(1))
                    if (dataRead == 1) {
                        val availableStart = available()
                        ObjectInputStream(this).apply {
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
                            val availableEnd = available()
                            val messageLength = 1 + (availableStart - availableEnd)
                            close()
                            return ChooseSongMessage(bytes.copyOfRange(0,messageLength), SongChoiceInfo(title, artist, track, orientation, beatScroll, smoothScroll, minFontSize, maxFontSize, Rect(0, 0, screenWidth, screenHeight)))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(BeatPrompterApplication.TAG, "Couldn't read ChooseSongMessage data, assuming not enough data", e)
            }
            throw NotEnoughDataException()
        }
    }
}
