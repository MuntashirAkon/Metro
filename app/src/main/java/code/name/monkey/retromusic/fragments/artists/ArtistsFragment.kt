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

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.GridLayoutManager
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.adapter.artist.ArtistAdapter
import code.name.monkey.retromusic.databinding.FragmentArtistsBinding
import code.name.monkey.retromusic.extensions.accentColor
import code.name.monkey.retromusic.extensions.dip
import code.name.monkey.retromusic.fragments.base.AbsLibraryPagerRecyclerViewFragment
import code.name.monkey.retromusic.helper.SortOrder
import code.name.monkey.retromusic.interfaces.IArtistClickListener
import code.name.monkey.retromusic.model.Artist
import code.name.monkey.retromusic.util.PreferenceUtil
import com.google.android.material.transition.MaterialSharedAxisTransition

class ArtistsFragment : AbsLibraryPagerRecyclerViewFragment<ArtistAdapter, GridLayoutManager>(),
    IArtistClickListener {
    private var _binding: FragmentArtistsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentArtistsBinding.bind(view)
        enterTransition = MaterialSharedAxisTransition(MaterialSharedAxisTransition.Y_AXIS, true)
        exitTransition = MaterialSharedAxisTransition(MaterialSharedAxisTransition.Y_AXIS, false)
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        menu.findItem(R.id.action_grid_size)?.isVisible = true
        menu.findItem(R.id.action_layout_type)?.isVisible = true
        menu.findItem(R.id.action_sort_order)?.isVisible = true
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (handleSortOrderMenuItem(menuItem)) {
            return true
        }
        return super.onMenuItemSelected(menuItem)
    }

    private fun handleSortOrderMenuItem(item: MenuItem): Boolean {
        val sortOrder: String = when (item.itemId) {
            R.id.action_sort_order_asc -> SortOrder.ArtistSortOrder.ARTIST_A_Z
            R.id.action_sort_order_desc -> SortOrder.ArtistSortOrder.ARTIST_Z_A
            R.id.action_sort_order_artist_song_count -> SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_SONGS
            R.id.action_sort_order_artist_album_count -> SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_ALBUMS
            else -> return false
        }
        item.isChecked = true
        PreferenceUtil.artistSortOrder = sortOrder
        return true
    }

    override fun createLayoutManager(): GridLayoutManager {
        return GridLayoutManager(
            requireActivity(),
            getGridSize(),
            GridLayoutManager.VERTICAL,
            false
        )
    }

    override fun createAdapter(): ArtistAdapter {
        return ArtistAdapter(
            requireActivity(),
            emptyList(),
            PreferenceUtil.artistGridStyle,
            this,
            null,
            null
        )
    }

    override fun onArtist(artistId: Long, view: View) {
        exitTransition = MaterialSharedAxisTransition(MaterialSharedAxisTransition.Y_AXIS, false)
        requireView().findNavController().navigate(
            R.id.artistDetailsFragment,
            bundleOf(EXTRA_ARTIST_ID to artistId),
            null,
            FragmentNavigatorExtras(view to artistId.toString())
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun getEmptyStateDrawableRes(): Int = R.drawable.ic_empty_music
    override fun getEmptyStateTextRes(): Int = R.string.no_artists

    companion object {
        const val TAG: String = "ArtistsFragment"
        private const val EXTRA_ARTIST_ID = "extra_artist_id"
        private const val EXTRA_ARTIST_NAME = "extra_artist_name"
    }
}
