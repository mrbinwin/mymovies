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
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;

import com.example.mymovies.MainApplication;
import com.example.mymovies.R;
import com.example.mymovies.utils.rutrackerparser.api.ApiFactory;
import com.example.mymovies.utils.rutrackerparser.data.TorrentSearchResponse;
import java.io.IOException;
import retrofit2.Call;

/*
 * Get list of torrents by movie title in Russian, original title and release
 *
 */
public class TorrentSearchLoader extends AsyncTaskLoader<TorrentSearchResponse> {

    private Bundle bundle;
    private OnStartLoadingListener onStartLoadingListener;

    public interface OnStartLoadingListener {
        void onStartLoading();
    }

    public TorrentSearchLoader(@NonNull Context context, Bundle bundle) {
        super(context);
        this.bundle = bundle;
    }

    public void setOnStartLoadingListener(OnStartLoadingListener onStartLoadingListener) {
        this.onStartLoadingListener = onStartLoadingListener;
    }

    @Nullable
    @Override
    public TorrentSearchResponse loadInBackground() {
        if (bundle == null) {
            return null;
        }

        TorrentSearchResponse torrentSearchResponse = null;
        try {
            Call<TorrentSearchResponse> call = ApiFactory.getInstance().getApi().searchTorrents(
                    TorrentSearchLoader.removeNonAlphanumeric(bundle.getString("movieTitle")),
                    TorrentSearchLoader.removeNonAlphanumeric(bundle.getString("movieOriginalTitle")),
                    bundle.getInt("movieReleaseYear")
            );
            try {
                torrentSearchResponse = call.execute().body();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (ImproperlyConfiguredException e) {
            e.printStackTrace();
        }
        return torrentSearchResponse;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        if (onStartLoadingListener != null) {
            onStartLoadingListener.onStartLoading();
        }
    }

    private static String removeNonAlphanumeric(@Nullable String string) {
        return string == null ? null : string.replaceAll("[^A-Za-zА-Яа-я0-9_]", " ");
    }
}
