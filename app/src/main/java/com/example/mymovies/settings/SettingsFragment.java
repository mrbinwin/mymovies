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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;

import com.takisoft.preferencex.PreferenceFragmentCompat;

import com.example.mymovies.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    private static final String TAG = SettingsFragment.class.getSimpleName();

    private static final String MoviesAPISettings = "MoviesAPISettingsFragment";
    private static final String BehaviorSettings = "BehaviorSettingsFragment";
    private static final String NetworkSettings = "NetworkSettingsFragment";
    private static final String StorageSettings = "StorageSettingsFragment";
    private static final String LimitationsSettings = "LimitationsSettingsFragment";
    private static final String StreamingSettings = "StreamingSettingsFragment";

    private Callback callback;
    private AppCompatActivity activity;

    public interface Callback {
        void onDetailTitleChanged(String title);
    }

    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        fragment.setArguments(new Bundle());
        return fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (activity == null) {
            activity = (AppCompatActivity)getActivity();
        }

        androidx.preference.Preference moviesAPI = findPreference(MoviesAPISettingsFragment.class.getSimpleName());
        moviesAPI.setOnPreferenceClickListener(prefClickListener);

        androidx.preference.Preference behavior = findPreference(BehaviorSettingsFragment.class.getSimpleName());
        behavior.setOnPreferenceClickListener(prefClickListener);

        androidx.preference.Preference network = findPreference(NetworkSettingsFragment.class.getSimpleName());
        network.setOnPreferenceClickListener(prefClickListener);

        androidx.preference.Preference storage = findPreference(StorageSettingsFragment.class.getSimpleName());
        storage.setOnPreferenceClickListener(prefClickListener);

        Preference streaming = findPreference(StreamingSettingsFragment.class.getSimpleName());
        streaming.setOnPreferenceClickListener(prefClickListener);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof AppCompatActivity) {
            activity = (AppCompatActivity)context;
        }
        if (context instanceof Callback) {
            callback = (Callback)context;
        }
    }

    @Override
    public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_headers, rootKey);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callback = null;
    }

    private Preference.OnPreferenceClickListener prefClickListener = (Preference preference) -> {
        openPreference(preference.getKey());
        return true;
    };

    private void openPreference(String prefName) {
        switch (prefName) {
            case MoviesAPISettings:
                startActivity(MoviesAPISettingsFragment.class, getString(R.string.pref_header_backend_api));
                break;
            case BehaviorSettings:
                startActivity(BehaviorSettingsFragment.class, getString(R.string.pref_header_behavior));
                break;
            case NetworkSettings:
                startActivity(NetworkSettingsFragment.class, getString(R.string.pref_header_network));
                break;
            case StorageSettings:
                startActivity(StorageSettingsFragment.class, getString(R.string.pref_header_storage));
                break;
            case StreamingSettings:
                startActivity(StreamingSettingsFragment.class, getString(R.string.pref_header_streaming));
                break;
        }
    }

    private <F extends PreferenceFragmentCompat> void startActivity(Class<F> fragment, String title) {
        Intent i = new Intent(activity, PreferenceActivity.class);
        PreferenceActivityConfig config = new PreferenceActivityConfig(
                fragment.getSimpleName(),
                title);

        i.putExtra(PreferenceActivity.TAG_CONFIG, config);
        startActivity(i);
    }
}
