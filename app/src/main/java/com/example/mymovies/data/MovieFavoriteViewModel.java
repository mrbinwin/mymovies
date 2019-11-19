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

import android.app.Application;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class MovieFavoriteViewModel extends AndroidViewModel {

    private static MovieDatabase database;
    private LiveData<List<MovieFavorite>> favorites;

    public MovieFavoriteViewModel(@NonNull Application application) {
        super(application);
        database = MovieDatabase.getInstance(getApplication());
        favorites = database.movieFavoriteDao().getAll();
    }

    public MovieFavorite getById(int id) {
        try {
            return new getTask().execute(id).get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public LiveData<List<MovieFavorite>> getAll() {
        return favorites;
    }

    public boolean isFavorited(Movie movie) {
        try {
            MovieFavorite movieFavorite = new getTask().execute(movie.getId()).get();
            if (movieFavorite != null) {
                return true;
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void deleteAll() {
        new DeleteAllTask().execute();
    }

    public void insert(MovieFavorite movieFavorite) {
        new InsertTask().execute(movieFavorite);
    }

    public void delete(MovieFavorite movieFavorite) {
        new DeleteTask().execute(movieFavorite);
    }

    private static class getTask extends AsyncTask<Integer, Void, MovieFavorite> {
        @Override
        protected MovieFavorite doInBackground(Integer... ids) {
            if (ids != null && ids.length > 0) {
                return database.movieFavoriteDao().get(ids[0]);
            }
            return null;
        }
    }

    private static class DeleteAllTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            database.movieFavoriteDao().deleteAll();
            return null;
        }
    }

    private static class InsertTask extends AsyncTask<MovieFavorite, Void, Void> {
        @Override
        protected Void doInBackground(MovieFavorite... movieFavorites) {
            if (movieFavorites != null && movieFavorites.length > 0) {
                database.movieFavoriteDao().insert(movieFavorites[0]);
            }
            return null;
        }
    }

    private static class DeleteTask extends AsyncTask<MovieFavorite, Void, Void> {
        @Override
        protected Void doInBackground(MovieFavorite... movieFavorites) {
            if (movieFavorites != null && movieFavorites.length > 0) {
                database.movieFavoriteDao().delete(movieFavorites[0].getId());
            }
            return null;
        }
    }
}
