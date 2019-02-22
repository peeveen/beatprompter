package com.stevenfrew.beatprompter.cache

enum class CacheComparisonResult {
    // Indicates that the file exists in the cache, and still has the same last-modified date.
    Same,
    // Indicates that the file exists in the cache, but is an older version.
    // OR that the file does not yet exist in the cache.
    Newer,
    // Indicates that the file exists in the cache, but it appears to have changed
    // location (subfolder) ... or now exists in additional or fewer locations.
    Relocated,
}