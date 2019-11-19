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

package com.example.mymovies.fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.collection.ArraySet;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Parcelable;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.mymovies.R;
import com.example.mymovies.adapters.DownloadableFilesAdapter;
import com.example.mymovies.libretorrent.core.BencodeFileItem;
import com.example.mymovies.libretorrent.core.TorrentMetaInfo;
import com.example.mymovies.libretorrent.core.filetree.BencodeFileTree;
import com.example.mymovies.libretorrent.core.filetree.FileNode;
import com.example.mymovies.libretorrent.core.utils.BencodeFileTreeUtils;
import com.example.mymovies.libretorrent.core.utils.FileTreeDepthFirstSearch;

import org.libtorrent4j.Priority;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * The fragment shows a list of files from parsed .torrent meta file
 *
 */
public class AddTorrentFilesFragment extends Fragment
        implements DownloadableFilesAdapter.ViewHolder.ClickListener {

    private static final String TAG_LIST_FILES_STATE = "list_files_state";
    private static final String TAG_FILE_TREE = "file_tree";
    private static final String TAG_CUR_DIR = "cur_dir";

    private RecyclerView recyclerViewFilesList;
    private TextView textViewFilesSize;

    private DownloadableFilesAdapter downloadableFilesAdapter;
    private AppCompatActivity activity;
    /* Current directory */
    private BencodeFileTree curDir;
    private ArrayList<BencodeFileItem> files;
    private BencodeFileTree fileTree;
    private LinearLayoutManager layoutManager;
    /* Save state scrolling */
    private Parcelable listFilesState;
    private List<Priority> priorities;
    private TorrentMetaInfo torrentMetaInfo;

    public static AddTorrentFilesFragment newInstance() {
        AddTorrentFilesFragment fragment = new AddTorrentFilesFragment();
        Bundle b = new Bundle();
        fragment.setArguments(b);
        return fragment;
    }

    public Set<Integer> getSelectedFileIndexes() {
        if (fileTree == null) {
            return new HashSet<>();
        }
        List<BencodeFileTree> files = BencodeFileTreeUtils.getFiles(fileTree);
        Set<Integer> indexes = new ArraySet<>();
        for (BencodeFileTree file : files) {
            if (file.isSelected()) {
                indexes.add(file.getIndex());
            }
        }
        return indexes;
    }

    public long getSelectedFileSize() {
        return (fileTree != null ? fileTree.selectedFileSize() : 0);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof AppCompatActivity) {
            activity = (AppCompatActivity)context;
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (activity == null) {
            activity = (AppCompatActivity) getActivity();
        }
        if (savedInstanceState != null) {
            fileTree = (BencodeFileTree)savedInstanceState.getSerializable(TAG_FILE_TREE);
            curDir = (BencodeFileTree)savedInstanceState.getSerializable(TAG_CUR_DIR);
        }
        updateFileSize();

        layoutManager = new LinearLayoutManager(activity);
        recyclerViewFilesList.setLayoutManager(layoutManager);
        DefaultItemAnimator animator = new DefaultItemAnimator() {
            @Override
            public boolean canReuseUpdatedViewHolder(RecyclerView.ViewHolder viewHolder) {
                return true;
            }
        };
        recyclerViewFilesList.setItemAnimator(animator);
        downloadableFilesAdapter = new DownloadableFilesAdapter(
                getChildren(curDir), activity, R.layout.item_torrent_downloadable_file, this
        );
        recyclerViewFilesList.setAdapter(downloadableFilesAdapter);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_torrent_files, container, false);
        recyclerViewFilesList = view.findViewById(R.id.recycler_view_file_list);
        textViewFilesSize = view.findViewById(R.id.text_view_files_size);

        return view;
    }

    @Override
    public void onItemClicked(BencodeFileTree node) {
        if (node.getName().equals(BencodeFileTree.PARENT_DIR)) {
            backToParent();
            return;
        }

        if (node.getType() == FileNode.Type.DIR) {
            chooseDirectory(node);
            reloadData();
        }
    }

    @Override
    public void onItemCheckedChanged(BencodeFileTree node, boolean selected) {
        node.select(selected);
        updateFileSize();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        listFilesState = layoutManager.onSaveInstanceState();
        outState.putParcelable(TAG_LIST_FILES_STATE, listFilesState);
        outState.putSerializable(TAG_FILE_TREE, fileTree);
        outState.putSerializable(TAG_CUR_DIR, curDir);
    }

    public void setFiles(TorrentMetaInfo torrentMetaInfo) {
        files = torrentMetaInfo.fileList;
        makeFileTree();
        updateFileSize();
        reloadData();
    }

    private void backToParent() {
        curDir = curDir.getParent();
        reloadData();
    }

    private void chooseDirectory(BencodeFileTree node) {
        if (node.isFile()) {
            node = fileTree;
        }
        curDir = node;
    }

    private List<BencodeFileTree> getChildren(BencodeFileTree node) {
        List<BencodeFileTree> children = new ArrayList<>();
        if (node == null || node.isFile())
            return children;

        /* Adding parent dir for navigation */
        if (curDir != fileTree && curDir.getParent() != null)
            children.add(0, new BencodeFileTree(BencodeFileTree.PARENT_DIR, 0L, FileNode.Type.DIR, curDir.getParent()));

        children.addAll(curDir.getChildren());

        return children;
    }

    private void makeFileTree() {
        if (files == null) {
            return;
        }

        fileTree = BencodeFileTreeUtils.buildFileTree(files);

        if (priorities == null || priorities.size() == 0) {
            fileTree.select(true);
        } else {
            FileTreeDepthFirstSearch<BencodeFileTree> search = new FileTreeDepthFirstSearch<>();
            /* Select files that have non-IGNORE priority (see BEP35 standard) */
            long n = (priorities.size() > files.size() ? files.size() : priorities.size());
            for (int i = 0; i < n; i++) {
                if (priorities.get(i) == Priority.IGNORE)
                    continue;
                BencodeFileTree file = search.find(fileTree, i);
                if (file != null)
                    file.select(true);
            }
        }

        /* Is assigned the root dir of the file tree */
        curDir = fileTree;
    }

    private final synchronized void reloadData() {
        downloadableFilesAdapter.clearFiles();
        List<BencodeFileTree> children = getChildren(curDir);
        if (children.size() == 0) {
            downloadableFilesAdapter.notifyDataSetChanged();
        }
        else {
            downloadableFilesAdapter.addFiles(children);
        }
    }

    private void updateFileSize()
    {
        if (fileTree == null) {
            return;
        }

        textViewFilesSize.setText(
                String.format(
                        getString(R.string.files_size),
                        Formatter.formatFileSize(activity.getApplicationContext(), fileTree.selectedFileSize()),
                        Formatter.formatFileSize(activity.getApplicationContext(), fileTree.size())
                )
        );
    }
}
