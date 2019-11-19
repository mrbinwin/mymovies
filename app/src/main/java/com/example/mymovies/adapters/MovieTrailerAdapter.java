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
import com.example.mymovies.data.MovieTrailer;

import java.util.ArrayList;

/*
 * The adapter for a movie trailers RecyclerView
 *
 */
public class MovieTrailerAdapter extends RecyclerView.Adapter<MovieTrailerAdapter.MovieTrailerViewHolder> {

    private OnTrailerClickListener onTrailerClickListener;
    private ArrayList<MovieTrailer> trailers = new ArrayList<>();

    public interface OnTrailerClickListener {
        void onTrailerClick(String url);
    }

    @NonNull
    @Override
    public MovieTrailerViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_trailer, viewGroup, false);
        return new MovieTrailerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieTrailerViewHolder holder, int position) {
        MovieTrailer movieTrailer = trailers.get(position);
        holder.textViewName.setText(movieTrailer.getName());

    }

    @Override
    public int getItemCount() {
        return trailers.size();
    }

    public void setOnTrailerClickListener(OnTrailerClickListener onTrailerClickListener) {
        this.onTrailerClickListener = onTrailerClickListener;
    }

    public void setTrailers(ArrayList<MovieTrailer> trailers) {
        this.trailers = trailers;
        notifyDataSetChanged();
    }

    class MovieTrailerViewHolder extends RecyclerView.ViewHolder {

        private CardView cardView;
        private TextView textViewName;

        public MovieTrailerViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardViewTrailer);
            textViewName = itemView.findViewById(R.id.textViewName);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onTrailerClickListener != null) {
                        onTrailerClickListener.onTrailerClick(trailers.get(getAdapterPosition()).getYoutubeId());
                    }
                }
            });
        }
    }
}
