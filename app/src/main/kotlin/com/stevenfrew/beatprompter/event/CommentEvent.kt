package com.stevenfrew.beatprompter.event

import com.stevenfrew.beatprompter.Comment

/**
 * A CommentEvent tells the event processor to display a comment message onscreen temporarily.
 */
class CommentEvent(eventTime: Long, var mComment: Comment) : BaseEvent(eventTime)