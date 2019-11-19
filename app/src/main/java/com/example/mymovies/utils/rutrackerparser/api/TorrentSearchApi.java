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

package com.example.mymovies.utils.rutrackerparser.api;

import com.example.mymovies.utils.rutrackerparser.data.TorrentSearchResponse;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface TorrentSearchApi {

    /*
     * Get list of torrents by movie title in Russian, original title and release
     *
     */
    @GET("search/{title}/{original_title}/{year}")
    public Call<TorrentSearchResponse> searchTorrents(
            @Path("title") String title,
            @Path("original_title") String original_title,
            @Path("year") int year
    );

    /*
     * Download torrent metadata file by torrent id
     *
     */
    @GET("download/{id}")
    Call<ResponseBody> downloadTorrentFile(
            @Path("id") long id
    );
}
