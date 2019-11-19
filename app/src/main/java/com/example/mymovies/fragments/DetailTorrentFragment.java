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
import android.os.Handler;
import android.os.Parcelable;
import android.text.format.Formatter;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymovies.R;
import com.example.mymovies.adapters.TorrentContentFilesAdapter;
import com.example.mymovies.dialogs.BaseAlertDialog;
import com.example.mymovies.libretorrent.core.BencodeFileItem;
import com.example.mymovies.libretorrent.core.Torrent;
import com.example.mymovies.libretorrent.core.TorrentHelper;
import com.example.mymovies.libretorrent.core.TorrentMetaInfo;
import com.example.mymovies.libretorrent.core.TorrentStateCode;
import com.example.mymovies.libretorrent.core.TorrentStateMsg;
import com.example.mymovies.libretorrent.core.exceptions.FreeSpaceException;
import com.example.mymovies.libretorrent.core.filetree.BencodeFileTree;
import com.example.mymovies.libretorrent.core.filetree.FileNode;
import com.example.mymovies.libretorrent.core.filetree.FilePriority;
import com.example.mymovies.libretorrent.core.filetree.FileTree;
import com.example.mymovies.libretorrent.core.filetree.TorrentContentFileTree;
import com.example.mymovies.libretorrent.core.server.TorrentStreamServer;
import com.example.mymovies.libretorrent.core.stateparcel.AdvanceStateParcel;
import com.example.mymovies.libretorrent.core.stateparcel.BasicStateParcel;
import com.example.mymovies.libretorrent.core.storage.TorrentStorage;
import com.example.mymovies.libretorrent.core.utils.FileTreeDepthFirstSearch;
import com.example.mymovies.libretorrent.core.utils.TorrentContentFileTreeUtils;
import com.example.mymovies.libretorrent.core.utils.Utils;
import com.example.mymovies.receivers.TorrentTaskServiceReceiver;
import com.example.mymovies.settings.SettingsManager;
import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.libtorrent4j.Priority;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/*
 * The fragment handles one selected item from a downloads list,
 * shows internal files, priorities, download progress
 *
 */
public class DetailTorrentFragment extends Fragment implements
        BaseAlertDialog.OnClickListener,
        BaseAlertDialog.OnDialogShowListener,
        TorrentContentFilesAdapter.ViewHolder.ClickListener,
        IDetailTorrentFragmentCallback {

    @SuppressWarnings("unused")
    private static final String TAG = DetailTorrentFragment.class.getSimpleName();
    private static final String TAG_DELETE_TORRENT_DIALOG = "delete_torrent_dialog";
    private static final String TAG_IN_ACTION_MODE = "in_action_mode";
    private static final String TAG_TORRENT_ID = "torrent_id";
    private static final String HEAVY_STATE_TAG = TAG + "_" + HeavyInstanceStorage.class.getSimpleName();
    private static final String TAG_FILES = "files";
    private static final String TAG_PRIORITIES = "priorities";
    private static final String TAG_LIST_FILE_STATE = "list_file_state";
    private static final String TAG_FILE_TREE = "file_tree";
    private static final String TAG_CUR_DIR = "cur_dir";
    private static final String TAG_SELECTABLE_ADAPTER = "selectable_adapter";
    private static final String TAG_SELECTED_FILES = "selected_files";
    private static final String TAG_PRIORITY_DIALOG = "priority_dialog";

    private static final int SYNC_TIME = 1000; /* ms */

    private AppCompatActivity activity;
    private Toolbar toolbar;
    private CoordinatorLayout coordinatorLayout;

    private AdvanceStateParcel advanceStateCache;
    private boolean downloadingMetadata = false;
    private boolean inActionMode = false;
    private TorrentMetaInfo infoCache;
    private TorrentStorage repo;
    private String torrentId;
    private Torrent torrent;

    private RecyclerView fileList;
    private LinearLayoutManager layoutManager;
    private TorrentContentFilesAdapter adapter;
    /* Save state scrolling */
    private Parcelable listFileState;
    private TextView filesSize;
    private ActionMode actionMode;
    private ActionModeCallback actionModeCallback = new ActionModeCallback();
    private ArrayList<String> selectedFiles = new ArrayList<>();
    private IDetailTorrentFragmentCallback callback;

    private ArrayList<BencodeFileItem> files;
    private ArrayList<FilePriority> priorities;
    private TorrentContentFileTree fileTree;
    private TorrentContentFileTree curDir;

    /* Update the torrent status params, which aren't included in BasicStateParcel */
    private Handler updateTorrentStateHandler = new Handler();
    private Runnable updateTorrentState = new Runnable() {
        @Override
        public void run() {
            getFilesStateRequest();
            updateTorrentStateHandler.postDelayed(this, SYNC_TIME);
        }
    };

    private class ActionModeCallback implements ActionMode.Callback
    {
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu)
        {
            MenuItem shareStreamUrl = menu.findItem(R.id.share_stream_url_menu);
            shareStreamUrl.setVisible(false);

            if (selectedFiles.size() != 1)
                return true;
            if (curDir == null)
                return true;
            TorrentContentFileTree file = curDir.getChild(selectedFiles.get(0));
            if (file == null || !file.isFile())
                return true;

            shareStreamUrl.setVisible(true);

            return true;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu)
        {
            inActionMode = true;
            mode.getMenuInflater().inflate(R.menu.detail_torrent_files_action_mode, menu);
            Utils.showActionModeStatusBar(activity, true);

            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item)
        {
            switch (item.getItemId()) {
                case R.id.change_priority_menu:
                    mode.finish();

                    showPriorityDialog();
                    break;
                case R.id.share_stream_url_menu:
                    mode.finish();

                    shareStreamUrl(selectedFiles.get(0));
                    selectedFiles.clear();
                    break;
            }

            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode)
        {
            adapter.clearSelection();
            actionMode = null;
            inActionMode = false;
            Utils.showActionModeStatusBar(activity, false);
        }
    }

    public static DetailTorrentFragment newInstance(String id) {
        DetailTorrentFragment fragment = new DetailTorrentFragment();
        fragment.torrentId = id;
        fragment.setArguments(new Bundle());
        return fragment;
    }

    public void disableSelectedFiles() {
        if (fileTree == null) {
            return;
        }

        List<TorrentContentFileTree> files = TorrentContentFileTreeUtils.getFiles(fileTree);
        if (files == null)
            return;

        for (TorrentContentFileTree file : files)
            if (file != null && file.getSelectState() == TorrentContentFileTree.SelectState.SELECTED)
                file.select(TorrentContentFileTree.SelectState.DISABLED);

        adapter.notifyDataSetChanged();
    }

    public Priority[] getPriorities() {
        if (fileTree == null) {
            return null;
        }

        List<TorrentContentFileTree> files = TorrentContentFileTreeUtils.getFiles(fileTree);
        if (files == null)
            return null;

        Priority[] priorities = new Priority[files.size()];
        for (TorrentContentFileTree file : files)
            if (file != null && (file.getIndex() >= 0 && file.getIndex() < files.size()))
                priorities[file.getIndex()] =  file.getFilePriority().getPriority();

        return priorities;
    }

    public long getSelectedFileSize() {
        return fileTree != null ? fileTree.selectedFileSize() : 0;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_detail_torrent, container, false);

        toolbar = v.findViewById(R.id.detail_toolbar);
        coordinatorLayout = v.findViewById(R.id.coordinator_layout);
        filesSize = v.findViewById(R.id.files_size);
        fileList = v.findViewById(R.id.file_list);

        return v;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof AppCompatActivity) {
            activity = (AppCompatActivity)context;
        }
        callback = this;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callback = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!TorrentTaskServiceReceiver.getInstance().isRegistered(serviceReceiver)) {
            TorrentTaskServiceReceiver.getInstance().register(serviceReceiver);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        TorrentTaskServiceReceiver.getInstance().unregister(serviceReceiver);
        stopUpdateTorrentState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (activity == null) {
            activity = (AppCompatActivity)getActivity();
        }

        if (savedInstanceState != null) {
            torrentId = savedInstanceState.getString(TAG_TORRENT_ID);
            inActionMode = savedInstanceState.getBoolean(TAG_IN_ACTION_MODE);
            selectedFiles = savedInstanceState.getStringArrayList(TAG_SELECTED_FILES);
            if (savedInstanceState.getBoolean(TAG_IN_ACTION_MODE, false)) {
                actionMode = activity.startActionMode(actionModeCallback);
                adapter.setSelectedItems(savedInstanceState.getIntegerArrayList(TAG_SELECTABLE_ADAPTER));
                actionMode.setTitle(String.valueOf(adapter.getSelectedItemCount()));
            }
        }

        HeavyInstanceStorage storage = HeavyInstanceStorage.getInstance(getFragmentManager());
        if (storage != null) {
            Bundle heavyInstance = storage.popData(HEAVY_STATE_TAG);
            if (heavyInstance != null) {
                files = (ArrayList<BencodeFileItem>)heavyInstance.getSerializable(TAG_FILES);
                priorities = (ArrayList<FilePriority>)heavyInstance.getSerializable(TAG_PRIORITIES);
                fileTree = (TorrentContentFileTree)heavyInstance.getSerializable(TAG_FILE_TREE);
                curDir = (TorrentContentFileTree)heavyInstance.getSerializable(TAG_CUR_DIR);
            }
        }

        updateFileSize();

        layoutManager = new LinearLayoutManager(activity);
        fileList.setLayoutManager(layoutManager);

        /*
         * A RecyclerView by default creates another copy of the ViewHolder in order to
         * fade the views into each other. This causes the problem because the old ViewHolder gets
         * the payload but then the new one doesn't. So needs to explicitly tell it to reuse the old one.
         */
        DefaultItemAnimator animator = new DefaultItemAnimator()
        {
            @Override
            public boolean canReuseUpdatedViewHolder(RecyclerView.ViewHolder viewHolder)
            {
                return true;
            }
        };
        fileList.setItemAnimator(animator);
        adapter = new TorrentContentFilesAdapter(getChildren(curDir), activity,
                R.layout.item_torrent_content_file, this);

        fileList.setAdapter(adapter);

        repo = new TorrentStorage(activity.getApplicationContext());
        if (torrentId != null) {
            torrent = repo.getTorrentByID(torrentId);
        }
        downloadingMetadata = torrent != null && torrent.isDownloadingMetadata();

        if (torrent != null) {
            toolbar.setTitle(torrent.getName());
        }
        activity.setSupportActionBar(toolbar);
        setHasOptionsMenu(true);

        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initRequest();
        startUpdateTorrentState();

        if (torrentId != null) {
            initFilesList(torrentId, getFileList(), getPrioritiesList());
            if (advanceStateCache != null) {
                updateFiles(
                        advanceStateCache.filesReceivedBytes,
                        advanceStateCache.filesAvailability
                );
            }

        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(TAG_TORRENT_ID, torrentId);
        outState.putBoolean(TAG_IN_ACTION_MODE, inActionMode);

        if (layoutManager != null) {
            listFileState = layoutManager.onSaveInstanceState();
        }
        outState.putParcelable(TAG_LIST_FILE_STATE, listFileState);
        if (adapter != null) {
            outState.putIntegerArrayList(TAG_SELECTABLE_ADAPTER, adapter.getSelectedItems());
        }
        outState.putBoolean(TAG_IN_ACTION_MODE, inActionMode);
        outState.putStringArrayList(TAG_SELECTED_FILES, selectedFiles);
        outState.putString(TAG_TORRENT_ID, torrentId);

        Bundle b = new Bundle();
        b.putSerializable(TAG_FILES, files);
        b.putSerializable(TAG_PRIORITIES, priorities);
        b.putSerializable(TAG_FILE_TREE, fileTree);
        b.putSerializable(TAG_CUR_DIR, curDir);
        HeavyInstanceStorage storage = HeavyInstanceStorage.getInstance(getFragmentManager());
        if (storage != null) {
            storage.pushData(HEAVY_STATE_TAG, b);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listFileState != null && layoutManager != null) {
            layoutManager.onRestoreInstanceState(listFileState);
        }
    }

    public void onTorrentFilesChanged() {

    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            listFileState = savedInstanceState.getParcelable(TAG_LIST_FILE_STATE);
        }
    }

    public void setTorrentId(String id) {
        torrentId = id;
    }

    public String getTorrentId() {
        return torrentId;
    }

    @Override
    public void onItemClicked(int position, TorrentContentFileTree node) {
        if (actionMode == null) {
            if (node.getName().equals(BencodeFileTree.PARENT_DIR)) {
                backToParent();
                return;
            }

            if (node.getType() == FileNode.Type.DIR) {
                chooseDirectory(node);
                reloadData();
            } else if (node.getSelectState().equals(TorrentContentFileTree.SelectState.DISABLED) &&
                    node.getReceivedBytes() == node.size()) {
                /* Completed file */
                if (callback != null)
                    callback.openFile(node.getPath());
            }

        } else if (!node.getName().equals(FileTree.PARENT_DIR)) {
            onItemSelected(node.getName(), position);
        }
    }

    @Override
    public boolean onItemLongClicked(int position, TorrentContentFileTree node) {
        if (node.getName().equals(FileTree.PARENT_DIR)) {
            return false;
        }

        if (actionMode == null) {
            actionMode = activity.startActionMode(actionModeCallback);
        }
        onItemSelected(node.getName(), position);
        return true;
    }

    @Override
    public void onItemCheckedChanged(TorrentContentFileTree node, boolean selected) {
        if (node.getSelectState() == TorrentContentFileTree.SelectState.DISABLED) {
            return;
        }

        node.select((
                selected ?
                        TorrentContentFileTree.SelectState.SELECTED :
                        TorrentContentFileTree.SelectState.UNSELECTED
        ));
        adapter.updateItem(node);

        if (callback != null) {
            callback.onTorrentFilesChanged();
        }
        updateFileSize();
    }

    @Override
    public void onPositiveClicked(@Nullable View v) {
        FragmentManager fm = getFragmentManager();
        if (v != null && fm != null) {
            if (fm.findFragmentByTag(TAG_DELETE_TORRENT_DIALOG) != null) {
                CheckBox withFiles = v.findViewById(R.id.dialog_delete_torrent_with_downloaded_files);
                deleteTorrentRequest(withFiles.isChecked());
                finish(new Intent(), FragmentCallback.ResultCode.CANCEL);

            } else if (curDir != null && fm.findFragmentByTag(TAG_PRIORITY_DIALOG) != null) {
                RadioGroup group = v.findViewById(R.id.dialog_priorities_group);
                int radioButtonId = group.getCheckedRadioButtonId();
                List<TorrentContentFileTree> files = new ArrayList<>();

                for (String name : selectedFiles) {
                    files.add(curDir.getChild(name));
                }

                switch (radioButtonId) {
                    case R.id.dialog_priority_low:
                        applyPriority(files, new FilePriority(FilePriority.Type.IGNORE));
                        break;
                    case R.id.dialog_priority_normal:
                        applyPriority(files, new FilePriority(FilePriority.Type.NORMAL));
                        break;
                    case R.id.dialog_priority_high:
                        applyPriority(files, new FilePriority(FilePriority.Type.HIGH));
                        break;
                    default:
                        /* No selected */
                        selectedFiles.clear();
                        return;
                }
                selectedFiles.clear();
            }
        }
    }

    @Override
    public void onNegativeClicked(@Nullable View v) {
        selectedFiles.clear();
    }

    @Override
    public void onNeutralClicked(@Nullable View v) {
        /* Nothing */
    }

    final synchronized void reloadData() {
        adapter.clearFiles();

        List<TorrentContentFileTree> children = getChildren(curDir);
        if (children.size() == 0)
            adapter.notifyDataSetChanged();
        else
            adapter.addFiles(children);
    }

    private void applyPriority(List<TorrentContentFileTree> files, FilePriority priority) {
        if (files == null || priority == null) {
            return;
        }

        for (TorrentContentFileTree file : files)
            if (file != null)
                file.setPriority(priority);

        if (callback != null)
            callback.onTorrentFilesChanged();

        adapter.notifyDataSetChanged();
    }

    /*
     * Navigate back to an upper directory.
     */
    private void backToParent() {
        curDir = curDir.getParent();
        reloadData();
    }

    private void chooseDirectory(TorrentContentFileTree node) {
        if (node.isFile()) {
            node = fileTree;
        }
        curDir = node;
    }

    private void initFilesList(String torrentId, ArrayList<BencodeFileItem> files,
                     List<Priority> priorities) {
        if (fileTree != null)
            return;
        if (torrentId == null || files == null || priorities == null)
            return;

        this.torrentId = torrentId;
        this.files = files;
        this.priorities = new ArrayList<>();
        for (Priority priority : priorities)
            this.priorities.add(new FilePriority(priority));

        makeFileTree();
        updateFileSize();
        reloadData();
    }

    private List<TorrentContentFileTree> getChildren(TorrentContentFileTree node) {
        List<TorrentContentFileTree> children = new ArrayList<>();
        if (node == null || node.isFile())
            return children;

        /* Adding parent dir for navigation */
        if (curDir != fileTree && curDir.getParent() != null)
            children.add(0, new TorrentContentFileTree(FileTree.PARENT_DIR, 0L,
                    FileNode.Type.DIR, curDir.getParent()));
        children.addAll(curDir.getChildren());

        return children;
    }

    private ArrayList<BencodeFileItem> getFileList() {
        return (infoCache != null ? infoCache.fileList : new ArrayList<>());
    }

    private List<Priority> getPrioritiesList() {
        if (torrent == null) {
            return null;
        }
        return torrent.getFilePriorities();
    }

    private void makeFileTree() {
        if ((files == null || priorities == null) || files.size() != priorities.size()) {
            return;
        }

        fileTree = TorrentContentFileTreeUtils.buildFileTree(files);
        FileTreeDepthFirstSearch<TorrentContentFileTree> search = new FileTreeDepthFirstSearch<>();
        /* Set priority for selected files */
        for (int i = 0; i < files.size(); i++) {
            if (priorities.get(i).getType() != FilePriority.Type.IGNORE) {
                BencodeFileItem f = files.get(i);

                TorrentContentFileTree file = search.find(fileTree, f.getIndex());
                if (file != null) {
                    file.setPriority(priorities.get(i));
                    /*
                     * Disable the ability to select the file
                     * because it's being downloaded/download
                     */
                    file.select(TorrentContentFileTree.SelectState.DISABLED);
                }
            }
        }

        /* Is assigned the root dir of the file tree */
        curDir = fileTree;
    }

    private void onItemSelected(String fileName, int position) {
        toggleSelection(position);

        if (selectedFiles.contains(fileName))
            selectedFiles.remove(fileName);
        else
            selectedFiles.add(fileName);

        int size = selectedFiles.size();
        /* Show/hide menu items after change selected channels */
        if (actionMode != null && (size == 1 || size == 2))
            actionMode.invalidate();
    }

    private void shareStreamUrl(String fileName) {
        if (curDir == null) {
            return;
        }

        TorrentContentFileTree file = curDir.getChild(fileName);
        if (file == null || !file.isFile()) {
            return;
        }

        int fileIndex = file.getIndex();

        SharedPreferences pref = SettingsManager.getPreferences(activity.getApplicationContext());
        String hostname = pref.getString(
                getString(R.string.pref_key_streaming_hostname),
                SettingsManager.Default.streamingHostname
        );
        int port = pref.getInt(
                getString(R.string.pref_key_streaming_port),
                SettingsManager.Default.streamingPort
        );

        String url = TorrentStreamServer.makeStreamUrl(hostname, port, torrentId, fileIndex);

        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "url");
        sharingIntent.putExtra(Intent.EXTRA_TEXT, url);

        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)));
    }

    private void showPriorityDialog() {
        FragmentManager fm  = getFragmentManager();
        if (fm != null && fm.findFragmentByTag(TAG_PRIORITY_DIALOG) == null) {
            BaseAlertDialog priorityDialog = BaseAlertDialog.newInstance(
                    getString(R.string.dialog_change_priority_title),
                    null,
                    R.layout.dialog_change_priority,
                    getString(R.string.ok),
                    getString(R.string.cancel),
                    null,
                    this);

            priorityDialog.show(fm, TAG_PRIORITY_DIALOG);
        }
    }

    private void toggleSelection(int position) {
        adapter.toggleSelection(position);
        int count = adapter.getSelectedItemCount();

        if (count == 0) {
            actionMode.finish();
        } else {
            actionMode.setTitle(String.valueOf(count));
            actionMode.invalidate();
        }
    }

    private void updateFileSize() {
        if (fileTree == null) {
            return;
        }

        filesSize.setText(
                String.format(
                        getString(R.string.files_size),
                        Formatter.formatFileSize(activity.getApplicationContext(), fileTree.selectedFileSize()),
                        Formatter.formatFileSize(activity.getApplicationContext(), fileTree.size())
                )
        );
    }

    public void updateFiles(long[] receivedBytes, double[] availability) {
        if (fileTree == null) {
            return;
        }

        Map<Integer, TorrentContentFileTree> files = TorrentContentFileTreeUtils.getFilesAsMap(fileTree);
        if (receivedBytes != null) {
            for (int i = 0; i < receivedBytes.length; i++) {
                TorrentContentFileTree file = files.get(i);
                if (file != null)
                    file.setReceivedBytes(receivedBytes[i]);
            }
        }
        if (availability != null) {
            for (int i = 0; i < availability.length; i++) {
                TorrentContentFileTree file = files.get(i);
                if (file != null)
                    file.setAvailability(availability[i]);
            }
        }

        adapter.notifyItemRangeChanged(0, curDir.getChildrenCount());
    }



    private void applyFilesChanges() {
        Priority[] priorities = getPriorities();
        if (priorities != null) {
            disableSelectedFiles();
            changeFilesPriorityRequest(priorities);
        }
    }

    public void openFile(String relativePath) {
        if (relativePath == null) {
            return;
        }

        String path = torrent.getDownloadPath() + File.separator + relativePath;
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        /* Give user a choice than to open file (without determining MIME type) */
        intent.setDataAndType(FileProvider.getUriForFile(activity,
                activity.getApplicationContext().getPackageName() + ".provider", new File(path)), "*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, getString(R.string.open_using)));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.detail_torrent, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        prepareOptionsMenu(menu);
    }

    private void prepareOptionsMenu(Menu menu) {
        MenuItem pauseResume = menu.findItem(R.id.pause_resume_torrent_menu);
        if (torrent == null || !torrent.isPaused()) {
            pauseResume.setTitle(R.string.pause_torrent);
            pauseResume.setIcon(R.drawable.ic_pause_white_24dp);
        } else {
            pauseResume.setTitle(R.string.resume_torrent);
            pauseResume.setIcon(R.drawable.ic_play_arrow_white_24dp);

        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        FragmentManager fm = getFragmentManager();
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.pause_resume_torrent_menu:
                TorrentHelper.pauseResumeTorrent(torrentId);
                break;
            case R.id.delete_torrent_menu:
                if (fm != null && fm.findFragmentByTag(TAG_DELETE_TORRENT_DIALOG) == null) {
                    BaseAlertDialog deleteTorrentDialog = BaseAlertDialog.newInstance(
                            getString(R.string.deleting),
                            getString(R.string.delete_selected_torrent),
                            R.layout.dialog_delete_torrent,
                            getString(R.string.ok),
                            getString(R.string.cancel),
                            null,
                            DetailTorrentFragment.this);

                    deleteTorrentDialog.show(fm, TAG_DELETE_TORRENT_DIALOG);
                }
                break;
        }

        return true;
    }

    @Override
    public void onShow(final AlertDialog dialog) {
        FragmentManager fm = getFragmentManager();
        if (dialog != null && fm != null && fm.findFragmentByTag(TAG_PRIORITY_DIALOG) != null) {
            if (curDir == null) {
                return;
            }

            List<FilePriority> priorities = new ArrayList<>();
            for (String name : selectedFiles) {
                TorrentContentFileTree file = curDir.getChild(name);
                if (file != null)
                    priorities.add(file.getFilePriority());
            }
            if (priorities.size() == 0)
                return;

            /*
             * We compare the array with randomly selected priority.
             * If all elements equals with this priority, set isMixedPriority as true and based on
             * the random priority choosing radio button, which will be checked by default.
             * Else, set isMixedPriority as false and clear check in RadioGroup
             */
            FilePriority randomPriority = priorities.get(new Random().nextInt(priorities.size()));
            boolean isMixedPriority = false;

            if (randomPriority != null && randomPriority.getType() == FilePriority.Type.MIXED) {
                isMixedPriority = true;
            } else {
                for (FilePriority priority : priorities) {
                    if (randomPriority != null && randomPriority != priority) {
                        isMixedPriority = true;
                        break;
                    }
                }
            }

            if (randomPriority != null && !isMixedPriority) {
                int resId;
                switch (randomPriority.getType()) {
                    case IGNORE:
                        resId = R.id.dialog_priority_low;
                        break;
                    case HIGH:
                        resId = R.id.dialog_priority_high;
                        break;
                    default:
                        resId = R.id.dialog_priority_normal;
                }

                RadioButton button = dialog.findViewById(resId);
                if (button != null) {
                    button.setChecked(true);
                }
            } else {
                RadioGroup group = dialog.findViewById(R.id.dialog_priorities_group);
                if (group != null) {
                    group.clearCheck();
                }
            }
        }
    }

    private void initRequest() {
        infoCache = TorrentHelper.getTorrentMetaInfo(torrentId);
    }

    private void changeFilesPriorityRequest(Priority[] priorities) {
        if (priorities == null || torrentId == null)
            return;

        try {
            TorrentHelper.changeFilesPriority(activity.getApplicationContext(), torrentId, priorities);

        } catch (FreeSpaceException e) {
            Snackbar.make(coordinatorLayout,
                    R.string.error_free_space,
                    Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    private void deleteTorrentRequest(boolean withFiles) {
        ArrayList<String> list = new ArrayList<>();
        list.add(torrentId);
        TorrentHelper.deleteTorrents(activity.getApplicationContext(), list, withFiles);
    }

    private void startUpdateTorrentState() {
        updateTorrentStateHandler.post(updateTorrentState);
    }

    private void stopUpdateTorrentState() {
        updateTorrentStateHandler.removeCallbacks(updateTorrentState);
    }

    private void getFilesStateRequest() {
        AdvanceStateParcel advanceStateParcel = TorrentHelper.makeAdvancedState(torrentId);
        if (advanceStateParcel == null) {
            return;
        }

        advanceStateCache = advanceStateParcel;
        updateFiles(advanceStateCache.filesReceivedBytes, advanceStateCache.filesAvailability);
    }

    TorrentTaskServiceReceiver.Callback serviceReceiver = new TorrentTaskServiceReceiver.Callback() {
        @Subscribe(threadMode = ThreadMode.MAIN)
        public void onReceive(Bundle b) {
            if (b != null) {
                switch ((TorrentStateMsg.Type)b.getSerializable(TorrentStateMsg.TYPE)) {
                    case UPDATE_TORRENT: {
                        BasicStateParcel state = b.getParcelable(TorrentStateMsg.STATE);
                        if (state != null && state.torrentId.equals(torrentId))
                            handleTorrentState(state);
                        break;
                    }
                    case UPDATE_TORRENTS: {
                        Bundle states = b.getParcelable(TorrentStateMsg.STATES);
                        if (states != null && states.containsKey(torrentId))
                            handleTorrentState(states.getParcelable(torrentId));
                        break;
                    }
                }
            }
        }
    };

    private void handleTorrentState(final BasicStateParcel state) {
        if (state == null) {
            return;
        }

        if (downloadingMetadata && state.stateCode != TorrentStateCode.DOWNLOADING_METADATA) {
            downloadingMetadata = false;

            if (torrentId != null) {
                torrent = repo.getTorrentByID(torrentId);
            }

            TorrentMetaInfo info = TorrentHelper.getTorrentMetaInfo(torrentId);
            if (info != null) {
                infoCache = info;
            }
            initFilesList(torrentId, getFileList(), getPrioritiesList());
            if (advanceStateCache != null) {
                updateFiles(advanceStateCache.filesReceivedBytes, advanceStateCache.filesAvailability);
            }
            activity.invalidateOptionsMenu();
        }

        if (torrent != null) {
            if (state.stateCode == TorrentStateCode.PAUSED || torrent.isPaused()) {
                torrent.setPaused(state.stateCode == TorrentStateCode.PAUSED);
                /* Redraw pause/resume menu */
                activity.invalidateOptionsMenu();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        /* Nothing */
    }

    public void onBackPressed() {
        finish(new Intent(), FragmentCallback.ResultCode.BACK);
    }

    private void finish(Intent intent, FragmentCallback.ResultCode code) {
        ((FragmentCallback)activity).fragmentFinished(intent, code);
    }
}
