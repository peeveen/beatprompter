package com.stevenfrew.beatprompter

internal class CircularGraphicsList:ArrayList<LineGraphic>() {
    override fun add(element:LineGraphic):Boolean {
        if (!isEmpty()) {
            element.mPrevGraphic = last()
            last().mNextGraphic = element
            element.mNextGraphic = first()
            first().mPrevGraphic = element
        }
        else
        {
            element.mPrevGraphic=element
            element.mNextGraphic=element
        }
        return super.add(element)
    }
}