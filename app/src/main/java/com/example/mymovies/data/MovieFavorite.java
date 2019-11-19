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

package com.example.mymovies.data;

import androidx.room.Entity;
import androidx.room.Ignore;

/*
 * Favorite movie db model
 *
 */

@Entity(tableName = "favorite")
public class MovieFavorite extends Movie {

    public MovieFavorite(int id, int voteCount, String title, String originalTitle, String overview, String smallPosterPath, String largePosterPath, String backdropPath, double voteAverage, String releaseDate) {
        super(id, voteCount, title, originalTitle, overview, smallPosterPath, largePosterPath, backdropPath, voteAverage, releaseDate);
    }

    @Ignore
    public MovieFavorite(Movie movie) {
        super(
            movie.getId(),
            movie.getVoteCount(),
            movie.getTitle(),
            movie.getOriginalTitle(),
            movie.getOverview(),
            movie.getSmallPosterPath(),
            movie.getLargePosterPath(),
            movie.getBackdropPath(),
            movie.getVoteAverage(),
            movie.getReleaseDate()
        );
    }

    public Movie getMovie() {
        return new Movie(
            this.getId(),
            this.getVoteCount(),
            this.getTitle(),
            this.getOriginalTitle(),
            this.getOverview(),
            this.getSmallPosterPath(),
            this.getLargePosterPath(),
            this.getBackdropPath(),
            this.getVoteAverage(),
            this.getReleaseDate()
        );
    }
}
