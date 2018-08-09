package com.stevenfrew.beatprompter.cloud

interface CloudListener {
    fun onAuthenticationRequired()
    fun shouldCancel(): Boolean
}