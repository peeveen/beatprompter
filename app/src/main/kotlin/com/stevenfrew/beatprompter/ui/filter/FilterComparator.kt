package com.stevenfrew.beatprompter.ui.filter

class FilterComparator private constructor() : Comparator<Filter> {
	override fun compare(p0: Filter?, p1: Filter?): Int =
		if (p0 == null)
			if (p1 == null) 0 else -1
		else if (p1 == null)
			1
		else when (p0) {
			is AllSongsFilter -> -1
			is TemporarySetListFilter -> when (p1) {
				is AllSongsFilter -> 1
				else -> -1
			}

			is FolderFilter -> when (p1) {
				is AllSongsFilter -> 1
				is TemporarySetListFilter -> 1
				is FolderFilter -> p0.name.compareTo(p1.name)
				else -> -1
			}

			is TagFilter -> when (p1) {
				is AllSongsFilter -> 1
				is TemporarySetListFilter -> 1
				is FolderFilter -> 1
				is TagFilter -> p0.name.compareTo(p1.name)
				else -> -1
			}

			is SetListFilter -> when (p1) {
				is SetListFilter -> p0.name.compareTo(p1.name)
				is MIDIAliasFilesFilter -> 1
				else -> 1
			}

			else -> 1 // MIDIAliasFilesFilter
		}

	companion object {
		val instance = FilterComparator()
	}
}