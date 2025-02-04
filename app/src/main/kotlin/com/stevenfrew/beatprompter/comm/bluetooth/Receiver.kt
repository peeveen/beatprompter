package com.stevenfrew.beatprompter.comm.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothSocket
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.comm.CommunicationType
import com.stevenfrew.beatprompter.comm.Message
import com.stevenfrew.beatprompter.comm.ReceiverBase
import com.stevenfrew.beatprompter.comm.ReceiverTask
import com.stevenfrew.beatprompter.comm.bluetooth.message.BluetoothMessage
import com.stevenfrew.beatprompter.comm.bluetooth.message.ChooseSongMessage
import com.stevenfrew.beatprompter.comm.bluetooth.message.HeartbeatMessage
import com.stevenfrew.beatprompter.comm.bluetooth.message.NotEnoughDataException
import com.stevenfrew.beatprompter.comm.bluetooth.message.PauseOnScrollStartMessage
import com.stevenfrew.beatprompter.comm.bluetooth.message.QuitSongMessage
import com.stevenfrew.beatprompter.comm.bluetooth.message.SetSongTimeMessage
import com.stevenfrew.beatprompter.comm.bluetooth.message.ToggleStartStopMessage
import com.stevenfrew.beatprompter.comm.bluetooth.message.UnknownMessageException
import com.stevenfrew.beatprompter.events.EventRouter
import com.stevenfrew.beatprompter.events.Events

@SuppressLint("MissingPermission") // The method that uses this constructor checks for SecurityException.
class Receiver(private val socket: BluetoothSocket, type: CommunicationType) :
	ReceiverBase(socket.remoteDevice.name, type) {
	override fun unregister(task: ReceiverTask) = Bluetooth.removeReceiver(task)

	override fun receiveMessageData(buffer: ByteArray, offset: Int, maximumAmount: Int): Int =
		socket.inputStream.read(buffer, offset, maximumAmount)

	override fun parseMessageData(buffer: ByteArray, dataEnd: Int): Int {
		var bufferCopy = buffer
		var dataParsed = 0
		var dataRemaining = dataEnd
		val receivedMessages = mutableListOf<Message>()
		var lastSetTimeMessage: SetSongTimeMessage? = null
		var lastChooseSongMessage: ChooseSongMessage? = null
		while (dataRemaining > 0) {
			try {
				val messageLength = try {
					fromBytes(bufferCopy).also {
						if (it is SetSongTimeMessage)
							lastSetTimeMessage = it
						else if (it is ChooseSongMessage)
							lastChooseSongMessage = it
						receivedMessages.add(it)
					}.length
				} catch (_: UnknownMessageException) {
					Logger.logComms("Unknown Bluetooth message received.")
					// If bad message, skip the byte that doesn't match any known message type
					1
				}

				dataParsed += messageLength
				bufferCopy = bufferCopy.copyOfRange(messageLength, dataRemaining)
				dataRemaining -= messageLength
			} catch (_: NotEnoughDataException) {
				// Read again!
				break
			}
		}
		// If we receive multiple SetSongTimeMessages or ChooseSongMessages, there is no point in processing any except the last one.
		receivedMessages
			.filter {
				(it !is SetSongTimeMessage && it !is ChooseSongMessage)
					|| it == lastSetTimeMessage
					|| it == lastChooseSongMessage
			}
			.forEach { routeBluetoothMessage(it) }
		return dataParsed
	}

	override fun close() = socket.close()

	private fun routeBluetoothMessage(msg: Message) {
		when (msg) {
			is ChooseSongMessage -> EventRouter.sendEventToSongList(
				Events.BLUETOOTH_CHOOSE_SONG,
				msg.choiceInfo
			)

			is PauseOnScrollStartMessage -> EventRouter.sendEventToSongDisplay(Events.BLUETOOTH_PAUSE_ON_SCROLL_START)
			is QuitSongMessage -> EventRouter.sendEventToSongDisplay(
				Events.BLUETOOTH_QUIT_SONG,
				msg.songTitle to msg.songArtist
			)

			is SetSongTimeMessage -> EventRouter.sendEventToSongDisplay(
				Events.BLUETOOTH_SET_SONG_TIME,
				msg.time
			)

			is ToggleStartStopMessage -> EventRouter.sendEventToSongDisplay(
				Events.BLUETOOTH_TOGGLE_START_STOP,
				msg.toggleInfo
			)
		}
	}

	companion object {
		/**
		 * Constructs the message object from the received bytes.
		 */
		internal fun fromBytes(bytes: ByteArray): Message {
			if (bytes.isNotEmpty())
				return when (bytes[0]) {
					BluetoothMessage.CHOOSE_SONG_MESSAGE_ID -> ChooseSongMessage.fromBytes(bytes)
					BluetoothMessage.TOGGLE_START_STOP_MESSAGE_ID -> ToggleStartStopMessage.fromBytes(bytes)
					BluetoothMessage.SET_SONG_TIME_MESSAGE_ID -> SetSongTimeMessage.fromBytes(bytes)
					BluetoothMessage.PAUSE_ON_SCROLL_START_MESSAGE_ID -> PauseOnScrollStartMessage
					BluetoothMessage.QUIT_SONG_MESSAGE_ID -> QuitSongMessage.fromBytes(bytes)
					BluetoothMessage.HEARTBEAT_MESSAGE_ID -> HeartbeatMessage
					else -> throw UnknownMessageException()
				}
			throw NotEnoughDataException()
		}
	}
}