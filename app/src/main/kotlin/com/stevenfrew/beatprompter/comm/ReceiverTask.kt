package com.stevenfrew.beatprompter.comm

import com.stevenfrew.beatprompter.EventHandler
import kotlinx.coroutines.experimental.launch

class ReceiverTask : CommunicationTask<Receiver>() {
    override fun doWork() {
        while (!shouldStop) {
            try {
                val receivers = getCommunicators()
                if (receivers.isNotEmpty())
                    receivers.forEach {
                        launch {
                            try {
                                it.value.receive()
                            } catch (ioException: Exception) {
                                // Problem with the I/O. This receiver is now dead to us.
                                EventHandler.sendEventToSongDisplay(EventHandler.CONNECTION_LOST,it.value.name)
                                removeCommunicator(it.key)
                            }
                        }
                    }
                else
                    Thread.sleep(250)
            }
            catch(interruptedException:InterruptedException)
            {
                break
            }
        }
    }
}