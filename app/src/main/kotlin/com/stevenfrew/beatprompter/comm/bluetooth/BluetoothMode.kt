package com.stevenfrew.beatprompter.comm.bluetooth

enum class BluetoothMode {
    // Not listening
    None,

    // Looking for a band leader to follow.
    Client,

    // Looking for band members to instruct.
    Server
}