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

package com.example.mymovies.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymovies.R;
import com.example.mymovies.utils.rutrackerparser.data.TorrentResult;

import java.util.ArrayList;
import java.util.List;

/*
 * The adapter for a list of available torrents received from REST API backend
 *
 */
public class TorrentResultAdapter extends RecyclerView.Adapter<TorrentResultAdapter.TorrentResultViewHolder> {

    private class VIEW_TYPES {
        public static final int Header = 1;
        public static final int Normal = 2;
    }

    private Context context;
    private OnLiveStreamClickListener onLiveStreamClickListener;
    private List<TorrentResult> torrentResults = new ArrayList<>();

    public interface OnLiveStreamClickListener {
        void onLiveStreamClick(int position);
    }

    public TorrentResultAdapter(Context context) {
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        if(torrentResults.get(position).isHeader) {
            return VIEW_TYPES.Header;
        } else {
            return VIEW_TYPES.Normal;
        }
    }

    @NonNull
    @Override
    public TorrentResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case VIEW_TYPES.Header:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_torrent_result_header, parent, false);
                break;
            default:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_torrent_result, parent, false);
                break;
        }
        return new TorrentResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TorrentResultViewHolder holder, int position) {
        TorrentResult torrentResult = torrentResults.get(position);
        if (!torrentResult.isHeader) {
            holder.textViewName.setText(torrentResult.getName());
            holder.textViewSize.setText(torrentResult.getShortSize());
            holder.textViewSeeds.setText(String.valueOf(torrentResult.getSeeders()));
            if (torrentResult.getSeeders() == 0) {
                holder.textViewLiveStream.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return torrentResults.size();
    }

    public List<TorrentResult> getTorrentResults() {
        return torrentResults;
    }

    public void setOnLiveStreamClickListener(OnLiveStreamClickListener onLiveStreamClickListener) {
        this.onLiveStreamClickListener = onLiveStreamClickListener;
    }

    public void setTorrentResults(List<TorrentResult> torrentResults) {
        if (torrentResults.size() > 0) {
            this.torrentResults = new ArrayList<>();
            TorrentResult torrentResultHeader = new TorrentResult(
                    0,
                    context.getString(R.string.torrent_search_result_name),
                    0,
                    context.getString(R.string.torrent_search_result_size)
             );
            torrentResultHeader.isHeader = true;
            this.torrentResults.add(torrentResultHeader);
            this.torrentResults.addAll(torrentResults);
        } else {
            this.torrentResults = torrentResults;
        }
        notifyDataSetChanged();
    }

    class TorrentResultViewHolder extends RecyclerView.ViewHolder {

        private TextView textViewName;
        private TextView textViewSize;
        private TextView textViewSeeds;
        private TextView textViewLiveStream;

        public TorrentResultViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewSize = itemView.findViewById(R.id.textViewSize);
            textViewSeeds = itemView.findViewById(R.id.textViewSeeds);
            textViewLiveStream = itemView.findViewById(R.id.textViewLiveStream);
            if (textViewLiveStream != null) {
                textViewLiveStream.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (onLiveStreamClickListener != null) {
                            onLiveStreamClickListener.onLiveStreamClick(getAdapterPosition());
                        }
                    }
                });
            }
        }
    }
}
