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
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ProgressBar;

import com.example.mymovies.R;
import com.example.mymovies.adapters.AddTorrentViewPagerAdapter;
import com.example.mymovies.fragments.AddTorrentFilesFragment;
import com.example.mymovies.fragments.AddTorrentInfoFragment;
import com.example.mymovies.libretorrent.core.AddTorrentParams;
import com.example.mymovies.libretorrent.core.TorrentMetaInfo;
import com.example.mymovies.libretorrent.core.exceptions.DecodeException;
import com.example.mymovies.libretorrent.core.utils.FileIOUtils;
import com.example.mymovies.utils.TorrentFileLoader;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import org.libtorrent4j.Priority;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import static com.example.mymovies.services.TorrentTaskService.ACTION_ADD_TORRENT;

/*
 * The activity downloads .torrent file by torrentId from the backend REST API and parses it (meta data).
 * On FloatingActionButton click sends .torrent to downloads list
 *
 */
public class AddTorrentActivity extends BaseActivity {

    private static final int DOWNLOAD_TORRENT_FILE_LOADER_ID = 1;
    private static final String TAG_ADD_TORRENT_PARAMS = "add_torrent_params";
    private static final String TAG_INFO = "torrentMetaInfo";

    private CoordinatorLayout coordinatorLayout;
    private FloatingActionButton floatingActionButtonBuildTorrent;
    private ProgressBar progressBar;
    private TabLayout tabLayout;
    private Toolbar toolbar;
    private ViewPager viewPagerAddTorrent;

    private LoaderManager loaderManager;
    private TorrentFileLoaderCallbacks torrentFileLoaderCallbacks;
    private String torrentFilePath;
    private long torrentId;
    private TorrentMetaInfo torrentMetaInfo;
    private AddTorrentViewPagerAdapter addTorrentViewPagerAdapter;

    class TorrentFileLoaderCallbacks implements LoaderManager.LoaderCallbacks<File> {

        @NonNull
        @Override
        public Loader<File> onCreateLoader(int id, @Nullable Bundle bundle) {
            TorrentFileLoader torrentFileLoader = new TorrentFileLoader(AddTorrentActivity.this, bundle);
            torrentFileLoader.setOnStartLoadingListener(new TorrentFileLoader.OnStartLoadingListener() {
                @Override
                public void onStartLoading() {
                    progressBar.setVisibility(View.VISIBLE);
                    floatingActionButtonBuildTorrent.setEnabled(false);
                    torrentFilePath = "";
                }
            });
            torrentFileLoader.forceLoad();
            return torrentFileLoader;
        }

        @Override
        public void onLoadFinished(@NonNull Loader<File> loader, File file) {
            progressBar.setVisibility(View.GONE);
            if (file.exists() && file.getAbsolutePath() != null) {
                try {
                    torrentMetaInfo = new TorrentMetaInfo(file.getAbsolutePath());
                    floatingActionButtonBuildTorrent.setEnabled(true);
                    torrentFilePath = file.getAbsolutePath();
                    updateInfoFragment();
                    updateFilesFragment();
                } catch (DecodeException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onLoaderReset(@NonNull Loader<File> loader) {

        }
    }

    public void onClickBuildTorrent(View view) {
        AddTorrentInfoFragment infoFragment = (AddTorrentInfoFragment) addTorrentViewPagerAdapter.getFragment(AddTorrentViewPagerAdapter.INFO_FRAGMENT_POS);
        AddTorrentFilesFragment fileFragment = (AddTorrentFilesFragment) addTorrentViewPagerAdapter.getFragment(AddTorrentViewPagerAdapter.FILES_FRAGMENT_POS);
        if (infoFragment == null || torrentMetaInfo == null) {
            return;
        }

        String downloadDir = infoFragment.getDownloadDir();
        String torrentName = infoFragment.getTorrentName();
        if (TextUtils.isEmpty(torrentName)) {
            return;
        }
        boolean sequentialDownload = infoFragment.getIsSequentialDownload();
        boolean startTorrent = infoFragment.getIsStartTorrent();
        Set<Integer> selectedIndexes = null;
        if (fileFragment != null) {
            selectedIndexes = fileFragment.getSelectedFileIndexes();
        }

        if (selectedIndexes == null || selectedIndexes.size() == 0) {
            Snackbar.make(
                    coordinatorLayout,
                    R.string.error_no_files_selected,
                    Snackbar.LENGTH_LONG
            ).show();
            return;
        }

        if (fileFragment != null && (FileIOUtils.getFreeSpace(downloadDir) < fileFragment.getSelectedFileSize())) {
            Snackbar.make(
                    coordinatorLayout,
                    R.string.error_free_space,
                    Snackbar.LENGTH_LONG
            ).show();
            updateInfoFragment();
            return;
        }

        ArrayList<Priority> priorities = null;
        if (torrentMetaInfo.fileCount != 0) {
            if (selectedIndexes != null && selectedIndexes.size() == torrentMetaInfo.fileCount) {
                priorities = new ArrayList<>(Collections.nCopies(torrentMetaInfo.fileCount, Priority.DEFAULT));
            } else {
                priorities = new ArrayList<>(Collections.nCopies(torrentMetaInfo.fileCount, Priority.IGNORE));
                if (selectedIndexes != null) {
                    for (int index : selectedIndexes) {
                        priorities.set(index, Priority.DEFAULT);
                    }
                }
            }
        }

        String source = torrentFilePath;
        AddTorrentParams params = new AddTorrentParams(
                source, false, torrentMetaInfo.sha1Hash,
                torrentName, priorities, downloadDir,
                sequentialDownload, !startTorrent
        );

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra(AddTorrentActivity.TAG_ADD_TORRENT_PARAMS, params);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(ACTION_ADD_TORRENT);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.add_torrent, menu);
        return true;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(TAG_INFO, torrentMetaInfo);
    }

    private void downloadTorrentFile(long id) {
        Bundle bundle = new Bundle();
        bundle.putLong("torrentId", id);
        loaderManager.initLoader(DOWNLOAD_TORRENT_FILE_LOADER_ID, bundle, torrentFileLoaderCallbacks);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_torrent);

        if (savedInstanceState != null) {
            torrentMetaInfo = savedInstanceState.getParcelable(TAG_INFO);
        }

        coordinatorLayout = findViewById(R.id.coordinator_layout);
        floatingActionButtonBuildTorrent = findViewById(R.id.floating_action_button_build_torrent);
        floatingActionButtonBuildTorrent.setEnabled(false);
        progressBar = findViewById(R.id.progress_bar_loading);
        tabLayout = findViewById(R.id.tabs_layout_add_torrent);
        toolbar = findViewById(R.id.toolbar);
        viewPagerAddTorrent = findViewById(R.id.viewpager_add_torrent);

        if (toolbar != null) {
            toolbar.setTitle(getString(R.string.add_torrent_title));
            setSupportActionBar(toolbar);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();
        if (!intent.hasExtra("torrentId")) {
            finish();
            return;
        }
        torrentId = intent.getLongExtra("torrentId", 0);
        if (torrentId == 0) {
            finish();
            return;
        }

        addTorrentViewPagerAdapter = new AddTorrentViewPagerAdapter(getSupportFragmentManager(), getApplicationContext());
        viewPagerAddTorrent.setAdapter(addTorrentViewPagerAdapter);
        viewPagerAddTorrent.setOffscreenPageLimit(AddTorrentViewPagerAdapter.NUM_FRAGMENTS);
        tabLayout.setupWithViewPager(viewPagerAddTorrent);

        if (torrentFileLoaderCallbacks == null) {
            torrentFileLoaderCallbacks = new TorrentFileLoaderCallbacks();
        }
        loaderManager = LoaderManager.getInstance(this);
        downloadTorrentFile(torrentId);
    }

    private synchronized void updateInfoFragment() {
        if (addTorrentViewPagerAdapter == null || torrentMetaInfo == null) {
            return;
        }

        AddTorrentInfoFragment addTorrentInfoFragment = (AddTorrentInfoFragment) addTorrentViewPagerAdapter.getFragment(AddTorrentViewPagerAdapter.INFO_FRAGMENT_POS);
        if (addTorrentInfoFragment == null) {
            return;
        }
        addTorrentInfoFragment.setInfo(torrentMetaInfo);
    }

    private synchronized void updateFilesFragment() {
        if (addTorrentViewPagerAdapter == null || torrentMetaInfo == null) {
            return;
        }

        AddTorrentFilesFragment addTorrentFilesFragment = (AddTorrentFilesFragment) addTorrentViewPagerAdapter.getFragment(AddTorrentViewPagerAdapter.FILES_FRAGMENT_POS);
        if (addTorrentFilesFragment == null) {
            return;
        }
        addTorrentFilesFragment.setFiles(torrentMetaInfo);
    }
}
