package com.stevenfrew.beatprompter.util

import kotlinx.coroutines.CoroutineScope

interface CoroutineTask<TParameters, TProgress, TResult> : CoroutineScope {
	fun onPreExecute()
	fun doInBackground(params: TParameters, progressUpdater: suspend (TProgress) -> Unit): TResult
	fun onPostExecute(result: TResult)
	fun onProgressUpdate(progress: TProgress)
}