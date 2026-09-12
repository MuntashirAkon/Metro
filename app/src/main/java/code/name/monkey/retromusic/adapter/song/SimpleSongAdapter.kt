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
package code.name.monkey.retromusic.adapter.song

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.FragmentActivity
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.glide.RetroGlideExtension.songCoverOptions
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.MusicUtil
import com.bumptech.glide.Glide

class SimpleSongAdapter(
    context: FragmentActivity,
    songs: ArrayList<Song>,
    layoutRes: Int
) : SongAdapter(context, songs, layoutRes) {

    private var dynamicPrimaryTextColor: Int? = null
    private var dynamicSecondaryTextColor: Int? = null

    override fun swapDataSet(dataSet: List<Song>) {
        this.dataSet = dataSet.toMutableList()
        notifyDataSetChanged()
    }

    /**
     * Stores the page's current foreground/secondary colors and re-applies them on
     * every bind in onBindViewHolder below — including rows bound later (recycled
     * views, or rows added afterwards via appendSongs) — instead of a one-off pass
     * over whatever happens to be visible at the moment the color is extracted.
     */
    fun setDynamicTextColors(primaryColor: Int, secondaryColor: Int) {
        dynamicPrimaryTextColor = primaryColor
        dynamicSecondaryTextColor = secondaryColor
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)

        // Track number removed — only the duration is shown now.
        holder.time?.text = MusicUtil.getReadableDurationString(dataSet[position].duration)
        holder.text2?.text = dataSet[position].artistName

        dynamicPrimaryTextColor?.let { holder.title?.setTextColor(it) }
        dynamicSecondaryTextColor?.let { color ->
            holder.text?.setTextColor(color)
            holder.text2?.setTextColor(color)
            holder.time?.setTextColor(color)
            holder.menu?.let { menuView ->
                ImageViewCompat.setImageTintList(menuView, ColorStateList.valueOf(color))
            }
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    // item_song (المستخدم هنا بس) معندوش paletteColorContainer ولا mask، وألوان
    // النصوص جايه أصلاً من setDynamicTextColors فوق (لون الصفحة الديناميكي) -
    // مش من لون الغلاف نفسه. فمفيش داعي نستخرج Palette من كل صورة أغنية
    // ونرميها زي ما كان بيحصل في الكلاس الأساسي SongAdapter.
    override fun loadAlbumCover(song: Song, holder: ViewHolder) {
        if (holder.image == null) return
        Glide.with(activity)
            .asDrawable()
            .songCoverOptions(song)
            .load(RetroGlideExtension.getSongModel(song))
            .into(holder.image!!)
    }
}
