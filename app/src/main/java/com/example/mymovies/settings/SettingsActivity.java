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

import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;

import com.example.mymovies.R;
import com.example.mymovies.activities.BaseActivity;

public class SettingsActivity extends BaseActivity {

    private static final String TAG = SettingsActivity.class.getSimpleName();

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.BaseTheme_Settings_Black);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(getString(R.string.settings));
            setSupportActionBar(toolbar);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
}
