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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.example.mymovies.R;
import com.takisoft.preferencex.EditTextPreference;
import com.takisoft.preferencex.PreferenceFragmentCompat;

public class MoviesAPISettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

    public static MoviesAPISettingsFragment newInstance() {
        MoviesAPISettingsFragment fragment = new MoviesAPISettingsFragment();
        fragment.setArguments(new Bundle());
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences sharedPreferences = SettingsManager.getPreferences(getActivity());

        String keyTmdbApiKey = getString(R.string.pref_key_tmdb_api_key);
        EditTextPreference tmdbApiKey = (EditTextPreference) findPreference(keyTmdbApiKey);
        String tmdbApiKeyValue = sharedPreferences.getString(keyTmdbApiKey, SettingsManager.Default.tmdbApiKey);
        tmdbApiKey.setText(tmdbApiKeyValue);
        tmdbApiKey.setSummary(tmdbApiKeyValue);
        bindOnPreferenceChangeListener(tmdbApiKey);

        String keyBackendUrl = getString(R.string.pref_key_backend_url);
        EditTextPreference backendUrl = (EditTextPreference) findPreference(keyBackendUrl);
        String backendUrlValue = sharedPreferences.getString(keyBackendUrl, SettingsManager.Default.backendUrl);
        backendUrl.setText(backendUrlValue);
        if (!TextUtils.isEmpty(backendUrlValue)) {
            backendUrl.setSummary(backendUrlValue);
        } else {
            backendUrl.setSummary(getString(R.string.pref_backend_url_summary));
        }
        bindOnPreferenceChangeListener(backendUrl);

        String keyEnableBackendBasicAuth = getString(R.string.pref_key_enable_backend_basic_auth);
        SwitchPreferenceCompat enableBackendBasicAuth = (SwitchPreferenceCompat)findPreference(keyEnableBackendBasicAuth);
        enableBackendBasicAuth.setChecked(sharedPreferences.getBoolean(keyEnableBackendBasicAuth, SettingsManager.Default.enableBackendBasicAuth));

        String keyBackendBasicAuthUsername = getString(R.string.pref_key_backend_basic_auth_username);
        EditTextPreference backendBasicAuthUsername = (EditTextPreference) findPreference(keyBackendBasicAuthUsername);
        String backendBasicAuthUsernameValue = sharedPreferences.getString(keyBackendBasicAuthUsername, SettingsManager.Default.backendBasicAuthUsername);
        backendBasicAuthUsername.setText(backendBasicAuthUsernameValue);
        backendBasicAuthUsername.setSummary(backendBasicAuthUsernameValue);
        bindOnPreferenceChangeListener(backendBasicAuthUsername);

        String keyBackendBasicAuthPassword = getString(R.string.pref_key_backend_basic_auth_password);
        EditTextPreference backendBasicAuthPassword = (EditTextPreference) findPreference(keyBackendBasicAuthPassword);
        String backendBasicAuthPasswordValue = sharedPreferences.getString(keyBackendBasicAuthPassword, SettingsManager.Default.backendBasicAuthPassword);
        backendBasicAuthPassword.setText(backendBasicAuthPasswordValue);
        backendBasicAuthPassword.setSummary(backendBasicAuthPasswordValue);
        bindOnPreferenceChangeListener(backendBasicAuthPassword);
    }

    @Override
    public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_movies_search_api, rootKey);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.getKey().equals(getString(R.string.pref_key_backend_url))) {
            if (!TextUtils.isEmpty((String)newValue)) {
                preference.setSummary((String)newValue);
            } else {
                preference.setSummary(getString(R.string.pref_backend_url_summary));
            }
        } else if (preference instanceof EditTextPreference) {
            EditTextPreference editTextPreference = (EditTextPreference) findPreference(preference.getKey());
            editTextPreference.setSummary(String.valueOf(newValue));
        }
        return true;
    }

    private void bindOnPreferenceChangeListener(Preference preference) {
        preference.setOnPreferenceChangeListener(this);
    }

}
