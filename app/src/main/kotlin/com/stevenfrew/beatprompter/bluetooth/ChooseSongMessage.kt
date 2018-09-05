package com.stevenfrew.beatprompter.bluetooth

import android.graphics.Rect
import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * Message that is sent/received when a song is chosen.
 */
class ChooseSongMessage constructor(val mNormalizedTitle: String, val mNormalizedArtist:String, val mTrack: String, val mOrientation: Int, val mBeatScroll: Boolean, val mSmoothScroll: Boolean, val mMinFontSize: Float, val mMaxFontSize: Float, val mScreenSize: Rect): BluetoothMessage() {
    override val bytes: ByteArray
        get() {
            return ByteArrayOutputStream().apply {
                write(byteArrayOf(CHOOSE_SONG_MESSAGE_ID), 0, 1)
                ObjectOutputStream(this).apply {
                    writeObject(mNormalizedTitle)
                    writeObject(mNormalizedArtist)
                    writeObject(mTrack)
                    writeBoolean(mBeatScroll)
                    writeBoolean(mSmoothScroll)
                    writeInt(mOrientation)
                    writeFloat(mMinFontSize)
                    writeFloat(mMaxFontSize)
                    writeInt(mScreenSize.width())
                    writeInt(mScreenSize.height())
                    flush()
                    close()
                }
            }.toByteArray()
        }

    companion object {
        internal const val CHOOSE_SONG_MESSAGE_ID: Byte = 0

        @Throws(NotEnoughBluetoothDataException::class)
        internal fun fromBytes(bytes: ByteArray):IncomingBluetoothMessage {
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
                            return IncomingBluetoothMessage(ChooseSongMessage(title,artist,track,orientation,beatScroll,smoothScroll,minFontSize,maxFontSize,Rect(0,0,screenWidth,screenHeight)),messageLength)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(BeatPrompterApplication.TAG, "Couldn't read ChooseSongMessage data, assuming not enough data", e)
            }
            throw NotEnoughBluetoothDataException()
        }
    }
}
