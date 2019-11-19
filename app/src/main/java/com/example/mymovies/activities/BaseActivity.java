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

package com.example.mymovies.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.example.mymovies.R;
import com.example.mymovies.dialogs.BaseAlertDialog;
import com.example.mymovies.settings.SettingsManager;
import com.example.mymovies.utils.NetworkUtils;
import com.example.mymovies.utils.OptionsMenuHandler;

import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;

public abstract class BaseActivity extends AppCompatActivity
        implements
        BaseAlertDialog.OnClickListener,
        BaseAlertDialog.OnDialogShowListener {

    public static final String TAG_METHOD_OF_SORT = "method_of_sort";
    public static final String TAG_MOVIE_NAME_QUERY = "movie_name";
    private static final String TAG_SEARCH_MOVIES = "search_movies";

    protected static int methodOfSort;
    protected static String movieNameQuery;

    protected SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = SettingsManager.getPreferences(this);
        if (savedInstanceState != null) {
            methodOfSort = savedInstanceState.getInt(TAG_METHOD_OF_SORT);
            movieNameQuery = savedInstanceState.getString(TAG_MOVIE_NAME_QUERY);
        } else {
            methodOfSort = NetworkUtils.SORT_BY_POPULARITY;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search_movies:
                searchMoviesDialog();
                return true;
        }

        return OptionsMenuHandler.handleOptionsItemSelected(this, item);
    }

    @Override
    public void onPositiveClicked(@Nullable View v) {
        if (v == null) {
            return;
        }

        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag(TAG_SEARCH_MOVIES) != null) {
            EditText editTextMovieName = v.findViewById(R.id.text_view_movie_name);
            RadioGroup radioGroup = v.findViewById(R.id.radio_group_method_of_sort);

            if (editTextMovieName.getText().length() > 0) {
                movieNameQuery = editTextMovieName.getText().toString();
            } else {
                movieNameQuery = null;
            }
            int radioButtonId = radioGroup.getCheckedRadioButtonId();
            if (radioButtonId == R.id.radio_sort_by_popularity) {
                methodOfSort = NetworkUtils.SORT_BY_POPULARITY;
            } else {
                methodOfSort = NetworkUtils.SORT_BY_RATING;
            }

            Intent intent = new Intent(this, MainActivity.class);
            intent.setAction(MainActivity.ACTION_SEARCH_MOVIES);
            intent.setFlags(FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra(TAG_MOVIE_NAME_QUERY, movieNameQuery);
            intent.putExtra(TAG_METHOD_OF_SORT, methodOfSort);
            startActivity(intent);
        }
    }

    @Override
    public void onNegativeClicked(@Nullable View v) {

    }

    @Override
    public void onNeutralClicked(@Nullable View v) {
        /* Nothing */
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(TAG_METHOD_OF_SORT, methodOfSort);
        outState.putString(TAG_MOVIE_NAME_QUERY, movieNameQuery);
    }

    @Override
    public void onShow(final AlertDialog dialog) {
        if (dialog != null) {
            FragmentManager fm = getSupportFragmentManager();
            if (fm.findFragmentByTag(TAG_SEARCH_MOVIES) != null) {
                initSearchMoviesDialog(dialog);
            }
        }
    }

    private void searchMoviesDialog() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag(TAG_SEARCH_MOVIES) == null) {
            BaseAlertDialog searchMoviesDialog = BaseAlertDialog.newInstance(
                    getString(R.string.search_movies),
                    null,
                    R.layout.dialog_search_movies,
                    getString(R.string.ok),
                    getString(R.string.cancel),
                    null,
                    this);
            searchMoviesDialog.show(fm, TAG_SEARCH_MOVIES);
        }
    }

    protected void initSearchMoviesDialog(final AlertDialog dialog) {
        EditText movieName = dialog.findViewById(R.id.text_view_movie_name);
        RadioGroup radioGroup = dialog.findViewById(R.id.radio_group_method_of_sort);

        if (movieName != null && radioGroup != null) {
            if (methodOfSort == NetworkUtils.SORT_BY_POPULARITY) {
                radioGroup.check(R.id.radio_sort_by_popularity);
            } else {
                radioGroup.check(R.id.radio_sort_by_rating);
            }
            if (movieNameQuery != null) {
                movieName.setText(movieNameQuery);
            }
        }
    }
}
