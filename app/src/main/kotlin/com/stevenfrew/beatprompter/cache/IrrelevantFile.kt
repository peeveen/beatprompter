package com.stevenfrew.beatprompter.cache

import org.w3c.dom.Element

@CacheXmlTag("irrelevantfile")
/**
 * A file in our cache that we won't be processing in any way.
 * We keep track of them so that they don't get downloaded every time.
 */
class IrrelevantFile internal constructor(file: CachedFile)
    : CachedFile(file)
