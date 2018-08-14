package com.stevenfrew.beatprompter.bluetooth

import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class ChooseSongMessage : BluetoothMessage {

    var mTitle: String
    var mTrack: String
    var mBeatScroll: Boolean = false
    var mSmoothScroll: Boolean = false
    var mOrientation: Int = 0
    var mMinFontSize: Int = 0
    var mMaxFontSize: Int = 0
    var mScreenWidth: Int = 0
    var mScreenHeight: Int = 0

    override val bytes: ByteArray?
        get() {
            try {
                val byteArrayOutputStream = ByteArrayOutputStream()
                byteArrayOutputStream.write(byteArrayOf(CHOOSE_SONG_MESSAGE_ID), 0, 1)
                val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
                objectOutputStream.writeObject(mTitle)
                objectOutputStream.writeObject(mTrack)
                objectOutputStream.writeBoolean(mBeatScroll)
                objectOutputStream.writeBoolean(mSmoothScroll)
                objectOutputStream.writeInt(mOrientation)
                objectOutputStream.writeInt(mMinFontSize)
                objectOutputStream.writeInt(mMaxFontSize)
                objectOutputStream.writeInt(mScreenWidth)
                objectOutputStream.writeInt(mScreenHeight)
                objectOutputStream.flush()
                objectOutputStream.close()

                val byteArray = byteArrayOutputStream.toByteArray()
                byteArrayOutputStream.close()

                return byteArray
            } catch (e: Exception) {
                Log.e(BeatPrompterApplication.TAG, "Couldn't write ChooseSongMessage data.", e)
            }

            return null
        }

    constructor(title: String?, track: String?, orientation: Int, beatScroll: Boolean, smoothScroll: Boolean, minFontSize: Int, maxFontSize: Int, screenWidth: Int, screenHeight: Int) {
        mTitle = title ?: ""
        mTrack = track ?: ""
        mOrientation = orientation
        mBeatScroll = beatScroll
        mSmoothScroll = smoothScroll
        mMinFontSize = minFontSize
        mMaxFontSize = maxFontSize
        mScreenWidth = screenWidth
        mScreenHeight = screenHeight
    }

    @Throws(NotEnoughBluetoothDataException::class)
    internal constructor(bytes: ByteArray) {
        try {
            val byteArrayInputStream = ByteArrayInputStream(bytes)
            val dataRead = byteArrayInputStream.read(ByteArray(1))
            if (dataRead == 1) {
                val availableStart = byteArrayInputStream.available()
                val objectInputStream = ObjectInputStream(byteArrayInputStream)
                mTitle = objectInputStream.readObject() as String
                mTrack = objectInputStream.readObject() as String
                mBeatScroll = objectInputStream.readBoolean()
                mSmoothScroll = objectInputStream.readBoolean()
                mOrientation = objectInputStream.readInt()
                mMinFontSize = objectInputStream.readInt()
                mMaxFontSize = objectInputStream.readInt()
                mScreenWidth = objectInputStream.readInt()
                mScreenHeight = objectInputStream.readInt()
                val availableEnd = byteArrayInputStream.available()
                objectInputStream.close()
                byteArrayInputStream.close()
                mMessageLength = 1 + (availableStart - availableEnd)
            } else
                throw NotEnoughBluetoothDataException()
        } catch (e: Exception) {
            Log.e(BeatPrompterApplication.TAG, "Couldn't read ChooseSongMessage data, assuming not enough data", e)
            throw NotEnoughBluetoothDataException()
        }
    }

    companion object {
        internal const val CHOOSE_SONG_MESSAGE_ID: Byte = 0
    }
}
