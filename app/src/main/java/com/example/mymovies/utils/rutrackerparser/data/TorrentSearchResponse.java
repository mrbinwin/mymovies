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

package com.example.mymovies.utils.rutrackerparser.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/*
 * Pojo. A list of torrents received from REST API backend.
 *
 */
public class TorrentSearchResponse {
    @SerializedName("success")
    @Expose
    private boolean success;

    @SerializedName("title")
    @Expose
    private String title;

    @SerializedName("original_title")
    @Expose
    private String original_title;

    @SerializedName("year")
    @Expose
    private int year;

    @SerializedName("torrents")
    @Expose
    private ArrayList<TorrentResult> torrents;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOriginal_title() {
        return original_title;
    }

    public void setOriginal_title(String original_title) {
        this.original_title = original_title;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public ArrayList<TorrentResult> getTorrents() {
        return torrents;
    }

    public void setTorrents(ArrayList<TorrentResult> torrents) {
        this.torrents = torrents;
    }
}
