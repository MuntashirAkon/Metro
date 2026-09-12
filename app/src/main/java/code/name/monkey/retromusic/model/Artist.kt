/*
 * Copyright (c) 2019 Hemanth Savarala.
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

package code.name.monkey.retromusic.model

import android.os.Parcelable
import code.name.monkey.retromusic.helper.SortOrder
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.util.PreferenceUtil
import kotlinx.parcelize.Parcelize
import java.text.Collator

@Parcelize
data class Artist(
    val id: Long,
    val albums: List<Album>,
    val isAlbumArtist: Boolean = false,
    // True when this Artist was built by splitting a combined artist tag
    // (e.g. "A, B feat. C") into individual artists. When true, `name`
    // uses [multiArtistName] as-is instead of re-deriving it from the raw
    // (un-split) tag on the first song.
    val isMultiArtist: Boolean = false,
    // Only set (and only used) when isMultiArtist is true. Kept as an
    // explicit primary-constructor property — rather than assigned onto a
    // body `var` from the secondary constructor, as before — so it
    // round-trips correctly through @Parcelize; anything not in the
    // primary constructor resets to its default when un-parceled.
    private val multiArtistName: String? = null
) : Parcelable {
    constructor(
        artistName: String,
        albums: List<Album>,
        isAlbumArtist: Boolean = false,
        isMultiArtist: Boolean = false
    ) : this(
        if (isMultiArtist) {
            // Derive a stable-ish id from the split name itself, since there's
            // no single MediaStore artistId that represents a split artist.
            artistName.lowercase().hashCode().toLong()
        } else {
            albums.firstOrNull()?.artistId ?: -1L
        },
        albums,
        isAlbumArtist,
        isMultiArtist,
        if (isMultiArtist) artistName else null
    )

    val name: String
        get() {
            // Multi-artist entries already carry the correct split name;
            // don't re-derive it from the (un-split) raw tag on the song.
            if (isMultiArtist) return multiArtistName ?: "-"

            val name = if (isAlbumArtist) getAlbumArtistName()
            else getArtistName()
            return when {
                MusicUtil.isVariousArtists(name) ->
                    VARIOUS_ARTISTS_DISPLAY_NAME
                MusicUtil.isArtistNameUnknown(name) ->
                    UNKNOWN_ARTIST_DISPLAY_NAME
                else -> name!!
            }
        }

    val songCount: Int
        get() {
            var songCount = 0
            for (album in albums) {
                songCount += album.songCount
            }
            return songCount
        }

    val albumCount: Int
        get() = albums.size

    val songs: List<Song>
        get() = albums.flatMap { it.songs }

    val sortedSongs: List<Song>
        get() {
            val collator = Collator.getInstance()
            return songs.sortedWith(
                when (PreferenceUtil.artistDetailSongSortOrder) {
                    SortOrder.ArtistSongSortOrder.SONG_A_Z -> { o1, o2 ->
                        collator.compare(o1.title, o2.title)
                    }
                    SortOrder.ArtistSongSortOrder.SONG_Z_A -> { o1, o2 ->
                        collator.compare(o2.title, o1.title)
                    }
                    SortOrder.ArtistSongSortOrder.SONG_ALBUM -> { o1, o2 ->
                        collator.compare(o1.albumName, o2.albumName)
                    }
                    SortOrder.ArtistSongSortOrder.SONG_YEAR -> { o1, o2 ->
                        o2.year.compareTo(
                            o1.year
                        )
                    }
                    SortOrder.ArtistSongSortOrder.SONG_DURATION -> { o1, o2 ->
                        o1.duration.compareTo(
                            o2.duration
                        )
                    }
                    else -> {
                        throw IllegalArgumentException("invalid ${PreferenceUtil.artistDetailSongSortOrder}")
                    }
                })
        }

    fun safeGetFirstAlbum(): Album {
        return albums.firstOrNull() ?: Album.empty
    }

    private fun getArtistName(): String {
        return safeGetFirstAlbum().safeGetFirstSong().artistName
    }

    private fun getAlbumArtistName(): String? {
        return safeGetFirstAlbum().safeGetFirstSong().albumArtist
    }

    companion object {
        const val UNKNOWN_ARTIST_DISPLAY_NAME = "Unknown Artist"
        const val VARIOUS_ARTISTS_DISPLAY_NAME = "Various Artists"
        const val VARIOUS_ARTISTS_ID: Long = -2
        val empty = Artist(-1, emptyList())

    }
}
