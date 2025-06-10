package com.stevenfrew.beatprompter.ui.filter

class FilterComparator private constructor() : Comparator<Filter> {
	override fun compare(p0: Filter?, p1: Filter?): Int =
		if (p0 == null)
			if (p1 == null) 0 else -1
		else if (p1 == null)
			1
		// Return 1 to indicate that p1 should appear ABOVE in the list
		// -1 for BELOW
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

			is VariationFilter -> when (p1) {
				is AllSongsFilter -> 1
				is TemporarySetListFilter -> 1
				is FolderFilter -> 1
				is TagFilter -> 1
				is VariationFilter -> p0.name.compareTo(p1.name)
				else -> -1
			}

			is SetListFilter -> when (p1) {
				is SetListFilter -> p0.name.compareTo(p1.name)
				is MidiAliasFilesFilter -> -1
				else -> 1
			}

			else -> 1 // MIDIAliasFilesFilter
		}

	companion object {
		val instance = FilterComparator()
	}
}