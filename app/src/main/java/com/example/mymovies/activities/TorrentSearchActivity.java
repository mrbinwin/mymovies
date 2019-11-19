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

package com.example.mymovies.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProviders;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mymovies.R;
import com.example.mymovies.adapters.TorrentResultAdapter;
import com.example.mymovies.data.Movie;
import com.example.mymovies.data.MovieFavorite;
import com.example.mymovies.data.MovieFavoriteViewModel;
import com.example.mymovies.data.MovieViewModel;
import com.example.mymovies.utils.TorrentSearchLoader;
import com.example.mymovies.utils.rutrackerparser.data.TorrentResult;
import com.example.mymovies.utils.rutrackerparser.data.TorrentSearchResponse;

/*
 * This activity receives and displays a list of available torrents from REST API for the current movie
 *
 */
public class TorrentSearchActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<TorrentSearchResponse> {

    private static final int LOADER_ID = 1;

    private static boolean isLoading = true;

    private LoaderManager loaderManager;
    private Movie movie;
    private MovieViewModel movieViewModel;
    private MovieFavoriteViewModel movieFavoriteViewModel;
    private TorrentResultAdapter torrentResultAdapter;

    private ProgressBar progressBarLoading;
    private RecyclerView recyclerViewTorrentResults;
    private TextView textViewName;
    private Toolbar toolbar;


    @NonNull
    @Override
    public Loader<TorrentSearchResponse> onCreateLoader(int id, @Nullable Bundle bundle) {
        TorrentSearchLoader torrentSearchLoader = new TorrentSearchLoader(this, bundle);
        torrentSearchLoader.setOnStartLoadingListener(new TorrentSearchLoader.OnStartLoadingListener() {
            @Override
            public void onStartLoading() {
                isLoading = true;
                progressBarLoading.setVisibility(View.VISIBLE);
            }
        });
        torrentSearchLoader.forceLoad();
        return torrentSearchLoader;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.tracker_list, menu);
        return true;
    }

    @Override
    public void onLoaderReset(@NonNull Loader<TorrentSearchResponse> loader) {

    }

    @Override
    public void onLoadFinished(@NonNull Loader<TorrentSearchResponse> loader, TorrentSearchResponse torrentSearchResponse) {
        isLoading = false;
        progressBarLoading.setVisibility(View.INVISIBLE);

        if (torrentSearchResponse == null) {
            loaderManager.destroyLoader(LOADER_ID);
            Toast.makeText(this, getText(R.string.torrent_search_error), Toast.LENGTH_SHORT).show();
        } else {
            if (torrentSearchResponse.getTorrents().size() == 0) {
                Toast.makeText(this, getText(R.string.torrent_search_no_results), Toast.LENGTH_SHORT).show();
            } else {
                torrentResultAdapter.setTorrentResults(torrentSearchResponse.getTorrents());
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_torrent_search);

        progressBarLoading = findViewById(R.id.progressBarLoading);
        textViewName = findViewById(R.id.textViewName);

        recyclerViewTorrentResults = findViewById(R.id.recyclerViewTorrentResults);
        torrentResultAdapter = new TorrentResultAdapter(this);
        recyclerViewTorrentResults.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewTorrentResults.setAdapter(torrentResultAdapter);

        torrentResultAdapter.setOnLiveStreamClickListener(new TorrentResultAdapter.OnLiveStreamClickListener() {
            @Override
            public void onLiveStreamClick(int position) {
                Intent intent = new Intent(TorrentSearchActivity.this, AddTorrentActivity.class);
                intent.putExtra("movieTitle", movie.getTitle());
                intent.putExtra("movieOriginalTitle", movie.getOriginalTitle());
                intent.putExtra("movieReleaseYear", Integer.valueOf(movie.getReleaseYear()));
                TorrentResult torrentResult = torrentResultAdapter.getTorrentResults().get(position);
                intent.putExtra("torrentId", torrentResult.getId());
                intent.putExtra("torrentName", torrentResult.getName());
                startActivity(intent);
            }
        });

        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(getString(R.string.torrent_search_title));
            setSupportActionBar(toolbar);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        loaderManager = LoaderManager.getInstance(this);

        movieViewModel = ViewModelProviders.of(this).get(MovieViewModel.class);
        movieFavoriteViewModel = ViewModelProviders.of(this).get(MovieFavoriteViewModel.class);
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("movieId")) {
            if (intent.hasExtra("movieFavorite") && intent.getBooleanExtra("movieFavorite", false)) {
                MovieFavorite movieFavorite = movieFavoriteViewModel.getById(intent.getIntExtra("movieId", 0));
                movie = movieFavorite.getMovie();
            } else {
                movie = movieViewModel.getById(intent.getIntExtra("movieId", 0));
            }
            if (movie == null) {
                finish();
                return;
            }
        } else {
            finish();
            return;
        }

        textViewName.setText(String.format("%s (%s)", movie.getTitle(), movie.getReleaseYear()));
        searchTorrents(movie);
    }

    private void searchTorrents(Movie movie) {
        Bundle bundle = new Bundle();
        bundle.putString("movieTitle", movie.getTitle());
        bundle.putString("movieOriginalTitle", movie.getOriginalTitle());
        bundle.putInt("movieReleaseYear", Integer.valueOf(movie.getReleaseYear()));
        loaderManager.initLoader(LOADER_ID, bundle, this);
    }
}
