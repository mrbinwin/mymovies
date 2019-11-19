/*
 * Copyright (C) 2019 MrBinWin (https://github.com/MrBinWin/),
 *                         Dmitry Kuznetsov <mrbinwin@gmail.com>
 *
 * This file is part of MyMovies.
 *
 * MyMovies is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyMovies is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyMovies.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.example.mymovies.adapters;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.mymovies.R;
import com.example.mymovies.fragments.DownloadsFragment;
import com.example.mymovies.fragments.MoviesFragment;

/*
 * The adapter for the main activity.
 * It has 2 tabs: Movies list and Downloads list
 *
 */
public class MainScreenViewPagerAdapter extends ViewPagerAdapter {

    public static final int NUM_FRAGMENTS = 2;
    public static final int MOVIES_FRAGMENT_POS = 0;
    public static final int DOWNLOADS_FRAGMENT_POS = 1;

    public MainScreenViewPagerAdapter(@NonNull FragmentManager fm, Context context) {
        super(null, fm);
        fragmentTitleList.add(context.getString(R.string.main_screen_tabs_movies));
        fragmentTitleList.add(context.getString(R.string.main_screen_tabs_downloads));
    }

    @Override
    public int getCount() {
        return NUM_FRAGMENTS;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case MOVIES_FRAGMENT_POS:
                return MoviesFragment.newInstance();
            case DOWNLOADS_FRAGMENT_POS:
                return DownloadsFragment.newInstance();
            default:
                return null;
        }
    }
}