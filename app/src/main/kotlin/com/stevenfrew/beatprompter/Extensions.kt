package com.stevenfrew.beatprompter

fun String.splitAndTrim(separator:String):List<String> {
    val bits=split(separator)
    return bits.mapNotNull()
    {
        val trimmed=it.trim()
        if(trimmed.isEmpty())
            null
        else
            trimmed
    }
}

/**
 * Replaces weird apostrophe with usual apostrophe ... prevents failed matches based on apostrophe difference.
 * Also remove any stupid BOF character
 */
fun String.normalize(): String {
    return replace('’', '\'').replace("\uFEFF", "").toLowerCase()
}

fun String?.looksLikeHex(): Boolean {
    if(this==null)
        return false
    val strippedString=stripHexSignifiers()
    // Hex values for this app are two-chars long, max.
    return strippedString.matches(Regex("[0-9a-f]{1,2}"))
}

fun String.stripHexSignifiers(): String {
    val str = toLowerCase()
    if (str.startsWith("0x"))
        return str.substringAfter("0x")
    else if (str.endsWith("h"))
        return str.substring(0, str.length - 1)
    return str
}

fun String.characters(): List<String> {
    return toCharArray().map{it.toString()}
}

fun List<Any?>.flattenAll():List<Any?> {
    val output=mutableListOf<Any?>()
    forEach{ e ->
        when(e) {
            !is List<Any?> -> output.add(e)
            else -> output.addAll(e.flattenAll())
        }
    }
    return output
}