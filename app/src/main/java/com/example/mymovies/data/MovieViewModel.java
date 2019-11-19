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

import android.app.Application;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class MovieViewModel extends AndroidViewModel {

    private static MovieDatabase database;
    private LiveData<List<Movie>> movies;

    public MovieViewModel(@NonNull Application application) {
        super(application);
        database = MovieDatabase.getInstance(getApplication());
        movies = database.movieDao().getAll();
    }

    public LiveData<List<Movie>> getAll() {
        return movies;
    }

    public Movie getById(int id) {
        try {
            return new getTask().execute(id).get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void deleteAll() {
        new DeleteAllTask().execute();
    }

    public void insert(Movie movie) {
        new InsertTask().execute(movie);
    }

    public void delete(Movie movie) {
        new DeleteTask().execute(movie);
    }

    private static class getTask extends AsyncTask<Integer, Void, Movie> {
        @Override
        protected Movie doInBackground(Integer... ids) {
            if (ids != null && ids.length > 0) {
                return database.movieDao().get(ids[0]);
            }
            return null;
        }
    }

    private static class DeleteAllTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            database.movieDao().deleteAll();
            return null;
        }
    }

    private static class InsertTask extends AsyncTask<Movie, Void, Void> {
        @Override
        protected Void doInBackground(Movie... movies) {
            if (movies != null && movies.length > 0) {
                database.movieDao().insert(movies[0]);
            }
            return null;
        }
    }

    private static class DeleteTask extends AsyncTask<Movie, Void, Void> {
        @Override
        protected Void doInBackground(Movie... movies) {
            if (movies != null && movies.length > 0) {
                database.movieDao().delete(movies[0]);
            }
            return null;
        }
    }
}
