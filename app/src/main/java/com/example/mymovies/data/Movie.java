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

package com.example.mymovies.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/*
 * POJO. Movie db model.
 *
 */

@Entity(tableName = "movie")
public class Movie {

    @PrimaryKey(autoGenerate = true)
    private int _db_id;
    private int id;
    private int voteCount;
    private String title;
    private String originalTitle;
    private String overview;
    private String smallPosterPath;
    private String largePosterPath;
    private String backdropPath;
    private double voteAverage;
    private String releaseDate;

    public Movie(int _db_id, int id, int voteCount, String title, String originalTitle, String overview, String smallPosterPath, String largePosterPath, String backdropPath, double voteAverage, String releaseDate) {
        this._db_id = _db_id;
        this.id = id;
        this.voteCount = voteCount;
        this.title = title;
        this.originalTitle = originalTitle;
        this.overview = overview;
        this.smallPosterPath = smallPosterPath;
        this.largePosterPath = largePosterPath;
        this.backdropPath = backdropPath;
        this.voteAverage = voteAverage;
        this.releaseDate = releaseDate;
    }

    @Ignore
    public Movie(int id, int voteCount, String title, String originalTitle, String overview, String smallPosterPath, String largePosterPath, String backdropPath, double voteAverage, String releaseDate) {
        this.id = id;
        this.voteCount = voteCount;
        this.title = title;
        this.originalTitle = originalTitle;
        this.overview = overview;
        this.smallPosterPath = smallPosterPath;
        this.largePosterPath = largePosterPath;
        this.backdropPath = backdropPath;
        this.voteAverage = voteAverage;
        this.releaseDate = releaseDate;
    }

    public int get_db_id() {
        return _db_id;
    }

    public void set_db_id(int _db_id) {
        this._db_id = _db_id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(int voteCount) {
        this.voteCount = voteCount;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public void setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getSmallPosterPath() {
        return smallPosterPath;
    }

    public void setSmallPosterPath(String smallPosterPath) {
        this.smallPosterPath = smallPosterPath;
    }

    public String getLargePosterPath() {
        return largePosterPath;
    }

    public void setLargePosterPath(String largePosterPath) {
        this.largePosterPath = largePosterPath;
    }

    public String getBackdropPath() {
        return backdropPath;
    }

    public void setBackdropPath(String backdropPath) {
        this.backdropPath = backdropPath;
    }

    public double getVoteAverage() {
        return voteAverage;
    }

    public void setVoteAverage(double voteAverage) {
        this.voteAverage = voteAverage;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public String getReleaseYear() {
        String releaseDate = getReleaseDate();
        return releaseDate.substring(0, 4);
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }
}
