package com.stevenfrew.beatprompter.collections

import com.stevenfrew.beatprompter.event.*

class EventList: ArrayList<BaseEvent>() {
    init {
        add(StartEvent())
    }
    override fun addAll(elements:Collection<BaseEvent>):Boolean
    {
        elements.forEach { add(it) }
        return true
    }
    override fun add(element:BaseEvent):Boolean {
        // First line event should be inserted at the start of the list immediately
        // after the StartEvent
        if(element is LineEvent) {
            if (element.mLine.mLineTime == 0L) {
                add(1, element)
                return true
            }
        }
        else if(element.mEventTime>lastOrNull()?.mEventTime?:0)
            return super.add(element)
        val index=findLastEventBefore(element.mEventTime)
        add(index+1,element)
        return true
    }
    private fun findLastEventBefore(time:Long):Int
    {
        if(isNotEmpty())
            for(f in (size-1) downTo 0)
                if(get(f).mEventTime<=time)
                    return f
        return -1
    }
    fun buildEventChain(finalSongTime:Long):BaseEvent
    {
        val firstEvent=removeAt(0)
        var nextEvent=firstEvent
        val audioEndTimes=mutableListOf(finalSongTime)
        forEach {
            if(it is AudioEvent)
                audioEndTimes.add(it.mAudioFile.mDuration+it.mEventTime)
            nextEvent.add(it)
            nextEvent=it
        }
        nextEvent.add(EndEvent(audioEndTimes.max()!!))
        return firstEvent
    }
}