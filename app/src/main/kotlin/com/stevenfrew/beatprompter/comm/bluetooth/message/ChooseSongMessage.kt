package com.stevenfrew.beatprompter.comm.bluetooth.message

import android.graphics.Rect
import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * OutgoingMessage that is sent/received when a song is chosen.
 */
class ChooseSongMessage constructor(val bytes:ByteArray,val mNormalizedTitle: String, val mNormalizedArtist:String, val mTrack: String, val mOrientation: Int, val mBeatScroll: Boolean, val mSmoothScroll: Boolean, val mMinFontSize: Float, val mMaxFontSize: Float, val mScreenSize: Rect): Message(bytes) {
    constructor(normalizedTitle: String, normalizedArtist:String, track: String, orientation: Int, beatScroll: Boolean, smoothScroll: Boolean, minFontSize: Float, maxFontSize: Float, screenSize: Rect):this(asBytes(normalizedTitle,normalizedArtist,track,orientation,beatScroll,smoothScroll,minFontSize,maxFontSize,screenSize),normalizedTitle,normalizedTitle,track,orientation,beatScroll,smoothScroll,minFontSize,maxFontSize,screenSize)
    companion object {
        internal const val CHOOSE_SONG_MESSAGE_ID: Byte = 0

        private fun asBytes(normalizedTitle: String, normalizedArtist:String, track: String, orientation: Int, beatScroll: Boolean, smoothScroll: Boolean, minFontSize: Float, maxFontSize: Float, screenSize: Rect): ByteArray {
            return ByteArrayOutputStream().apply {
                write(byteArrayOf(CHOOSE_SONG_MESSAGE_ID), 0, 1)
                ObjectOutputStream(this).apply {
                    writeObject(normalizedTitle)
                    writeObject(normalizedArtist)
                    writeObject(track)
                    writeBoolean(beatScroll)
                    writeBoolean(smoothScroll)
                    writeInt(orientation)
                    writeFloat(minFontSize)
                    writeFloat(maxFontSize)
                    writeInt(screenSize.width())
                    writeInt(screenSize.height())
                    flush()
                    close()
                }
            }.toByteArray()
        }

        @Throws(NotEnoughDataException::class)
        internal fun fromBytes(bytes: ByteArray): IncomingMessage {
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
                            return IncomingMessage(ChooseSongMessage(bytes.copyOfRange(0,messageLength),title, artist, track, orientation, beatScroll, smoothScroll, minFontSize, maxFontSize, Rect(0, 0, screenWidth, screenHeight)), messageLength)
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
