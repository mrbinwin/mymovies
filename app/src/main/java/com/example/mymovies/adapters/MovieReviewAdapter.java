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

package com.example.mymovies.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymovies.R;
import com.example.mymovies.data.Movie;
import com.example.mymovies.data.MovieReview;

import java.util.ArrayList;

/*
 * The adapter for a movie reviews RecyclerView
 *
 */
public class MovieReviewAdapter extends RecyclerView.Adapter<MovieReviewAdapter.MovieReviewViewHolder> {

    private ArrayList<MovieReview> reviews = new ArrayList<>();

    @NonNull
    @Override
    public MovieReviewAdapter.MovieReviewViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_review, viewGroup, false);
        return new MovieReviewViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    @Override
    public void onBindViewHolder(@NonNull MovieReviewAdapter.MovieReviewViewHolder holder, int position) {
        MovieReview review = reviews.get(position);
        holder.textViewAuthor.setText(review.getAuthor());
        holder.textViewContent.setText(review.getContent());
    }

    public void setReviews(ArrayList<MovieReview> reviews) {
        this.reviews = reviews;
        notifyDataSetChanged();
    }

    class MovieReviewViewHolder extends RecyclerView.ViewHolder {

        private CardView cardView;
        private TextView textViewAuthor;
        private TextView textViewContent;

        public MovieReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardViewReview);
            textViewAuthor = itemView.findViewById(R.id.textViewAuthor);
            textViewContent = itemView.findViewById(R.id.textViewContent);
        }
    }
}
