package com.stevenfrew.beatprompter.comm.bluetooth.message

import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * Bluetooth message that instructs the receiver to stop playing the current song.
 */
class QuitSongMessage constructor(val bytes: ByteArray,
                                  val songTitle: String,
                                  val songArtist: String)
    : BluetoothMessage(bytes) {
    constructor(songTitle: String, songArtist: String)
            : this(asBytes(songTitle, songArtist), songTitle, songArtist)

    companion object {
        private fun asBytes(songTitle: String, songArtist: String): ByteArray {
            return ByteArrayOutputStream().apply {
                write(byteArrayOf(QUIT_SONG_MESSAGE_ID), 0, 1)
                ObjectOutputStream(this).apply {
                    writeObject(songTitle)
                    writeObject(songArtist)
                    flush()
                    close()
                }
            }.toByteArray()
        }

        @Throws(NotEnoughDataException::class)
        internal fun fromBytes(bytes: ByteArray): QuitSongMessage {
            try {
                ByteArrayInputStream(bytes).apply {
                    val dataRead = read(ByteArray(1))
                    if (dataRead == 1) {
                        val availableStart = available()
                        val songInfo =
                                with(ObjectInputStream(this)) {
                                    val title = readObject() as String
                                    val artist = readObject() as String
                                    title to artist
                                }
                        val availableEnd = available()
                        close()
                        val messageLength = 1 + (availableStart - availableEnd)
                        return QuitSongMessage(bytes.copyOfRange(0, messageLength),
                                songInfo.first,
                                songInfo.second)
                    }
                }
            } catch (e: Exception) {
                Log.e(BeatPrompterApplication.TAG_COMMS, "Couldn't read QuitSongMessage data, assuming not enough data", e)
            }
            throw NotEnoughDataException()
        }
    }
}