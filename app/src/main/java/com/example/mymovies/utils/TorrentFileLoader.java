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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;

import com.example.mymovies.utils.rutrackerparser.api.ApiFactory;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import okhttp3.ResponseBody;
import retrofit2.Call;

/*
 * Download torrent metadata file by torrent id
 *
 */
public class TorrentFileLoader extends AsyncTaskLoader<File> {

    private static final String POSTFIX = ".torrent";

    private Bundle bundle;
    private OnStartLoadingListener onStartLoadingListener;

    public interface OnStartLoadingListener {
        void onStartLoading();
    }

    public TorrentFileLoader(@NonNull Context context, Bundle bundle) {
        super(context);
        this.bundle = bundle;
    }

    public void setOnStartLoadingListener(TorrentFileLoader.OnStartLoadingListener onStartLoadingListener) {
        this.onStartLoadingListener = onStartLoadingListener;
    }

    @Nullable
    @Override
    public File loadInBackground() {
        if (bundle == null) {
            return null;
        }

        try {
            Call<ResponseBody> call = ApiFactory.getInstance().getApi().downloadTorrentFile(
                    bundle.getLong("torrentId")
            );
            try {
                ResponseBody responseBody = call.execute().body();
                if (responseBody == null) {
                    return null;
                }
                InputStream inputStream = responseBody.byteStream();
                final File tempFile = File.createTempFile(UUID.randomUUID().toString(), POSTFIX);

                tempFile.deleteOnExit();
                try (FileOutputStream out = new FileOutputStream(tempFile)) {
                    IOUtils.copy(inputStream, out);
                }
                return tempFile;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (ImproperlyConfiguredException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        if (onStartLoadingListener != null) {
            onStartLoadingListener.onStartLoading();
        }
    }
}
