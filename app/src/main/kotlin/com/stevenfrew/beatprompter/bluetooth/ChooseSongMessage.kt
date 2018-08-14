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

    companion object {
        internal const val CHOOSE_SONG_MESSAGE_ID: Byte = 0

        @Throws(NotEnoughBluetoothDataException::class)
        internal fun fromBytes(bytes: ByteArray):IncomingBluetoothMessage {
            try {
                val byteArrayInputStream = ByteArrayInputStream(bytes)
                val dataRead = byteArrayInputStream.read(ByteArray(1))
                if (dataRead == 1) {
                    val availableStart = byteArrayInputStream.available()
                    val objectInputStream = ObjectInputStream(byteArrayInputStream)
                    val title = objectInputStream.readObject() as String
                    val track = objectInputStream.readObject() as String
                    val beatScroll = objectInputStream.readBoolean()
                    val smoothScroll = objectInputStream.readBoolean()
                    val orientation = objectInputStream.readInt()
                    val minFontSize = objectInputStream.readInt()
                    val maxFontSize = objectInputStream.readInt()
                    val screenWidth = objectInputStream.readInt()
                    val screenHeight = objectInputStream.readInt()
                    val availableEnd = byteArrayInputStream.available()
                    objectInputStream.close()
                    byteArrayInputStream.close()
                    val messageLength = 1 + (availableStart - availableEnd)
                    return IncomingBluetoothMessage(ChooseSongMessage(title,track,orientation,beatScroll,smoothScroll,minFontSize,maxFontSize,screenWidth,screenHeight),messageLength)
                } else
                    throw NotEnoughBluetoothDataException()
            } catch (e: Exception) {
                Log.e(BeatPrompterApplication.TAG, "Couldn't read ChooseSongMessage data, assuming not enough data", e)
                throw NotEnoughBluetoothDataException()
            }
        }
    }
}
