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
package code.name.monkey.retromusic.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.glide.RetroGlideExtension.artistImageOptions
import code.name.monkey.retromusic.model.Artist
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop

/**
 * Row adapter for the "Go to artist" picker dialog shown when a song has
 * multiple artists. Shows each artist's image (circular) next to their name.
 */
class ArtistPickerAdapter(
    private val activity: FragmentActivity,
    private val artists: List<Artist>
) : BaseAdapter() {

    override fun getCount(): Int = artists.size

    override fun getItem(position: Int): Artist = artists[position]

    override fun getItemId(position: Int): Long = artists[position].id

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(activity)
            .inflate(R.layout.dialog_artist_picker_item, parent, false)

        val artist = artists[position]
        val imageView = view.findViewById<ImageView>(R.id.artistImage)
        val nameView = view.findViewById<TextView>(R.id.artistName)

        nameView.text = artist.name
        Glide.with(activity)
            .asDrawable()
            .artistImageOptions(artist)
            .transform(CircleCrop())
            .load(RetroGlideExtension.getArtistModel(artist))
            .into(imageView)

        return view
    }
}
