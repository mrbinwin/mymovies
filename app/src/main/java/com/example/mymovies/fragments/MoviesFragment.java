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
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.Toast;

import com.example.mymovies.activities.MainActivity;
import com.example.mymovies.adapters.MovieAdapter;
import com.example.mymovies.R;
import com.example.mymovies.activities.FavoriteMoviesActivity;
import com.example.mymovies.activities.MovieDetailActivity;
import com.example.mymovies.data.Movie;
import com.example.mymovies.data.MovieViewModel;
import com.example.mymovies.settings.SettingsActivity;
import com.example.mymovies.settings.SettingsManager;
import com.example.mymovies.utils.JSONUtils;
import com.example.mymovies.utils.NetworkUtils;

import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;


/*
 * A list of movies recived from TMDB API or cached in database
 *
 */
public class MoviesFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<JSONObject> {

    private static final String TAG = MoviesFragment.class.getSimpleName();
    private static final String TAG_QUERY = "query";
    private static final int LOADER_ID = 1;

    private static boolean isLoading = false;
    private static int methodOfSort;
    private static int page = 1;
    private static String movieNameQuery = null;

    private AppCompatActivity activity;
    private MovieAdapter movieAdapter;
    private MovieViewModel movieViewModel;
    private String language;
    private LoaderManager loaderManager;

    ProgressBar progressBarLoading;
    RecyclerView recyclerViewMoviesList;
    Switch switchSort;

    public static MoviesFragment newInstance() {
        MoviesFragment fragment = new MoviesFragment();
        Bundle b = new Bundle();
        fragment.setArguments(b);
        return fragment;
    }

    public void downloadData(int page) {
        URL url = NetworkUtils.buildURL(methodOfSort, page, language, movieNameQuery);
        Bundle bundle = new Bundle();
        bundle.putString("url", url.toString());
        loaderManager.restartLoader(LOADER_ID, bundle, this);
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

        language = Locale.getDefault().getLanguage();
        setHasOptionsMenu(true);

        progressBarLoading = getView().findViewById(R.id.progressBarLoading);
        switchSort = getView().findViewById(R.id.switchSortBy);

        loaderManager = LoaderManager.getInstance(this);
        movieViewModel = ViewModelProviders.of(this).get(MovieViewModel.class);

        movieAdapter.setOnPosterClickListener(new MovieAdapter.OnPosterClickListener() {
            @Override
            public void onPosterClick(int position) {
                Intent intent = new Intent(activity, MovieDetailActivity.class);
                try {
                    Movie movie = movieAdapter.getMovies().get(position);
                    intent.putExtra("movieId", movie.getId());
                    startActivity(intent);
                } catch (ArrayIndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            }
        });
        movieAdapter.setOnReachEndListListener(new MovieAdapter.OnReachEndListListener() {
            @Override
            public void onReachEndList() {
                if (!isLoading) {
                    downloadData(page);
                }
            }
        });
        switchSort.setChecked(false);
        switchSort.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                page = 1;
                setMethodOfSort(isChecked);
            }
        });
        switchSort.setChecked(true);
        LiveData<List<Movie>> moviesLiveData = movieViewModel.getAll();
        moviesLiveData.observe(this, new Observer<List<Movie>>() {
            @Override
            public void onChanged(List<Movie> movies) {
                if (page == 1) {
                    movieAdapter.setMovies(movies);
                }
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof AppCompatActivity) {
            activity = (AppCompatActivity)context;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            movieNameQuery = savedInstanceState.getString(TAG_QUERY);
        }
    }

    @NonNull
    @Override
    public Loader<JSONObject> onCreateLoader(int id, @Nullable Bundle bundle) {
        NetworkUtils.JSONLoader jsonLoader = new NetworkUtils.JSONLoader(activity, bundle);
        jsonLoader.setOnStartLoadingListener(new NetworkUtils.JSONLoader.OnStartLoadingListener() {
            @Override
            public void onStartLoading() {
                isLoading = true;
                progressBarLoading.setVisibility(View.VISIBLE);
            }
        });
        return jsonLoader;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        if (!isAdded()) {
            return;
        }

        inflater.inflate(R.menu.movies, menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_movies, container, false);

        recyclerViewMoviesList = v.findViewById(R.id.recyclerViewMoviesList);
        recyclerViewMoviesList.setLayoutManager(new GridLayoutManager(activity, getColumnCount()));
        movieAdapter = new MovieAdapter();
        recyclerViewMoviesList.setAdapter(movieAdapter);

        return v;
    }

    @Override
    public void onLoadFinished(@NonNull Loader<JSONObject> loader, JSONObject data) {
        if (data == null) {
            Toast.makeText(activity, getString(R.string.error_tmdb_api_key), Toast.LENGTH_SHORT).show();
        }
        ArrayList<Movie> movies = JSONUtils.getMoviesFromJSON(data);
        if (movies != null && !movies.isEmpty()) {
            if (page == 1) {
                movieViewModel.deleteAll();
                movieAdapter.clear();
            }
            for (Movie movie : movies) {
                movieViewModel.insert(movie);
            }
            movieAdapter.addMovies(movies);
        }
        loaderManager.destroyLoader(LOADER_ID);
        page++;
        isLoading = false;
        progressBarLoading.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<JSONObject> loader) {

    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(TAG_QUERY, movieNameQuery);
    }

    public void setData(String movieName, int sort) {
        page = 1;
        movieNameQuery = movieName;
        methodOfSort = sort;

    }

    private int getColumnCount() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = (int) (displayMetrics.widthPixels / displayMetrics.density);
        int result = width / 185;
        return result >= 2 ? result : 2;
    }

    private void setMethodOfSort(boolean isTopRated) {
        if (isTopRated) {
            methodOfSort = NetworkUtils.SORT_BY_POPULARITY;
        } else {
            methodOfSort = NetworkUtils.SORT_BY_RATING;
        }
        downloadData(page);
    }

}
