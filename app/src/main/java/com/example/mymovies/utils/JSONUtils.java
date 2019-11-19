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

import com.example.mymovies.data.Movie;
import com.example.mymovies.data.MovieReview;
import com.example.mymovies.data.MovieTrailer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/*
 * The class converts json received from TMDB API to POJO
 *
 */
public class JSONUtils {
    private static final String BASE_YOUTUBE_URL = "https://www.youtube.com/watch?v=";
    private static final String KEY_RESULTS = "results";

    private static final String KEY_REVIEW_AUTHOR = "author";
    private static final String KEY_REVIEW_CONTENT = "content";

    private static final String KEY_TRAILER_YOUTUBE_ID = "key";
    private static final String KEY_TRAILER_NAME = "name";

    private static final String KEY_VOTE_COUNT = "vote_count";
    private static final String KEY_ID = "id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_ORIGINAL_TITLE = "original_title";
    private static final String KEY_OVERVIEW = "overview";
    private static final String KEY_POSTER_PATH = "poster_path";
    private static final String KEY_BACKDROP_PATH = "backdrop_path";
    private static final String KEY_VOTE_AVERAGE = "vote_average";
    private static final String KEY_RELEASE_DATE = "release_date";

    private static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/";
    private static final String SMALL_POSTER_SIZE = "w185";
    private static final String LARGE_POSTER_SIZE = "w780";
    private static final String BACKDROP_SIZE = "w780";

    public static ArrayList<Movie> getMoviesFromJSON(JSONObject jsonObject) {
        ArrayList<Movie> result = new ArrayList<>();
        if (jsonObject == null) {
            return result;
        }
        try {
            JSONArray jsonArray = jsonObject.getJSONArray(KEY_RESULTS);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonMovie = jsonArray.getJSONObject(i);
                int voteCount = jsonMovie.getInt(KEY_VOTE_COUNT);
                int id = jsonMovie.getInt(KEY_ID);
                String title = jsonMovie.getString(KEY_TITLE);
                String originalTitle = jsonMovie.getString(KEY_ORIGINAL_TITLE);
                String overview = jsonMovie.getString(KEY_OVERVIEW);
                String smallPosterPath = IMAGE_BASE_URL + SMALL_POSTER_SIZE + jsonMovie.getString(KEY_POSTER_PATH);
                String largePosterPath = IMAGE_BASE_URL + LARGE_POSTER_SIZE + jsonMovie.getString(KEY_POSTER_PATH);
                String backdropPath = IMAGE_BASE_URL + BACKDROP_SIZE + jsonMovie.getString(KEY_BACKDROP_PATH);
                Double voteAverage = jsonMovie.getDouble(KEY_VOTE_AVERAGE);
                String releaseDate = jsonMovie.getString(KEY_RELEASE_DATE);
                Movie movie = new Movie(id, voteCount, title, originalTitle, overview, smallPosterPath, largePosterPath, backdropPath, voteAverage, releaseDate);
                result.add(movie);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static ArrayList<MovieReview> getReviewsFromJSON(JSONObject jsonObject) {
        ArrayList<MovieReview> result = new ArrayList<>();
        if (jsonObject == null) {
            return result;
        }
        try {
            JSONArray jsonArray = jsonObject.getJSONArray(KEY_RESULTS);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonReview = jsonArray.getJSONObject(i);
                String author  = jsonReview.getString(KEY_REVIEW_AUTHOR);
                String content  = jsonReview.getString(KEY_REVIEW_CONTENT);
                MovieReview review = new MovieReview(author, content);
                result.add(review);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static ArrayList<MovieTrailer> getTrailersFromJSON(JSONObject jsonObject) {
        ArrayList<MovieTrailer> result = new ArrayList<>();
        if (jsonObject == null) {
            return result;
        }
        try {
            JSONArray jsonArray = jsonObject.getJSONArray(KEY_RESULTS);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonTrailer = jsonArray.getJSONObject(i);
                String name  = jsonTrailer.getString(KEY_TRAILER_NAME);
                String youtubeId  = BASE_YOUTUBE_URL + jsonTrailer.getString(KEY_TRAILER_YOUTUBE_ID);
                MovieTrailer trailer = new MovieTrailer(name, youtubeId);
                result.add(trailer);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }
}
