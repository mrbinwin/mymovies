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

package com.example.mymovies.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.example.mymovies.adapters.MovieAdapter;
import com.example.mymovies.R;
import com.example.mymovies.activities.MovieDetailActivity;
import com.example.mymovies.data.Movie;
import com.example.mymovies.data.MovieFavorite;
import com.example.mymovies.data.MovieFavoriteViewModel;
import com.example.mymovies.settings.SettingsActivity;
import com.example.mymovies.utils.OptionsMenuHandler;

import java.util.ArrayList;
import java.util.List;

/*
 * A list of favorite movies
 *
 */
public class FavoriteMoviesFragment extends BaseFragment {

    private AppCompatActivity activity;
    private MovieAdapter movieAdapter;
    private MovieFavoriteViewModel movieFavoriteViewModel;

    private RecyclerView recyclerViewFavoriteMoviesList;
    private Toolbar toolbar;

    public FavoriteMoviesFragment() {
        // Required empty public constructor
    }

    public void finish(Intent intent, FragmentCallback.ResultCode code) {
        ((FragmentCallback)activity).fragmentFinished(intent, code);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (activity == null) {
            activity = (AppCompatActivity) getActivity();
        }

        toolbar = activity.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(R.string.favorite_movies);
        }
        activity.setSupportActionBar(toolbar);
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setHasOptionsMenu(true);

        movieFavoriteViewModel = ViewModelProviders.of(this).get(MovieFavoriteViewModel.class);
        LiveData<List<MovieFavorite>> favoriteMovies = movieFavoriteViewModel.getAll();
        favoriteMovies.observe(this, new Observer<List<MovieFavorite>>() {
            @Override
            public void onChanged(List<MovieFavorite> favoriteMovies) {
                List<Movie> movies = new ArrayList<>();
                if (favoriteMovies != null) {
                    movies.addAll(favoriteMovies);
                    movieAdapter.setMovies(movies);
                }
            }
        });

        movieAdapter.setOnPosterClickListener(new MovieAdapter.OnPosterClickListener() {
            @Override
            public void onPosterClick(int position) {
                Intent intent = new Intent(activity, MovieDetailActivity.class);
                Movie movie = movieAdapter.getMovies().get(position);
                intent.putExtra("movieId", movie.getId());
                intent.putExtra("movieFavorite", true);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof AppCompatActivity) {
            activity = (AppCompatActivity)context;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        if (!isAdded()) {
            return;
        }

        inflater.inflate(R.menu.favorite_movies, menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_favorite_movies, container, false);

        recyclerViewFavoriteMoviesList = v.findViewById(R.id.recyclerViewFavoriteMoviesList);
        recyclerViewFavoriteMoviesList.setLayoutManager(new GridLayoutManager(activity, getColumnCount()));
        movieAdapter = new MovieAdapter();
        recyclerViewFavoriteMoviesList.setAdapter(movieAdapter);
        return v;
    }

    private int getColumnCount() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = (int) (displayMetrics.widthPixels / displayMetrics.density);
        int result = width / 185;
        return result >= 2 ? result : 2;
    }

}
