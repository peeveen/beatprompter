package com.stevenfrew.beatprompter.song.event

import com.stevenfrew.beatprompter.song.Song

/**
 * A CommentEvent tells the event processor to display a comment message onscreen temporarily.
 */
class CommentEvent(
	eventTime: Long,
	val mComment: Song.Comment
) : BaseEvent(eventTime)