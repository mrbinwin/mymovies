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

import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mymovies.R;
import com.example.mymovies.adapters.MovieTrailerAdapter;
import com.example.mymovies.data.Movie;
import com.example.mymovies.data.MovieFavorite;
import com.example.mymovies.data.MovieFavoriteViewModel;
import com.example.mymovies.data.MovieTrailer;
import com.example.mymovies.data.MovieViewModel;
import com.example.mymovies.utils.JSONUtils;
import com.example.mymovies.utils.NetworkUtils;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

/*
 * A movie details screen
 *
 */
public class MovieDetailActivity extends BaseActivity {

    private ImageView imageViewPoster;
    private ImageView imageViewFavorite;
    private TextView textViewTitle;
    private TextView textViewOriginalTitle;
    private TextView textViewRating;
    private TextView textViewReleaseDate;
    private TextView textViewDescription;
    private RecyclerView recyclerViewTrailers;
    private View viewContainerPlayOnlineStream;

    private String language;
    private MovieTrailerAdapter movieTrailerAdapter;
    private String movieModelType = "movie";

    private Movie movie;
    private MovieViewModel movieViewModel;
    private MovieFavoriteViewModel movieFavoriteViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        language = Locale.getDefault().getLanguage();
        setContentView(R.layout.activity_movie_detail);
        imageViewPoster = findViewById(R.id.imageViewPoster);
        imageViewPoster.setAlpha(0.5f);
        imageViewFavorite = findViewById(R.id.imageViewFavorite);

        textViewTitle = findViewById(R.id.textViewTitle);
        textViewOriginalTitle = findViewById(R.id.textViewOriginalTitle);
        textViewRating = findViewById(R.id.textViewRating);
        textViewReleaseDate = findViewById(R.id.textViewReleaseDate);
        textViewDescription = findViewById(R.id.textViewDescription);
        recyclerViewTrailers = findViewById(R.id.recyclerViewTrailers);
        viewContainerPlayOnlineStream = (View) findViewById(R.id.containerPlayOnlineStream);

        movieViewModel = ViewModelProviders.of(this).get(MovieViewModel.class);
        movieFavoriteViewModel = ViewModelProviders.of(this).get(MovieFavoriteViewModel.class);
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("movieId")) {
            if (intent.hasExtra("movieFavorite") && intent.getBooleanExtra("movieFavorite", false)) {
                MovieFavorite movieFavorite = movieFavoriteViewModel.getById(intent.getIntExtra("movieId", 0));
                movieModelType = "movieFavorite";
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
        Picasso.get().load(movie.getLargePosterPath()).into(imageViewPoster);
        textViewTitle.setText(movie.getTitle());
        textViewOriginalTitle.setText(movie.getOriginalTitle());
        textViewRating.setText(Double.toString(movie.getVoteAverage()));
        textViewReleaseDate.setText(movie.getReleaseDate());
        textViewDescription.setText(movie.getOverview());
        setFavorite();

        recyclerViewTrailers.setLayoutManager(new LinearLayoutManager(this));
        movieTrailerAdapter = new MovieTrailerAdapter();
        JSONObject movieTrailersJSON = NetworkUtils.getJSONTrailers(movie.getId(), language);
        ArrayList<MovieTrailer> movieTrailers = JSONUtils.getTrailersFromJSON(movieTrailersJSON);
        movieTrailerAdapter.setTrailers(movieTrailers);
        recyclerViewTrailers.setAdapter(movieTrailerAdapter);

        movieTrailerAdapter.setOnTrailerClickListener(new MovieTrailerAdapter.OnTrailerClickListener() {
            @Override
            public void onTrailerClick(String url) {
                Intent intentToTrailer = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intentToTrailer);
            }
        });

        viewContainerPlayOnlineStream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentToTrackerList = new Intent(MovieDetailActivity.this, TorrentSearchActivity.class);
                intentToTrackerList.putExtra("movieId", movie.getId());
                intentToTrackerList.putExtra("movieModelType", movieModelType);
                startActivity(intentToTrackerList);
            }
        });
    }

    public void onClickToggleFavorite(View view) {
        if (movieFavoriteViewModel.isFavorited(movie)) {
            movieFavoriteViewModel.delete(new MovieFavorite(movie));
            Toast.makeText(getApplicationContext(), getString(R.string.favorites_removed), Toast.LENGTH_SHORT).show();
        } else {
            movieFavoriteViewModel.insert(new MovieFavorite(movie));
            Toast.makeText(getApplicationContext(), getString(R.string.favorites_added), Toast.LENGTH_SHORT).show();
        }
        setFavorite();
    }

    private void setFavorite() {
        if (movieFavoriteViewModel.isFavorited(movie)) {
            imageViewFavorite.setImageResource(R.drawable.favorite_on);
        } else {
            imageViewFavorite.setImageResource(R.drawable.favorite_off);
        }
    }
}
