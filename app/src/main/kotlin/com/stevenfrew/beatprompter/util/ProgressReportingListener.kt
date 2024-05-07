package com.stevenfrew.beatprompter.util

interface ProgressReportingListener<TProgress> {
	suspend fun onProgressMessageReceived(message: TProgress)
}