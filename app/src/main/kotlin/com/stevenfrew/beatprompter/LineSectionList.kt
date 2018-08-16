package com.stevenfrew.beatprompter

class LineSectionList:ArrayList<LineSection>() {
    override fun add(element:LineSection):Boolean {
        val last = lastOrNull()
        if (last != null)
            last.mNextSection = element
        return super.add(element)
    }
}