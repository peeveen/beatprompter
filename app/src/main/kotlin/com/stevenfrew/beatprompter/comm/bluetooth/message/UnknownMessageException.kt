package com.stevenfrew.beatprompter.comm.bluetooth.message

/**
 * Exception thrown when we receive a Bluetooth message that doesn't match any of the known
 * ones.
 */
internal class UnknownMessageException
	: Exception()