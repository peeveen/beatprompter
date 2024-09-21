package com.stevenfrew.beatprompter.comm.midi

import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.Task
import com.stevenfrew.beatprompter.util.Utils

object ClockSignalGeneratorTask : Task(false) {
	private var _lastSignalTime = 0.0
	private var _clockSignalsSent = 0
	private var _nanoSecondsPerMidiSignal = 0.0
	private var _nextNanoSecondsPerMidiSignal = 0.0
	private val nanoSecondsPerMidiSignalSync = Any()
	private val nextNanoSecondsPerMidiSignalSync = Any()
	private val lastSignalTimeSync = Any()
	private val clockSignalsSentSync = Any()
	private var nanoSecondsPerMidiSignal: Double
		get() = synchronized(nanoSecondsPerMidiSignalSync) {
			return _nanoSecondsPerMidiSignal
		}
		set(value) = synchronized(nanoSecondsPerMidiSignalSync) {
			_nanoSecondsPerMidiSignal = value
		}
	private var nextNanoSecondsPerMidiSignal: Double
		get() = synchronized(nextNanoSecondsPerMidiSignalSync) {
			return _nextNanoSecondsPerMidiSignal
		}
		set(value) = synchronized(nextNanoSecondsPerMidiSignalSync) {
			_nextNanoSecondsPerMidiSignal = value
		}
	private var lastSignalTime: Double
		get() = synchronized(lastSignalTimeSync) {
			return _lastSignalTime
		}
		set(value) = synchronized(lastSignalTimeSync) {
			_lastSignalTime = value
		}
	private val clockSignalsSent: Int
		get() = synchronized(clockSignalsSentSync) {
			return _clockSignalsSent
		}

	private fun incrementClockSignalsSent(): Int =
		synchronized(clockSignalsSentSync) {
			return ++_clockSignalsSent
		}

	private fun resetClockSignalsSent() =
		synchronized(clockSignalsSentSync) {
			_clockSignalsSent = 0
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
				Logger.logComms({ "Failed to add MIDI timing clock signal to output queue." }, false, e)
			}
			nanoDiff -= nanoSecondsPerMidiSignal
		}
		try {
			Logger.logComms("Sending $signalsNeeded clock messages.")
			Midi.addBeatClockMessages(signalsNeeded)
		} catch (interruptedException: InterruptedException) {
			// Task was interrupted by the song being paused or stopped
		}
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
					Logger.logComms({ "Thread sleep was interrupted." }, false, e)
				}
			}
		}
	}

	fun setBPM(bpm: Double): Boolean =
		(bpm != 0.0 && !shouldStop).also {
			if (it) {
				val oldNanoSecondsPerMidiSignal = nextNanoSecondsPerMidiSignal
				val newNanosecondsPerMidiSignal = Utils.bpmToMIDIClockNanoseconds(bpm)
				if (oldNanoSecondsPerMidiSignal == 0.0) {
					// This is the first BPM value being set.
					resetClockSignalsSent()
					lastSignalTime = System.nanoTime().toDouble()
					nanoSecondsPerMidiSignal = newNanosecondsPerMidiSignal
					nextNanoSecondsPerMidiSignal = newNanosecondsPerMidiSignal
				} else
					nextNanoSecondsPerMidiSignal = newNanosecondsPerMidiSignal
			}
		}

	fun reset() {
		lastSignalTime = 0.0
		resetClockSignalsSent()
	}

	override fun stop(): Boolean {
		Logger.logComms("ClockSignalGeneratorTask is STOPPING.")
		return super.stop().also {
			reset()
		}
	}
}

