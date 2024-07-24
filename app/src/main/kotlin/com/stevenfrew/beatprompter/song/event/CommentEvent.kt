package com.stevenfrew.beatprompter.song.event

import com.stevenfrew.beatprompter.song.Song

/**
 * A CommentEvent tells the event processor to display a comment message onscreen temporarily.
 */
class CommentEvent(
	eventTime: Long,
	val comment: Song.Comment
) : BaseEvent(eventTime) {
	override fun offset(nanoseconds: Long): BaseEvent =
		CommentEvent(eventTime + nanoseconds, comment)
}