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

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;

import com.example.mymovies.MainApplication;
import com.example.mymovies.R;
import com.example.mymovies.settings.SettingsManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

/*
 * The class interacts with TMDB API
 *
 */

public class NetworkUtils {

    public static final int SORT_BY_POPULARITY = 1;
    public static final int SORT_BY_RATING = 2;

    private static final String API_DISCOVER_MOVIES_BASE_URL = "https://api.themoviedb.org/3/discover/movie?";
    private static final String API_GET_TRAILERS_BASE_URL = "https://api.themoviedb.org/3/movie/%s/videos?";
    private static final String API_GET_REVIEWS_BASE_URL = "https://api.themoviedb.org/3/movie/%s/reviews?";
    private static final String API_SEARCH_MOVIES_BASE_URL = "https://api.themoviedb.org/3/search/movie?";
    private static final String PARAMS_API_KEY = "api_key";
    private static final String PARAMS_LANGUAGE_KEY = "language";
    private static final String PARAMS_MOVIE_NAME_KEY = "query";
    private static final String PARAMS_SORT_KEY = "sort_by";
    private static final String PARAMS_PAGE_KEY = "page";
    private static final String PARAMS_VOTE_COUNT_KEY = "vote_count.gte";

    private static final String PARAMS_SORT_BY_POPULARITY_VALUE = "popularity.desc";
    private static final String PARAMS_SORT_BY_RATING_VALUE = "vote_average.desc";
    private static final int PARAMS_VOTE_COUNT_VALUE = 100;

    public static URL buildURL(int sortBy, int page, String language, @Nullable String query) {
        if (query != null) {
            return buildURLToSearchByName(query, page, language);
        }
        String methodOfSort;
        if (sortBy == SORT_BY_POPULARITY) {
            methodOfSort = PARAMS_SORT_BY_POPULARITY_VALUE;
        } else {
            methodOfSort = PARAMS_SORT_BY_RATING_VALUE;
        }

        SharedPreferences sharedPreferences = SettingsManager.getPreferences(MainApplication.getContext());
        String apiKey = sharedPreferences.getString(MainApplication.getContext().getString(R.string.pref_key_tmdb_api_key),
                SettingsManager.Default.tmdbApiKey);

        Uri uri = Uri.parse(API_DISCOVER_MOVIES_BASE_URL).buildUpon()
                .appendQueryParameter(PARAMS_API_KEY, apiKey)
                .appendQueryParameter(PARAMS_LANGUAGE_KEY, language)
                .appendQueryParameter(PARAMS_SORT_KEY, methodOfSort)
                .appendQueryParameter(PARAMS_PAGE_KEY, Integer.toString(page))
                .appendQueryParameter(PARAMS_VOTE_COUNT_KEY, Integer.toString(PARAMS_VOTE_COUNT_VALUE))
                .build();
        URL result = null;
        try {
            result = new URL(uri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static URL buildURLToSearchByName(String movieNameQuery, int page, String language) {

        String methodOfSort = PARAMS_SORT_BY_POPULARITY_VALUE;

        SharedPreferences sharedPreferences = SettingsManager.getPreferences(MainApplication.getContext());
        String apiKey = sharedPreferences.getString(MainApplication.getContext().getString(R.string.pref_key_tmdb_api_key),
                SettingsManager.Default.tmdbApiKey);

        Uri uri = Uri.parse(API_SEARCH_MOVIES_BASE_URL).buildUpon()
                .appendQueryParameter(PARAMS_API_KEY, apiKey)
                .appendQueryParameter(PARAMS_LANGUAGE_KEY, language)
                .appendQueryParameter(PARAMS_SORT_KEY, methodOfSort)
                .appendQueryParameter(PARAMS_PAGE_KEY, Integer.toString(page))
                .appendQueryParameter(PARAMS_MOVIE_NAME_KEY, movieNameQuery)
                .build();
        URL result = null;
        try {
            result = new URL(uri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static URL buildURLToTrailers(int movieId, String language) {

        SharedPreferences sharedPreferences = SettingsManager.getPreferences(MainApplication.getContext());
        String apiKey = sharedPreferences.getString(MainApplication.getContext().getString(R.string.pref_key_tmdb_api_key),
                SettingsManager.Default.tmdbApiKey);

        Uri uri = Uri.parse(String.format(API_GET_TRAILERS_BASE_URL, movieId)).buildUpon()
                .appendQueryParameter(PARAMS_API_KEY, apiKey)
                .appendQueryParameter(PARAMS_LANGUAGE_KEY, language)
                .build();
        URL result = null;
        try {
            result = new URL(uri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static JSONObject getJSONTrailers(int movieId, String language) {
        URL url = buildURLToTrailers(movieId, language);
        JSONObject result = null;
        try {
            result = new JSONLoadTask().execute(url).get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static class JSONLoader extends AsyncTaskLoader<JSONObject> {

        private Bundle bundle;
        private OnStartLoadingListener onStartLoadingListener;


        public interface OnStartLoadingListener {
            void onStartLoading();
        }

        public JSONLoader(@NonNull Context context, Bundle bundle) {
            super(context);
            this.bundle = bundle;
        }

        @Nullable
        @Override
        public JSONObject loadInBackground() {
            if (bundle == null) {
                return null;
            }
            String urlAsString = bundle.getString("url");
            URL url = null;
            try {
                url = new URL(urlAsString);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            if (url == null) {
                return null;
            }
            JSONObject result = null;
            HttpURLConnection httpURLConnection = null;
            try {
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setConnectTimeout(10 * 1000);
                httpURLConnection.setReadTimeout(20 * 1000);
                InputStreamReader inputStreamReader = new InputStreamReader(httpURLConnection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                StringBuilder stringBuilder = new StringBuilder();
                String line = bufferedReader.readLine();
                while (line != null) {
                    stringBuilder.append(line);
                    line = bufferedReader.readLine();
                }
                result = new JSONObject(stringBuilder.toString());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
            }
            return result;
        }

        public void setOnStartLoadingListener(OnStartLoadingListener onStartLoadingListener) {
            this.onStartLoadingListener = onStartLoadingListener;
        }

        @Override
        protected void onStartLoading() {
            super.onStartLoading();
            forceLoad();
            if (onStartLoadingListener != null) {
                onStartLoadingListener.onStartLoading();
            }
        }
    }

    private static class JSONLoadTask extends AsyncTask<URL, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(URL... urls) {
            JSONObject result = null;
            if (urls == null || urls.length == 0) {
                return result;
            }
            HttpURLConnection httpURLConnection = null;
            try {
                httpURLConnection = (HttpURLConnection) urls[0].openConnection();
                httpURLConnection.setConnectTimeout(10 * 1000);
                httpURLConnection.setReadTimeout(20 * 1000);
                InputStreamReader inputStreamReader = new InputStreamReader(httpURLConnection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                StringBuilder stringBuilder = new StringBuilder();
                String line = bufferedReader.readLine();
                while (line != null) {
                    stringBuilder.append(line);
                    line = bufferedReader.readLine();
                }
                result = new JSONObject(stringBuilder.toString());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
            }
            return result;
        }
    }


}
