package com.stevenfrew.beatprompter

class LineSectionList:ArrayList<LineSection>() {
    override fun add(element:LineSection):Boolean {
        lastOrNull()?.mNextSection=element
        return super.add(element)
    }
}