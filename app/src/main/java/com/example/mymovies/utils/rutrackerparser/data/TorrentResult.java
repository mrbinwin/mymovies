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

/*
 * Pojo. A torrent info received from REST API backend.
 *
 */
public class TorrentResult {

    public boolean isHeader = false;

    @SerializedName("id")
    @Expose
    private long id;

    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("seeds")
    @Expose
    private int seeders;

    @SerializedName("size")
    @Expose
    private String size;

    public TorrentResult(long id, String name, int seeders, String size) {
        this.id = id;
        this.name = name;
        this.seeders = seeders;
        this.size = size;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSeeders() {
        return seeders;
    }

    public void setSeeders(int seeders) {
        this.seeders = seeders;
    }

    public String getSize() {
        return size;
    }

    public String getShortSize() {
        return getSize().replace(" ", "").replace("↓", "");
    }

    public void setSize(String size) {
        this.size = size;
    }
}
