package com.example.mymovies.fragments;

public interface IDetailTorrentFragmentCallback {
    void onTorrentFilesChanged();
    void openFile(String relativePath);
}