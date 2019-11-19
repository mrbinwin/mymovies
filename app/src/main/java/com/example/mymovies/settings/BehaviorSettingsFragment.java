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

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.example.mymovies.R;
import com.takisoft.preferencex.PreferenceFragmentCompat;

public class BehaviorSettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

    public static BehaviorSettingsFragment newInstance() {
        BehaviorSettingsFragment fragment = new BehaviorSettingsFragment();
        fragment.setArguments(new Bundle());
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences sharedPreferences = SettingsManager.getPreferences(getActivity());

        String keyKeepAlive = getString(R.string.pref_key_keep_alive);
        SwitchPreferenceCompat keepAlive = (SwitchPreferenceCompat)findPreference(keyKeepAlive);
        keepAlive.setChecked(sharedPreferences.getBoolean(keyKeepAlive, SettingsManager.Default.keepAlive));
        bindOnPreferenceChangeListener(keepAlive);

        String keyWifiOnly = getString(R.string.pref_key_wifi_only);
        SwitchPreferenceCompat wifiOnly = (SwitchPreferenceCompat)findPreference(keyWifiOnly);
        wifiOnly.setChecked(sharedPreferences.getBoolean(keyWifiOnly, SettingsManager.Default.wifiOnly));
        bindOnPreferenceChangeListener(wifiOnly);
    }

    @Override
    public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_behavior, rootKey);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final SharedPreferences sharedPreferences = SettingsManager.getPreferences(getActivity());
        return true;
    }

    private void bindOnPreferenceChangeListener(Preference preference) {
        preference.setOnPreferenceChangeListener(this);
    }

}
