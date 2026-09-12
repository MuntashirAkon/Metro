/*
 * Copyright (c) 2026
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by
 *  the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

package code.name.monkey.retromusic.util

/**
 * Splits a raw "artist" tag string (as stored in MediaStore) into a list of
 * individual artist names.
 *
 * Handles the following multi-artist separators found in music tags:
 *   ","          e.g. "Cairokee, Amir Eid"   -> ["Cairokee", "Amir Eid"]
 *   "&"          e.g. "Drake & Rihanna"       -> ["Drake", "Rihanna"]
 *   ";"          e.g. "Drake; Rihanna"        -> ["Drake", "Rihanna"]
 *   "Feat."/"feat." e.g. "Drake feat. Rihanna" -> ["Drake", "Rihanna"]
 *   "ft"         e.g. "Drake ft Rihanna"      -> ["Drake", "Rihanna"]
 *   "featuring"  e.g. "Drake featuring Rihanna" -> ["Drake", "Rihanna"]
 *
 * Note: "feat" without a trailing period is NOT treated as a separator
 * (only "feat." is), to avoid accidentally splitting on the substring
 * inside unrelated words/names.
 *
 * Names are trimmed and de-duplicated (case-insensitive) while preserving
 * the original casing of the first occurrence.
 */
object ArtistTagUtil {

    private val SEPARATOR_REGEX = Regex(
        pattern = """\s*(?:,|;|&|\bfeat\.|\bft\.?\b|\bfeaturing\b)\s*""",
        option = RegexOption.IGNORE_CASE
    )

    /**
     * Splits [rawArtistName] into a list of trimmed, non-empty, de-duplicated
     * artist names. If nothing can be split (or input is blank), returns a
     * single-element list containing the trimmed input (or empty list if blank).
     */
    fun splitArtistNames(rawArtistName: String?): List<String> {
        if (rawArtistName.isNullOrBlank()) return emptyList()

        val parts = rawArtistName
            .split(SEPARATOR_REGEX)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (parts.isEmpty()) return listOf(rawArtistName.trim())

        // De-duplicate case-insensitively while keeping first-seen casing
        val seen = LinkedHashMap<String, String>()
        for (part in parts) {
            val key = part.lowercase()
            if (!seen.containsKey(key)) {
                seen[key] = part
            }
        }
        return seen.values.toList()
    }

    /**
     * True if [rawArtistName] contains more than one artist once split.
     */
    fun isMultiArtist(rawArtistName: String?): Boolean {
        return splitArtistNames(rawArtistName).size > 1
    }
}
