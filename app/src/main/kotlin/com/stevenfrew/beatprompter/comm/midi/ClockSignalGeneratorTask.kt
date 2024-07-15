package com.stevenfrew.beatprompter.comm.midi

import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.Task
import com.stevenfrew.beatprompter.comm.midi.message.StartMessage
import com.stevenfrew.beatprompter.comm.midi.message.StopMessage
import com.stevenfrew.beatprompter.util.Utils

class ClockSignalGeneratorTask : Task(false) {
	private var mLastSignalTime = 0.0
	private var mClockSignalsSent = 0
	private var mNanoSecondsPerMidiSignal = 0.0
	private var mNextNanoSecondsPerMidiSignal = 0.0
	private val nanoSecondsPerMidiSignalSync = Any()
	private val nextNanoSecondsPerMidiSignalSync = Any()
	private val lastSignalTimeSync = Any()
	private val clockSignalsSentSync = Any()
	private var nanoSecondsPerMidiSignal: Double
		get() = synchronized(nanoSecondsPerMidiSignalSync) {
			return mNanoSecondsPerMidiSignal
		}
		set(value) = synchronized(nanoSecondsPerMidiSignalSync) {
			mNanoSecondsPerMidiSignal = value
		}
	private var nextNanoSecondsPerMidiSignal: Double
		get() = synchronized(nextNanoSecondsPerMidiSignalSync) {
			return mNextNanoSecondsPerMidiSignal
		}
		set(value) = synchronized(nextNanoSecondsPerMidiSignalSync) {
			mNextNanoSecondsPerMidiSignal = value
		}
	private var lastSignalTime: Double
		get() = synchronized(lastSignalTimeSync) {
			return mLastSignalTime
		}
		set(value) = synchronized(lastSignalTimeSync) {
			mLastSignalTime = value
		}
	private val clockSignalsSent: Int
		get() = synchronized(clockSignalsSentSync) {
			return mClockSignalsSent
		}

	private fun incrementClockSignalsSent(): Int {
		synchronized(clockSignalsSentSync) {
			return ++mClockSignalsSent
		}
	}

	private fun resetClockSignalsSent() {
		synchronized(clockSignalsSentSync) {
			mClockSignalsSent = 0
		}
	}

	override fun doWork() {
		val vNanoSecondsPerMidiSignal = nextNanoSecondsPerMidiSignal

		// No speed set yet? Just return.
		if (vNanoSecondsPerMidiSignal == 0.0)
			return

		// If we're on a 24-signal boundary, switch to the next speed.
		if (clockSignalsSent == 0)
			nanoSecondsPerMidiSignal = vNanoSecondsPerMidiSignal
		val nanoTime = System.nanoTime()
		var nanoDiff = nanoTime - lastSignalTime
		var signalsNeeded = 0
		while (nanoDiff >= nanoSecondsPerMidiSignal) {
			try {
				++signalsNeeded
				// We've hit the 24-signal boundary. Switch to the next speed.
				if (incrementClockSignalsSent() == 24) {
					resetClockSignalsSent()
					nanoSecondsPerMidiSignal = nextNanoSecondsPerMidiSignal
				}
			} catch (e: Exception) {
				Logger.logComms({ "Failed to add MIDI timing clock signal to output queue." }, e)
			}
			nanoDiff -= nanoSecondsPerMidiSignal
		}
		MidiController.addBeatClockMessages(signalsNeeded)
		if (signalsNeeded > 0) {
			lastSignalTime = nanoTime - nanoDiff
			val nextSignalDue = lastSignalTime + nanoSecondsPerMidiSignal
			val nextSignalDueNano = nextSignalDue.toLong() - nanoTime
			if (nextSignalDueNano > 0) {
				val nextSignalDueMilli = Utils.nanoToMilli(nextSignalDueNano).toLong()
				val nextSignalDueNanoRoundedToMilli = Utils.milliToNano(nextSignalDueMilli)
				val nextSignalDueNanoRemainder =
					(if (nextSignalDueNanoRoundedToMilli > 0)
						nextSignalDueNano % nextSignalDueNanoRoundedToMilli
					else
						nextSignalDueNano)
						.toInt()
				try {
					Thread.sleep(nextSignalDueMilli, nextSignalDueNanoRemainder)
				} catch (e: Exception) {
					Logger.logComms({ "Thread sleep was interrupted." }, e)
				}
			}
		}
	}

	fun setBPM(bpm: Double) {
		if (bpm == 0.0)
			return
		if (shouldStop)
			return
		val oldNanoSecondsPerMidiSignal = nextNanoSecondsPerMidiSignal
		val newNanosecondsPerMidiSignal = Utils.bpmToMIDIClockNanoseconds(bpm)
		if (oldNanoSecondsPerMidiSignal == 0.0) {
			// This is the first BPM value being set.
			resetClockSignalsSent()
			lastSignalTime = System.nanoTime().toDouble()
			try {
				MidiController.putMessage(StartMessage)
			} catch (e: Exception) {
				Logger.logComms({ "Failed to add MIDI start signal to output queue." }, e)
			}

			nanoSecondsPerMidiSignal = newNanosecondsPerMidiSignal
			nextNanoSecondsPerMidiSignal = newNanosecondsPerMidiSignal
		} else
			nextNanoSecondsPerMidiSignal = newNanosecondsPerMidiSignal
	}

	override fun stop() {
		super.stop()
		lastSignalTime = 0.0
		resetClockSignalsSent()
		try {
			MidiController.putMessage(StopMessage)
		} catch (e: Exception) {
			Logger.logComms({ "Failed to add MIDI stop signal to output queue." }, e)
		}
	}
}
