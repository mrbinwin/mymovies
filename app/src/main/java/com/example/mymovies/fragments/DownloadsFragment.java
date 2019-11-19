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
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymovies.R;
import com.example.mymovies.activities.DetailTorrentActivity;
import com.example.mymovies.adapters.TorrentListAdapter;
import com.example.mymovies.adapters.TorrentListItem;
import com.example.mymovies.customviews.EmptyRecyclerView;
import com.example.mymovies.customviews.RecyclerViewDividerDecoration;
import com.example.mymovies.dialogs.BaseAlertDialog;
import com.example.mymovies.libretorrent.core.Torrent;
import com.example.mymovies.libretorrent.core.TorrentHelper;
import com.example.mymovies.libretorrent.core.TorrentStateMsg;
import com.example.mymovies.libretorrent.core.sorting.TorrentSortingComparator;
import com.example.mymovies.libretorrent.core.stateparcel.BasicStateParcel;
import com.example.mymovies.libretorrent.core.utils.Utils;
import com.example.mymovies.receivers.TorrentTaskServiceReceiver;
import com.example.mymovies.settings.SettingsManager;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/*
 * A list of downloads
 *
 */
public class DownloadsFragment extends BaseFragment
        implements
        BaseAlertDialog.OnClickListener,
        BaseAlertDialog.OnDialogShowListener {

    private static final String TAG = DownloadsFragment.class.getSimpleName();
    private static final String TAG_PREV_IMPL_INTENT = "prev_impl_intent";
    private static final String TAG_SELECTABLE_ADAPTER = "selectable_adapter";
    private static final String TAG_SELECTED_TORRENTS = "selected_torrents";
    private static final String TAG_IN_ACTION_MODE = "in_action_mode";
    private static final String TAG_DELETE_TORRENT_DIALOG = "delete_torrent_dialog";
    private static final String TAG_ERROR_OPEN_TORRENT_FILE_DIALOG = "error_open_torrent_file_dialog";
    private static final String TAG_SAVE_ERROR_DIALOG = "save_error_dialog";
    private static final String TAG_TORRENTS_LIST_STATE = "torrents_list_state";
    private static final String TAG_TORRENT_SORTING = "torrent_sorting";

    private ActionMode actionMode;
    private ActionModeCallback actionModeCallback = new ActionModeCallback();
    private AppCompatActivity activity;
    private TorrentListAdapter adapter;
    private boolean inActionMode = false;
    private LinearLayoutManager layoutManager;
    private EmptyRecyclerView recyclerViewTorrentList;
    private ArrayList<String> selectedTorrents = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    private TextView textViewTorrentListEmpty;
    private Parcelable torrentsListState;

    public static DownloadsFragment newInstance() {
        DownloadsFragment fragment = new DownloadsFragment();
        Bundle b = new Bundle();
        fragment.setArguments(b);
        return fragment;
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
        setHasOptionsMenu(true);

        sharedPreferences = SettingsManager.getPreferences(activity);
        recyclerViewTorrentList = getView().findViewById(R.id.recycler_view_torrent_list);
        textViewTorrentListEmpty = getView().findViewById(R.id.text_view_torrent_list_empty);

        layoutManager = new LinearLayoutManager(activity);
        recyclerViewTorrentList.setLayoutManager(layoutManager);

        /*
         * A RecyclerView by default creates another copy of the ViewHolder in order to
         * fade the views into each other. This causes the problem because the old ViewHolder gets
         * the payload but then the new one doesn't. So needs to explicitly tell it to reuse the old one.
         */
        DefaultItemAnimator animator = new DefaultItemAnimator() {
            @Override
            public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
                return true;
            }
        };

        TypedArray a = activity.obtainStyledAttributes(new TypedValue().data, new int[]{ R.attr.divider });
        recyclerViewTorrentList.setItemAnimator(animator);
        recyclerViewTorrentList.addItemDecoration(new RecyclerViewDividerDecoration(a.getDrawable(0)));

        recyclerViewTorrentList.setEmptyView(textViewTorrentListEmpty);

        adapter = new TorrentListAdapter(new HashMap<>(), activity, R.layout.item_torrent_list, torrentListListener,
                new TorrentSortingComparator(Utils.getTorrentSorting(activity.getApplicationContext())));
        recyclerViewTorrentList.setAdapter(adapter);

        if (savedInstanceState != null) {
            selectedTorrents = savedInstanceState.getStringArrayList(TAG_SELECTED_TORRENTS);
            if (savedInstanceState.getBoolean(TAG_IN_ACTION_MODE, false)) {
                actionMode = activity.startActionMode(actionModeCallback);
                adapter.setSelectedItems(savedInstanceState.getIntegerArrayList(TAG_SELECTABLE_ADAPTER));
                actionMode.setTitle(String.valueOf(adapter.getSelectedItemCount()));
            }
        }

    }

    @Override
    public void onAttach(Context context) {
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

        inflater.inflate(R.menu.downloads, menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_downloads, container, false);
        return v;
    }

    @Override
    public void onPositiveClicked(@Nullable View v) {
        if (v == null) {
            return;
        }

        FragmentManager fm = getFragmentManager();
        if (fm == null) {
            return;
        }

        if (fm.findFragmentByTag(TAG_DELETE_TORRENT_DIALOG) != null) {
            CheckBox withFiles = v.findViewById(R.id.dialog_delete_torrent_with_downloaded_files);
            TorrentHelper.deleteTorrents(
                    activity.getApplicationContext(),
                    selectedTorrents,
                    withFiles.isChecked()
            );
            selectedTorrents.clear();

        }
    }

    @Override
    public void onNegativeClicked(@Nullable View v) {
        FragmentManager fm = getFragmentManager();
        if (fm == null) {
            return;
        }
        if (fm.findFragmentByTag(TAG_DELETE_TORRENT_DIALOG) != null) {
            selectedTorrents.clear();
        }
    }

    @Override
    public void onNeutralClicked(@Nullable View v) {
        /* Nothing */
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putIntegerArrayList(TAG_SELECTABLE_ADAPTER, adapter.getSelectedItems());
        outState.putBoolean(TAG_IN_ACTION_MODE, inActionMode);
        outState.putStringArrayList(TAG_SELECTED_TORRENTS, selectedTorrents);
        torrentsListState = layoutManager.onSaveInstanceState();
        outState.putParcelable(TAG_TORRENTS_LIST_STATE, torrentsListState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onShow(final AlertDialog dialog) {
        if (dialog != null) {
            FragmentManager fm = getFragmentManager();
            if (fm == null) {
                return;
            }
            if (fm.findFragmentByTag(TAG_TORRENT_SORTING) != null) {
                //initTorrentSortingDialog(dialog);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        fetchStates();
        if (!TorrentTaskServiceReceiver.getInstance().isRegistered(serviceReceiver))
            TorrentTaskServiceReceiver.getInstance().register(serviceReceiver);

    }

    @Override
    public void onStop() {
        super.onStop();
        TorrentTaskServiceReceiver.getInstance().unregister(serviceReceiver);
    }

    private class ActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu)
        {
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            inActionMode = true;
            mode.getMenuInflater().inflate(R.menu.torrent_list_action_mode, menu);
            Utils.showActionModeStatusBar(activity, true);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            ArrayList<Integer> indexes = adapter.getSelectedItems();

            switch (item.getItemId()) {
                case R.id.delete_torrent_menu:
                    mode.finish();

                    FragmentManager fm = getFragmentManager();
                    if (fm != null && fm.findFragmentByTag(TAG_DELETE_TORRENT_DIALOG) == null) {
                        BaseAlertDialog deleteTorrentDialog = BaseAlertDialog.newInstance(
                                getString(R.string.deleting),
                                (indexes.size() > 1 ?
                                        getString(R.string.delete_selected_torrents) :
                                        getString(R.string.delete_selected_torrent)),
                                R.layout.dialog_delete_torrent,
                                getString(R.string.ok),
                                getString(R.string.cancel),
                                null,
                                DownloadsFragment.this);

                        deleteTorrentDialog.show(fm, TAG_DELETE_TORRENT_DIALOG);
                    }

                    break;
                case R.id.select_all_torrent_menu:
                    for (int i = 0; i < adapter.getItemCount(); i++) {
                        if (adapter.isSelected(i))
                            continue;

                        onItemSelected(adapter.getItem(i).torrentId, i);
                    }

                    break;
                case R.id.force_recheck_torrent_menu:
                    mode.finish();
                    TorrentHelper.forceRecheckTorrents(selectedTorrents);
                    selectedTorrents.clear();
                    break;
                case R.id.force_announce_torrent_menu:
                    mode.finish();
                    TorrentHelper.forceAnnounceTorrents(selectedTorrents);
                    selectedTorrents.clear();
                    break;
            }

            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            adapter.clearSelection();
            actionMode = null;
            inActionMode = false;
            Utils.showActionModeStatusBar(activity, false);
        }
    }

    private void fetchStates() {
        Bundle states = TorrentHelper.makeBasicStatesList();
        List<TorrentListItem> items = statesToItems(states);
        if (items.isEmpty()) {
            states = TorrentHelper.makeOfflineStatesList(activity.getApplicationContext());
            items = statesToItems(states);
        }
        reloadAdapter(items);
    }

    private void handleBasicStates(Bundle states) {
        if (states == null) {
            return;
        }
        reloadAdapter(statesToItems(states));
    }

    private void onItemSelected(String id, int position) {
        toggleSelection(position);
        if (selectedTorrents.contains(id)) {
            selectedTorrents.remove(id);
        } else {
            selectedTorrents.add(id);
        }
    }

    private synchronized void reloadAdapter(final List<TorrentListItem> items) {
        adapter.clearAll();
        if (items == null || items.size() == 0) {
            adapter.notifyDataSetChanged();
        }
        else {
            adapter.addItems(items);
        }
    }

    private List<TorrentListItem> statesToItems(Bundle states) {
        List<TorrentListItem> items = new ArrayList<>();
        for (String key : states.keySet()) {
            BasicStateParcel state = states.getParcelable(key);
            if (state != null) {
                items.add(new TorrentListItem(state));
            }
        }
        return items;
    }



    private TorrentTaskServiceReceiver.Callback serviceReceiver = new TorrentTaskServiceReceiver.Callback() {
        @Subscribe(threadMode = ThreadMode.MAIN)
        public void onReceive(Bundle b)
        {
            if (b != null) {
                switch ((TorrentStateMsg.Type)b.getSerializable(TorrentStateMsg.TYPE)) {
                    case TORRENT_ADDED: {
                        Torrent torrent = b.getParcelable(TorrentStateMsg.TORRENT);
                        if (torrent != null) {
                            TorrentListItem item = new TorrentListItem();
                            item.torrentId = torrent.getId();
                            item.name = torrent.getName();
                            item.dateAdded = torrent.getDateAdded();
                            adapter.addItem(item);
                        }
                        break;
                    }
                    case UPDATE_TORRENT: {
                        BasicStateParcel state = b.getParcelable(TorrentStateMsg.STATE);
                        if (state != null)
                            adapter.updateItem(state);
                        break;
                    }
                    case UPDATE_TORRENTS: {
                        handleBasicStates(b.getBundle(TorrentStateMsg.STATES));
                        break;
                    }
                    case TORRENT_REMOVED: {
                        String id = b.getString(TorrentStateMsg.TORRENT_ID);
                        if (id != null)
                            adapter.deleteItem(id);
                        break;
                    }
                }
            }
        }
    };

    private void showDetailTorrent(String id) {
        Intent i = new Intent(activity, DetailTorrentActivity.class);
        i.putExtra(DetailTorrentActivity.TAG_TORRENT_ID, id);
        startActivity(i);
    }

    private void toggleSelection(int position) {
        adapter.toggleSelection(position);
        int count = adapter.getSelectedItemCount();

        if (actionMode != null) {
            if (count == 0) {
                actionMode.finish();
            } else {
                actionMode.setTitle(String.valueOf(count));
                actionMode.invalidate();
            }
        }
    }

    private TorrentListAdapter.ViewHolder.ClickListener torrentListListener = new TorrentListAdapter.ViewHolder.ClickListener() {
        @Override
        public void onItemClicked(int position, TorrentListItem item) {
            if (actionMode == null) {
                showDetailTorrent(item.torrentId);
            } else {
                onItemSelected(item.torrentId, position);
            }
        }

        @Override
        public boolean onItemLongClicked(int position, TorrentListItem item) {
            if (actionMode == null) {
                actionMode = activity.startActionMode(actionModeCallback);
            }
            onItemSelected(item.torrentId, position);
            return true;
        }

        @Override
        public void onPauseButtonClicked(int position, TorrentListItem item) {
            TorrentHelper.pauseResumeTorrent(item.torrentId);
        }
    };

}
