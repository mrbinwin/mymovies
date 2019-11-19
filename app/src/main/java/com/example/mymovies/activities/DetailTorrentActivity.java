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

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mymovies.R;
import com.example.mymovies.fragments.DetailTorrentFragment;
import com.example.mymovies.fragments.FragmentCallback;
import com.example.mymovies.fragments.IDetailTorrentFragmentCallback;

/*
 * This screen appears on a click of item in downloads list,
 * displays a list of files of the selected torrent
 *
 */
public class DetailTorrentActivity extends AppCompatActivity
        implements
        IDetailTorrentFragmentCallback,
        FragmentCallback {

    @SuppressWarnings("unused")
    private static final String TAG = DetailTorrentActivity.class.getSimpleName();
    public static final String TAG_TORRENT_ID = "torrent_id";

    private DetailTorrentFragment detailTorrentFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_torrent);

        detailTorrentFragment = (DetailTorrentFragment)getSupportFragmentManager()
                .findFragmentById(R.id.detail_torrent_fragmentContainer);
        String id = getIntent().getStringExtra(TAG_TORRENT_ID);
        detailTorrentFragment.setTorrentId(id);
    }

    @Override
    public void onTorrentFilesChanged() {
        if (detailTorrentFragment != null) {
            detailTorrentFragment.onTorrentFilesChanged();
        }
    }

    @Override
    public void openFile(String relativePath) {
        if (detailTorrentFragment != null) {
            detailTorrentFragment.openFile(relativePath);
        }
    }

    @Override
    public void fragmentFinished(Intent intent, ResultCode code) {
        finish();
    }

    @Override
    public void onBackPressed() {
        detailTorrentFragment.onBackPressed();
    }
}
