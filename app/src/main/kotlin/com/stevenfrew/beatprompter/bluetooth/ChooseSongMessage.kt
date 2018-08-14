package com.stevenfrew.beatprompter.bluetooth

import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class ChooseSongMessage constructor(title: String, track: String, orientation: Int, beatScroll: Boolean, smoothScroll: Boolean, minFontSize: Int, maxFontSize: Int, screenWidth: Int, screenHeight: Int): BluetoothMessage() {

    var mTitle=title
    var mTrack=track
    var mBeatScroll=beatScroll
    var mSmoothScroll=smoothScroll
    var mOrientation=orientation
    var mMinFontSize=minFontSize
    var mMaxFontSize=maxFontSize
    var mScreenWidth=screenWidth
    var mScreenHeight=screenHeight

    override val bytes: ByteArray
        get() {
            return ByteArrayOutputStream().apply {
                write(byteArrayOf(CHOOSE_SONG_MESSAGE_ID), 0, 1)
                ObjectOutputStream(this).apply {
                    writeObject(mTitle)
                    writeObject(mTrack)
                    writeBoolean(mBeatScroll)
                    writeBoolean(mSmoothScroll)
                    writeInt(mOrientation)
                    writeInt(mMinFontSize)
                    writeInt(mMaxFontSize)
                    writeInt(mScreenWidth)
                    writeInt(mScreenHeight)
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
                            val track = readObject() as String
                            val beatScroll = readBoolean()
                            val smoothScroll = readBoolean()
                            val orientation = readInt()
                            val minFontSize = readInt()
                            val maxFontSize = readInt()
                            val screenWidth = readInt()
                            val screenHeight = readInt()
                            val availableEnd = available()
                            val messageLength = 1 + (availableStart - availableEnd)
                            close()
                            return IncomingBluetoothMessage(ChooseSongMessage(title,track,orientation,beatScroll,smoothScroll,minFontSize,maxFontSize,screenWidth,screenHeight),messageLength)
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
