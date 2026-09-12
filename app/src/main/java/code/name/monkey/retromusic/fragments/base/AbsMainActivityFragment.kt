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
package code.name.monkey.retromusic.fragments.base

import android.os.Bundle
import android.view.View
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.annotation.LayoutRes
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import code.name.monkey.retromusic.activities.MainActivity
import code.name.monkey.retromusic.fragments.LibraryViewModel
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * Base fragment that attaches to the activity MenuHost. Provides default (no-op)
 * implementations for MenuProvider so concrete fragments are not forced to
 * implement menu callbacks unless they need to.
 */
abstract class AbsMainActivityFragment(@LayoutRes layout: Int) : AbsMusicServiceFragment(layout),
    MenuProvider {
    val libraryViewModel: LibraryViewModel by activityViewModel()

    val mainActivity: MainActivity
        get() = activity as MainActivity

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val menuHost: MenuHost = requireActivity() as MenuHost
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.STARTED)
    }

    // Provide default no-op implementations so subclasses don't have to implement them
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        // no-op
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return false
    }
}
