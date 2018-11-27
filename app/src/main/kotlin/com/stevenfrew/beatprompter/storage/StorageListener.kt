package com.stevenfrew.beatprompter.storage

interface StorageListener {
    fun onAuthenticationRequired()
    fun shouldCancel(): Boolean
}