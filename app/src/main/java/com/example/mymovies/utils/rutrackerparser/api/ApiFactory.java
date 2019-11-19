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

package com.example.mymovies.utils.rutrackerparser.api;

import android.content.SharedPreferences;

import com.example.mymovies.MainApplication;
import com.example.mymovies.R;
import com.example.mymovies.settings.SettingsManager;
import com.example.mymovies.utils.ImproperlyConfiguredException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiFactory {
    private final String baseUrl;
    private final String authUsername;
    private final String authPassword;

    private static ApiFactory apiFactory;
    private static Retrofit retrofit;

    private ApiFactory() throws ImproperlyConfiguredException {

        final SharedPreferences sharedPreferences = SettingsManager.getPreferences(MainApplication.getContext());
        baseUrl = sharedPreferences.getString(
                MainApplication.getContext().getString(R.string.pref_key_backend_url),
                SettingsManager.Default.backendUrl
        );
        authUsername = sharedPreferences.getString(
                MainApplication.getContext().getString(R.string.pref_key_backend_basic_auth_username),
                SettingsManager.Default.backendBasicAuthUsername
        );
        authPassword = sharedPreferences.getString(
                MainApplication.getContext().getString(R.string.pref_key_backend_basic_auth_password),
                SettingsManager.Default.backendBasicAuthPassword
        );

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.authenticator(new Authenticator() {
            @Override
            public Request authenticate(@Nullable Route route, @NotNull Response response) throws IOException {
                Request request = response.request();
                if (request.header("Authorization") != null) {
                    return null;
                }
                return request.newBuilder()
                        .header("Authorization", Credentials.basic(authUsername, authPassword))
                        .build();
            }
        });
        OkHttpClient client = httpClient.build();

        try {
            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            throw new ImproperlyConfiguredException();
        }

    }

    public static ApiFactory getInstance() throws ImproperlyConfiguredException {
        if (apiFactory == null) {
            apiFactory = new ApiFactory();
        }
        return apiFactory;
    }

    public TorrentSearchApi getApi() {
        return retrofit.create(TorrentSearchApi.class);
    }
}
