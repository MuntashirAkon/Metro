/*
 * Copyright (c) 2020 Hemanth Savarla.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */
package code.name.monkey.retromusic.fragments.artists

import androidx.lifecycle.*
import code.name.monkey.retromusic.interfaces.IMusicServiceEventListener
import code.name.monkey.retromusic.model.Artist
import code.name.monkey.retromusic.repository.RealRepository
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

class ArtistDetailsViewModel(
    private val realRepository: RealRepository,
    private val artistId: Long?,
    private val artistName: String?,
    private val isMultiArtist: Boolean = false
) : ViewModel(), IMusicServiceEventListener {
    private val artistDetails = MutableLiveData<Artist>()
    private val biography = MutableLiveData<String?>()

    init {
        fetchArtist()
    }

    private fun fetchArtist() {
        viewModelScope.launch(IO) {
            if (isMultiArtist) {
                // Multi-artist mode: the artist we want may only appear inside
                // a combined tag (e.g. "Amr Diab, Jana Diab"), not as the
                // exact album_artist value, so we must use the split-aware
                // lookup instead of an exact album_artist match.
                artistName?.let {
                    val artist = realRepository.multiArtistByName(it)
                    artistDetails.postValue(artist)
                    fetchBiography(artist.name)
                }
            } else {
                artistId?.let {
                    val artist = realRepository.artistById(it)
                    artistDetails.postValue(artist)
                    fetchBiography(artist.name)
                }
                artistName?.let {
                    val artist = realRepository.albumArtistByName(it)
                    artistDetails.postValue(artist)
                    fetchBiography(artist.name)
                }
            }
        }
    }

    private fun fetchBiography(name: String) {
        viewModelScope.launch(IO) {
            biography.postValue(realRepository.artistBiography(name))
        }
    }

    fun getArtist(): LiveData<Artist> = artistDetails
    fun getBiography(): LiveData<String?> = biography

    override fun onMediaStoreChanged() {
        fetchArtist()
    }

    override fun onServiceConnected() {}
    override fun onServiceDisconnected() {}
    override fun onQueueChanged() {}
    override fun onPlayingMetaChanged() {}
    override fun onPlayStateChanged() {}
    override fun onRepeatModeChanged() {}
    override fun onShuffleModeChanged() {}
    override fun onFavoriteStateChanged() {}
}
