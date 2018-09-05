package com.stevenfrew.beatprompter.bluetooth.message

/**
 * Exception thrown when we receive a Bluetooth message that doesn't match any of the known
 * ones.
 */
internal class UnknownBluetoothMessageException : Exception()