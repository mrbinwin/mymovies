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
 *
 */

package com.example.mymovies.settings;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.example.mymovies.R;
import com.example.mymovies.activities.BaseActivity;
import com.takisoft.preferencex.PreferenceFragmentCompat;

public class PreferenceActivity extends BaseActivity {
    private static final String TAG = PreferenceActivity.class.getSimpleName();
    public static final String TAG_CONFIG = "config";
    private Toolbar toolbar;

    public <F extends PreferenceFragmentCompat> void setFragment(F fragment) {
        if (fragment == null) {
            return;
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.BaseTheme_Settings_Black);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preference);

        String fragment = null;
        String title = null;

        Intent intent = getIntent();
        if (intent.hasExtra(TAG_CONFIG)) {
            PreferenceActivityConfig config = intent.getParcelableExtra(TAG_CONFIG);
            fragment = config.getFragment();
            title = config.getTitle();
        }

        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            if (title != null) {
                toolbar.setTitle(title);
            }
            setSupportActionBar(toolbar);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (fragment != null && savedInstanceState == null) {
            setFragment(getFragment(fragment));
        }
    }

    private <F extends PreferenceFragmentCompat> F getFragment(String fragment) {
        if (fragment != null) {
            if (fragment.equals(MoviesAPISettingsFragment.class.getSimpleName()))
                return (F) MoviesAPISettingsFragment.newInstance();
            else if (fragment.equals(BehaviorSettingsFragment.class.getSimpleName()))
                return (F) BehaviorSettingsFragment.newInstance();
            else if (fragment.equals(StorageSettingsFragment.class.getSimpleName()))
                return (F) StorageSettingsFragment.newInstance();
            else if (fragment.equals(StreamingSettingsFragment.class.getSimpleName()))
                return (F) StreamingSettingsFragment.newInstance();
            else if (fragment.equals(NetworkSettingsFragment.class.getSimpleName()))
                return (F) NetworkSettingsFragment.newInstance();
            else if (fragment.equals(ProxySettingsFragment.class.getSimpleName()))
                return (F) ProxySettingsFragment.newInstance();
            /*else if (fragment.equals(LimitationsSettingsFragment.class.getSimpleName()))
                return (F) LimitationsSettingsFragment.newInstance();
            */
            else
                return null;
        }
        return null;
    }

}
