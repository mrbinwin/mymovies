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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.mymovies.R;
import com.example.mymovies.dialogs.filemanager.FileManagerConfig;
import com.example.mymovies.dialogs.filemanager.FileManagerDialog;
import com.example.mymovies.libretorrent.core.TorrentMetaInfo;
import com.example.mymovies.libretorrent.core.utils.FileIOUtils;
import com.example.mymovies.libretorrent.core.utils.TorrentUtils;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/*
 * The fragment shows a some general info from parsed .torrent meta file
 *
 */
public class AddTorrentInfoFragment extends Fragment {

    private static final String TAG_TORRENT_META_INFO = "info";
    private static final String TAG_DOWNLOAD_DIR = "download_dir";
    private static final String TAG_CUSTOM_NAME = "custom_name";

    private static final int DIR_CHOOSER_REQUEST = 1;

    private CheckBox checkBoxSequentialDownload;
    private CheckBox checkBoxStartTorrent;

    private EditText editTextTorrentName;
    private ImageButton folderChooserButton;

    private LinearLayout layoutTorrentCreateDate;
    private LinearLayout layoutTorrentComment;
    private LinearLayout layoutTorrentCreatedInProgram;
    private LinearLayout layoutTorrentSizeAndCount;

    private TextInputLayout textInputLayoutTorrentName;

    private TextView textViewFreeSpace;
    private TextView textViewUploadTorrentInfo;
    private TextView textViewTorrentCreateDate;
    private TextView textViewTorrentComment;
    private TextView textViewTorrentCreatedInProgram;
    private TextView textViewTorrentHashSum;
    private TextView textViewTorrentSize;
    private TextView textViewTorrentFilesCount;

    private AppCompatActivity activity;
    private String customName;
    private String downloadDir = "";
    private TorrentMetaInfo torrentMetaInfo;

    public static AddTorrentInfoFragment newInstance() {
        AddTorrentInfoFragment fragment = new AddTorrentInfoFragment();
        Bundle b = new Bundle();
        fragment.setArguments(b);
        return fragment;
    }

    public String getDownloadDir() {
        return downloadDir;
    }

    public String getTorrentName() {
        return editTextTorrentName.getText().toString();
    }

    public boolean getIsSequentialDownload() {
        return checkBoxSequentialDownload.isChecked();
    }

    public boolean getIsStartTorrent() {
        return checkBoxStartTorrent.isChecked();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (activity == null) {
            activity = (AppCompatActivity)getActivity();
        }

        editTextTorrentName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                validateEditTextTorrentName(s);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                validateEditTextTorrentName(s);
                String name = s.toString();
                if (torrentMetaInfo != null && name.equals(torrentMetaInfo.torrentName)) {
                    return;
                }
                customName = name;
            }
        });

        folderChooserButton.setOnClickListener((View v) -> {
            Intent i = new Intent(activity, FileManagerDialog.class);
            FileManagerConfig config = new FileManagerConfig(
                    downloadDir,
                    null,
                    null,
                    FileManagerConfig.DIR_CHOOSER_MODE
            );
            i.putExtra(FileManagerDialog.TAG_CONFIG, config);
            startActivityForResult(i, DIR_CHOOSER_REQUEST);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == DIR_CHOOSER_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                if (data.hasExtra(FileManagerDialog.TAG_RETURNED_PATH)) {
                    downloadDir = data.getStringExtra(FileManagerDialog.TAG_RETURNED_PATH);
                    textViewUploadTorrentInfo.setText(downloadDir);
                    textViewFreeSpace.setText(
                            String.format(
                                    getString(R.string.free_space),
                                    Formatter.formatFileSize(activity.getApplicationContext(), FileIOUtils.getFreeSpace(downloadDir))
                            )
                    );
                }
            }
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof AppCompatActivity) {
            activity = (AppCompatActivity)context;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            torrentMetaInfo = savedInstanceState.getParcelable(TAG_TORRENT_META_INFO);
            downloadDir = savedInstanceState.getString(TAG_DOWNLOAD_DIR);
            customName = savedInstanceState.getString(TAG_CUSTOM_NAME);
        } else {
            downloadDir = TorrentUtils.getTorrentDownloadPath(activity.getApplicationContext());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_torrent_info, container, false);

        checkBoxSequentialDownload = view.findViewById(R.id.checkbox_sequential_download);
        checkBoxStartTorrent = view.findViewById(R.id.checkbox_start_torrent);
        checkBoxStartTorrent.setChecked(true);
        editTextTorrentName = view.findViewById(R.id.edit_text_torrent_name);
        folderChooserButton = view.findViewById(R.id.folder_chooser_button);
        layoutTorrentComment = view.findViewById(R.id.layout_torrent_comment);
        layoutTorrentCreateDate = view.findViewById(R.id.layout_torrent_create_date);
        layoutTorrentCreatedInProgram = view.findViewById(R.id.layout_torrent_created_in_program);
        layoutTorrentSizeAndCount = view.findViewById(R.id.layout_torrent_size_and_count);
        textInputLayoutTorrentName = view.findViewById(R.id.layout_torrent_name);

        textViewUploadTorrentInfo = view.findViewById(R.id.text_view_upload_torrent_into);
        textViewTorrentComment = view.findViewById(R.id.text_view_torrent_comment);
        textViewTorrentCreateDate = view.findViewById(R.id.text_view_torrent_create_date);
        textViewTorrentCreatedInProgram = view.findViewById(R.id.text_view_torrent_created_in_program);
        textViewTorrentHashSum = view.findViewById(R.id.text_view_torrent_hash_sum);
        textViewFreeSpace = view.findViewById(R.id.text_view_free_space);
        textViewTorrentFilesCount = view.findViewById(R.id.text_view_torrent_files_count);
        textViewTorrentSize = view.findViewById(R.id.text_view_torrent_size);

        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(TAG_DOWNLOAD_DIR, downloadDir);
        outState.putString(TAG_CUSTOM_NAME, customName);
        outState.putParcelable(TAG_TORRENT_META_INFO, torrentMetaInfo);
    }

    public void setInfo(TorrentMetaInfo torrentMetaInfo) {
        this.torrentMetaInfo = torrentMetaInfo;
        initViewFields();
    }

    private void initViewFields() {
        if (torrentMetaInfo == null) {
            return;
        }

        editTextTorrentName.setText(TextUtils.isEmpty(customName) ? torrentMetaInfo.torrentName : customName);
        textViewTorrentHashSum.setText(torrentMetaInfo.sha1Hash);
        textViewUploadTorrentInfo.setText(downloadDir);

        if (TextUtils.isEmpty(torrentMetaInfo.comment)) {
            layoutTorrentComment.setVisibility(View.GONE);
        } else {
            textViewTorrentComment.setText(torrentMetaInfo.comment);
            layoutTorrentComment.setVisibility(View.VISIBLE);
        }

        if (TextUtils.isEmpty(torrentMetaInfo.createdBy)) {
            layoutTorrentCreatedInProgram.setVisibility(View.GONE);
        } else {
            textViewTorrentCreatedInProgram.setText(torrentMetaInfo.createdBy);
            layoutTorrentCreatedInProgram.setVisibility(View.VISIBLE);
        }

        if (torrentMetaInfo.creationDate == 0) {
            layoutTorrentCreateDate.setVisibility(View.GONE);
        } else {
            textViewTorrentCreateDate.setText(
                    SimpleDateFormat.getDateTimeInstance().format(new Date(torrentMetaInfo.creationDate))
            );
            layoutTorrentCreateDate.setVisibility(View.VISIBLE);
        }

        if (torrentMetaInfo.torrentSize == 0 || torrentMetaInfo.fileCount == 0) {
            layoutTorrentSizeAndCount.setVisibility(View.GONE);
        } else {
            textViewTorrentSize.setText(Formatter.formatFileSize(activity, torrentMetaInfo.torrentSize));
            textViewTorrentFilesCount.setText(String.format(Locale.getDefault(), "%d", torrentMetaInfo.fileCount));
            textViewFreeSpace.setText(
                    String.format(getString(R.string.free_space),
                    Formatter.formatFileSize(activity.getApplicationContext(), FileIOUtils.getFreeSpace(downloadDir)))
            );
            layoutTorrentSizeAndCount.setVisibility(View.VISIBLE);
        }
    }

    private void validateEditTextTorrentName(CharSequence s) {
        if (TextUtils.isEmpty(s)) {
            textInputLayoutTorrentName.setErrorEnabled(true);
            textInputLayoutTorrentName.setError(getString(R.string.error_field_required));
            textInputLayoutTorrentName.requestFocus();
        } else {
            textInputLayoutTorrentName.setErrorEnabled(false);
            textInputLayoutTorrentName.setError(null);
        }
    }
}
