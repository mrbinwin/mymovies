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

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;

import com.example.mymovies.R;
import com.example.mymovies.adapters.MainScreenViewPagerAdapter;
import com.example.mymovies.fragments.MoviesFragment;
import com.example.mymovies.libretorrent.core.AddTorrentParams;
import com.example.mymovies.libretorrent.core.utils.Utils;
import com.example.mymovies.fragments.FragmentCallback;
import com.example.mymovies.receivers.NotificationReceiver;
import com.example.mymovies.services.TorrentTaskService;
import com.example.mymovies.settings.SettingsManager;
import com.example.mymovies.utils.NetworkUtils;
import com.google.android.material.tabs.TabLayout;

import org.jetbrains.annotations.NotNull;

import static com.example.mymovies.services.TorrentTaskService.ACTION_ADD_TORRENT;
import static com.example.mymovies.services.TorrentTaskService.TAG_ADD_TORRENT_PARAMS;

/*
 * The activity contains 2 fragments:
 * - movies search
 * - downloads list
 *
 */
public class MainActivity extends BaseActivity implements FragmentCallback {

    public static final String ACTION_SHOW_DOWNLOADS = "com.example.mymovies.activities.MainActivity.ACTION_SHOW_DOWNLOADS";
    public static final String ACTION_SEARCH_MOVIES = "com.example.mymovies.activities.MainActivity.ACTION_SEARCH_MOVIES";

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final String TAG_PERM_DIALOG_IS_SHOW = "perm_dialog_is_show";

    private TabLayout tabLayout;
    private Toolbar toolbar;
    private ViewPager viewPagerMainScreen;

    private MainScreenViewPagerAdapter mainScreenViewPagerAdapter;
    private boolean permDialogIsShow = false;

    @Override
    public void fragmentFinished(Intent intent, ResultCode code) {
        switch (code) {
            case OK:
                Intent i = new Intent(getApplicationContext(), TorrentTaskService.class);
                i.setAction(TorrentTaskService.ACTION_SHUTDOWN);
                startService(i);
                finish();
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        SharedPreferences pref = SettingsManager.getPreferences(this);
        if (isFinishing() && !pref.getBoolean(getString(R.string.pref_key_keep_alive), SettingsManager.Default.keepAlive)) {
            Intent i = new Intent(getApplicationContext(), TorrentTaskService.class);
            i.setAction(TorrentTaskService.ACTION_SHUTDOWN);
            startService(i);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tabLayout = findViewById(R.id.tabs_layout_main_screen);
        toolbar = findViewById(R.id.toolbar);
        viewPagerMainScreen = findViewById(R.id.viewpager_main_screen);

        if (savedInstanceState != null) {
            permDialogIsShow = savedInstanceState.getBoolean(TAG_PERM_DIALOG_IS_SHOW);
        }

        if (!permDialogIsShow && !Utils.checkStoragePermission(getApplicationContext())) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, 1);
        }
        startService(new Intent(this, TorrentTaskService.class));

        mainScreenViewPagerAdapter = new MainScreenViewPagerAdapter(getSupportFragmentManager(), getApplicationContext());
        viewPagerMainScreen.setAdapter(mainScreenViewPagerAdapter);
        viewPagerMainScreen.setOffscreenPageLimit(MainScreenViewPagerAdapter.NUM_FRAGMENTS);
        tabLayout.setupWithViewPager(viewPagerMainScreen);

        toolbar.setTitle(R.string.app_name);
        invalidateFragmentMenus(MainScreenViewPagerAdapter.MOVIES_FRAGMENT_POS);

        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        if (intent.getAction() != null) {
            if (intent.getAction().equals(NotificationReceiver.NOTIFY_ACTION_SHUTDOWN_APP)) {
                finish();
                return;
            }
            if (intent.getAction().equals(ACTION_SHOW_DOWNLOADS)) {
                viewPagerMainScreen.setCurrentItem(MainScreenViewPagerAdapter.DOWNLOADS_FRAGMENT_POS);
            } else if (intent.getAction().equals(ACTION_SEARCH_MOVIES)) {
                String movieNameQuery = intent.getStringExtra(BaseActivity.TAG_MOVIE_NAME_QUERY);
                int methodOfSort = intent.getIntExtra(BaseActivity.TAG_METHOD_OF_SORT, NetworkUtils.SORT_BY_POPULARITY);
                mainScreenViewPagerAdapter.instantiateItem(viewPagerMainScreen, MainScreenViewPagerAdapter.MOVIES_FRAGMENT_POS);
                MoviesFragment moviesFragment = (MoviesFragment) mainScreenViewPagerAdapter.getFragment(MainScreenViewPagerAdapter.MOVIES_FRAGMENT_POS);
                if (moviesFragment == null) {
                    return;
                }
                viewPagerMainScreen.setCurrentItem(MainScreenViewPagerAdapter.MOVIES_FRAGMENT_POS);
                moviesFragment.setData(movieNameQuery, methodOfSort);
            } else if (intent.getAction().equals(ACTION_ADD_TORRENT) && savedInstanceState == null) {
                AddTorrentParams params = intent.getParcelableExtra(TAG_ADD_TORRENT_PARAMS);
                if (params != null) {
                    Intent i = new Intent(getApplicationContext(), TorrentTaskService.class);
                    i.setAction(TorrentTaskService.ACTION_ADD_TORRENT);
                    i.putExtra(TorrentTaskService.TAG_ADD_TORRENT_PARAMS, params);
                    i.putExtra(TorrentTaskService.TAG_SAVE_TORRENT_FILE, true);
                    startService(i);
                    viewPagerMainScreen.setCurrentItem(MainScreenViewPagerAdapter.DOWNLOADS_FRAGMENT_POS);
                }
            }
        }

        viewPagerMainScreen.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case MainScreenViewPagerAdapter.MOVIES_FRAGMENT_POS:
                        toolbar.setTitle(R.string.app_name);
                        break;
                    case MainScreenViewPagerAdapter.DOWNLOADS_FRAGMENT_POS:
                        toolbar.setTitle(R.string.downloads_title);
                        break;
                }
                invalidateFragmentMenus(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getAction() != null) {
            if (intent.getAction().equals(ACTION_SHOW_DOWNLOADS)) {
                viewPagerMainScreen.setCurrentItem(MainScreenViewPagerAdapter.DOWNLOADS_FRAGMENT_POS);
            } else if (intent.getAction().equals(ACTION_SEARCH_MOVIES)) {
                String movieNameQuery = intent.getStringExtra(BaseActivity.TAG_MOVIE_NAME_QUERY);
                int methodOfSort = intent.getIntExtra(BaseActivity.TAG_METHOD_OF_SORT, NetworkUtils.SORT_BY_POPULARITY);
                mainScreenViewPagerAdapter.instantiateItem(viewPagerMainScreen, MainScreenViewPagerAdapter.MOVIES_FRAGMENT_POS);
                MoviesFragment moviesFragment = (MoviesFragment) mainScreenViewPagerAdapter.getFragment(MainScreenViewPagerAdapter.MOVIES_FRAGMENT_POS);
                if (moviesFragment == null) {
                    return;
                }
                viewPagerMainScreen.setCurrentItem(MainScreenViewPagerAdapter.MOVIES_FRAGMENT_POS);
                moviesFragment.setData(movieNameQuery, methodOfSort);
                moviesFragment.downloadData(1);
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(TAG_PERM_DIALOG_IS_SHOW, permDialogIsShow);
    }

    private void invalidateFragmentMenus(int position) {
        for(int i = 0; i < mainScreenViewPagerAdapter.getCount(); i++){
            mainScreenViewPagerAdapter.getItem(i).setHasOptionsMenu(i == position);
        }
    }
}
