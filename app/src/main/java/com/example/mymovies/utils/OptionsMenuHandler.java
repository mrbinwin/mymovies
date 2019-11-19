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

package com.example.mymovies.utils;

import android.content.Intent;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mymovies.R;
import com.example.mymovies.activities.BaseActivity;
import com.example.mymovies.activities.FavoriteMoviesActivity;
import com.example.mymovies.activities.MainActivity;
import com.example.mymovies.fragments.FragmentCallback;
import com.example.mymovies.fragments.BaseFragment;
import com.example.mymovies.settings.SettingsActivity;

import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;

/*
 * A generic handler of options menu items
 *
 */
public class OptionsMenuHandler {
    public static boolean handleOptionsItemSelected(BaseFragment fragment, AppCompatActivity activity, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.downloads_menu:
                Intent intent = new Intent(activity, MainActivity.class);
                intent.setAction(MainActivity.ACTION_SHOW_DOWNLOADS);
                intent.setFlags(FLAG_ACTIVITY_SINGLE_TOP);
                fragment.startActivity(intent);
                break;
            case R.id.favorite_movies_menu:
                fragment.startActivity(new Intent(activity, FavoriteMoviesActivity.class));
                break;
            case R.id.settings_menu:
                fragment.startActivity(new Intent(activity, SettingsActivity.class));
                break;
            case R.id.shutdown_app_menu:
                activity.closeOptionsMenu();
                fragment.finish(new Intent(), FragmentCallback.ResultCode.OK);
                break;
        }
        return true;
    }

    public static boolean handleOptionsItemSelected(BaseActivity activity, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.home: {
                Intent intent = new Intent(activity, MainActivity.class);
                activity.startActivity(intent);
                break;
            }
            case R.id.downloads_menu: {
                Intent intent = new Intent(activity, MainActivity.class);
                intent.setAction(MainActivity.ACTION_SHOW_DOWNLOADS);
                intent.setFlags(FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(intent);
                break;
            }
            case R.id.favorite_movies_menu:
                activity.startActivity(new Intent(activity, FavoriteMoviesActivity.class));
                break;
            case android.R.id.home:
                activity.finish();
                break;
            case R.id.settings_menu:
                activity.startActivity(new Intent(activity, SettingsActivity.class));
                break;
            case R.id.shutdown_app_menu:
                activity.closeOptionsMenu();
                activity.finish();
                break;
        }
        return true;
    }
}
