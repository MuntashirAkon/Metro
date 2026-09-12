/*
 * Copyright (c) 2019 Hemanth Savarla.
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

package code.name.monkey.retromusic.repository

import android.provider.MediaStore.Audio.AudioColumns
import code.name.monkey.retromusic.ALBUM_ARTIST
import code.name.monkey.retromusic.helper.SortOrder
import code.name.monkey.retromusic.model.Album
import code.name.monkey.retromusic.model.Artist
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.ArtistTagUtil
import code.name.monkey.retromusic.util.PreferenceUtil
import java.text.Collator

interface ArtistRepository {
    fun artists(): List<Artist>

    fun albumArtists(): List<Artist>

    fun albumArtists(query: String): List<Artist>

    fun artists(query: String): List<Artist>

    fun artist(artistId: Long): Artist

    fun albumArtist(artistName: String): Artist

    // Multi-artist support: splits combined artist tags (e.g. "A, B feat. C")
    // into individual artists, so a song appears under each of its artists.
    fun multiArtists(): List<Artist>

    fun multiArtists(query: String): List<Artist>

    fun multiArtistByName(artistName: String): Artist
}

class RealArtistRepository(
    private val songRepository: RealSongRepository,
    private val albumRepository: RealAlbumRepository
) : ArtistRepository {

    private fun getSongLoaderSortOrder(): String {
        return PreferenceUtil.artistSortOrder + ", " +
                PreferenceUtil.artistAlbumSortOrder + ", " +
                PreferenceUtil.artistSongSortOrder
    }

    override fun artist(artistId: Long): Artist {
        if (artistId == Artist.VARIOUS_ARTISTS_ID) {
            // Get Various Artists
            val songs = songRepository.songs(
                songRepository.makeSongCursor(
                    null,
                    null,
                    getSongLoaderSortOrder()
                )
            )
            val albums = albumRepository.splitIntoAlbums(songs)
                .filter { it.albumArtist == Artist.VARIOUS_ARTISTS_DISPLAY_NAME }
            return Artist(Artist.VARIOUS_ARTISTS_ID, albums)
        }

        val songs = songRepository.songs(
            songRepository.makeSongCursor(
                AudioColumns.ARTIST_ID + "=?",
                arrayOf(artistId.toString()),
                getSongLoaderSortOrder()
            )
        )
        return Artist(artistId, albumRepository.splitIntoAlbums(songs))
    }

    override fun albumArtist(artistName: String): Artist {
        if (artistName == Artist.VARIOUS_ARTISTS_DISPLAY_NAME) {
            // Get Various Artists
            val songs = songRepository.songs(
                songRepository.makeSongCursor(
                    null,
                    null,
                    getSongLoaderSortOrder()
                )
            )
            val albums = albumRepository.splitIntoAlbums(songs)
                .filter { it.albumArtist == Artist.VARIOUS_ARTISTS_DISPLAY_NAME }
            return Artist(Artist.VARIOUS_ARTISTS_ID, albums, true)
        }

        val songs = songRepository.songs(
            songRepository.makeSongCursor(
                "album_artist" + "=?",
                arrayOf(artistName),
                getSongLoaderSortOrder()
            )
        )
        return Artist(artistName, albumRepository.splitIntoAlbums(songs), true)
    }

    override fun artists(): List<Artist> {
        val songs = songRepository.songs(
            songRepository.makeSongCursor(
                null, null,
                getSongLoaderSortOrder()
            )
        )
        val artists = splitIntoArtists(albumRepository.splitIntoAlbums(songs))
        return sortArtists(artists)
    }

    override fun albumArtists(): List<Artist> {
        val songs = songRepository.songs(
            songRepository.makeSongCursor(
                null,
                null,
                "lower($ALBUM_ARTIST)" +
                        if (PreferenceUtil.artistSortOrder == SortOrder.ArtistSortOrder.ARTIST_A_Z) "" else " DESC"
            )
        )
        val artists = splitIntoAlbumArtists(albumRepository.splitIntoAlbums(songs))
        return sortArtists(artists)
    }

    override fun albumArtists(query: String): List<Artist> {
        val songs = songRepository.songs(
            songRepository.makeSongCursor(
                "album_artist" + " LIKE ?",
                arrayOf("%$query%"),
                getSongLoaderSortOrder()
            )
        )
        val artists = splitIntoAlbumArtists(albumRepository.splitIntoAlbums(songs))
        return sortArtists(artists)
    }

    override fun artists(query: String): List<Artist> {
        val songs = songRepository.songs(
            songRepository.makeSongCursor(
                AudioColumns.ARTIST + " LIKE ?",
                arrayOf("%$query%"),
                getSongLoaderSortOrder()
            )
        )
        val artists = splitIntoArtists(albumRepository.splitIntoAlbums(songs))
        return sortArtists(artists)
    }

    override fun multiArtists(): List<Artist> {
        val songs = songRepository.songs(
            songRepository.makeSongCursor(
                null, null,
                getSongLoaderSortOrder()
            )
        )
        val artists = splitIntoMultiArtists(songs)
        return sortArtists(artists)
    }

    override fun multiArtists(query: String): List<Artist> {
        val songs = songRepository.songs(
            songRepository.makeSongCursor(
                AudioColumns.ARTIST + " LIKE ?",
                arrayOf("%$query%"),
                getSongLoaderSortOrder()
            )
        )
        val artists = splitIntoMultiArtists(songs)
        return sortArtists(artists).filter { artist ->
            artist.name.contains(query, ignoreCase = true)
        }
    }

    override fun multiArtistByName(artistName: String): Artist {
        if (artistName == Artist.VARIOUS_ARTISTS_DISPLAY_NAME) {
            val songs = songRepository.songs(
                songRepository.makeSongCursor(
                    null,
                    null,
                    getSongLoaderSortOrder()
                )
            )
            val albums = albumRepository.splitIntoAlbums(songs)
                .filter { it.albumArtist == Artist.VARIOUS_ARTISTS_DISPLAY_NAME }
            return Artist(Artist.VARIOUS_ARTISTS_ID, albums, true)
        }

        // We can't filter this in SQL because the artist name we want might be
        // embedded inside a combined tag (e.g. looking for "Amir Eid" inside
        // "Cairokee, Amir Eid"). So we pull all songs whose raw artist tag
        // contains the name as a substring (cheap SQL pre-filter), then confirm
        // with the real split logic, then re-group only the matching songs.
        val songs = songRepository.songs(
            songRepository.makeSongCursor(
                AudioColumns.ARTIST + " LIKE ?",
                arrayOf("%$artistName%"),
                getSongLoaderSortOrder()
            )
        )
        val matchingSongs = songs.filter { song ->
            ArtistTagUtil.splitArtistNames(song.artistName)
                .any { it.equals(artistName, ignoreCase = true) }
        }
        val albums = albumRepository.splitIntoAlbums(matchingSongs)
        return Artist(artistName, albums, isAlbumArtist = false, isMultiArtist = true)
    }

    private fun splitIntoAlbumArtists(albums: List<Album>): List<Artist> {
        return albums.groupBy { it.albumArtist }
            .filter {
                !it.key.isNullOrEmpty()
            }
            .map {
                val currentAlbums = it.value
                if (currentAlbums.isNotEmpty()) {
                    if (currentAlbums[0].albumArtist == Artist.VARIOUS_ARTISTS_DISPLAY_NAME) {
                        Artist(Artist.VARIOUS_ARTISTS_ID, currentAlbums, true)
                    } else {
                        Artist(currentAlbums[0].artistId, currentAlbums, true)
                    }
                } else {
                    Artist.empty
                }
            }
    }


    fun splitIntoArtists(albums: List<Album>): List<Artist> {
        return albums.groupBy { it.artistId }
            .map { Artist(it.key, it.value) }
    }

    /**
     * Groups songs by each *individual* artist name found in each song's own
     * raw artist tag, after splitting on the configured separators (see
     * [ArtistTagUtil]). This works at the Song level (not Album level) so that
     * within a single album (e.g. a movie/game soundtrack with a different
     * featured artist per track), each track is credited to its own artists
     * rather than inheriting the artist of the album's first song.
     *
     * A song/track with multiple artists ends up under every one of those
     * artists, and each artist's page shows every track they contributed to -
     * including as a secondary/featured artist.
     */
    fun splitIntoMultiArtists(songs: List<Song>): List<Artist> {
        val buckets = LinkedHashMap<String, MutableList<Song>>()
        val displayNames = LinkedHashMap<String, String>()

        for (song in songs) {
            val names = ArtistTagUtil.splitArtistNames(song.artistName)
            for (name in names) {
                val key = name.lowercase()
                buckets.getOrPut(key) { mutableListOf() }.add(song)
                if (!displayNames.containsKey(key)) {
                    displayNames[key] = name
                }
            }
        }

        return buckets.map { (key, songsForArtist) ->
            val albums = albumRepository.splitIntoAlbums(songsForArtist)
            Artist(
                displayNames[key] ?: key,
                albums,
                isAlbumArtist = false,
                isMultiArtist = true
            )
        }
    }

    private fun sortArtists(artists: List<Artist>): List<Artist> {
        val collator = Collator.getInstance()
        return when (PreferenceUtil.artistSortOrder) {
            SortOrder.ArtistSortOrder.ARTIST_A_Z -> {
                artists.sortedWith { a1, a2 -> collator.compare(a1.name, a2.name) }
            }
            SortOrder.ArtistSortOrder.ARTIST_Z_A -> {
                artists.sortedWith { a1, a2 -> collator.compare(a2.name, a1.name) }
            }
            else -> artists
        }
    }
}
