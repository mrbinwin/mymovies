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

package com.example.mymovies.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.mymovies.activities.MainActivity;
import com.example.mymovies.R;
import com.example.mymovies.libretorrent.core.AddTorrentParams;
import com.example.mymovies.libretorrent.core.ProxySettingsPack;
import com.example.mymovies.libretorrent.core.Torrent;
import com.example.mymovies.libretorrent.core.TorrentDownload;
import com.example.mymovies.libretorrent.core.TorrentEngine;
import com.example.mymovies.libretorrent.core.TorrentEngineCallback;
import com.example.mymovies.libretorrent.core.TorrentHelper;
import com.example.mymovies.libretorrent.core.TorrentMetaInfo;
import com.example.mymovies.libretorrent.core.TorrentStateCode;
import com.example.mymovies.libretorrent.core.TorrentStateMsg;
import com.example.mymovies.libretorrent.core.exceptions.DecodeException;
import com.example.mymovies.libretorrent.core.exceptions.FileAlreadyExistsException;
import com.example.mymovies.libretorrent.core.exceptions.FreeSpaceException;
import com.example.mymovies.libretorrent.core.server.TorrentStreamServer;
import com.example.mymovies.libretorrent.core.stateparcel.BasicStateParcel;
import com.example.mymovies.libretorrent.core.stateparcel.StateParcelCache;
import com.example.mymovies.libretorrent.core.storage.TorrentStorage;
import com.example.mymovies.libretorrent.core.utils.TorrentUtils;
import com.example.mymovies.libretorrent.core.utils.Utils;
import com.example.mymovies.receivers.NotificationReceiver;
import com.example.mymovies.receivers.TorrentTaskServiceReceiver;
import com.example.mymovies.receivers.WifiReceiver;
import com.example.mymovies.settings.SettingsManager;

import org.libtorrent4j.swig.settings_pack;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/*
 * The main service that interacts with libretorrent core TorrentEngine and streamService
 *
 */

public class TorrentTaskService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener, TorrentEngineCallback {

    public static final String ACTION_SHUTDOWN = "com.example.mymovies.services.TorrentTaskService.ACTION_SHUTDOWN";
    public static final String ACTION_ADD_TORRENT = "com.example.mymovies.services.TorrentTaskService.ACTION_ADD_TORRENT";
    public static final String ACTION_ADD_TORRENT_LIST = "com.example.mymovies.services.TorrentTaskService.ACTION_ADD_TORRENT_LIST";
    public static final String DEFAULT_CHAN_ID = "com.example.mymovies.DEFAULT_CHAN";
    public static final String FOREGROUND_NOTIFY_CHAN_ID = "com.example.mymovies.FOREGROUND_NOTIFY_CHAN";
    private static final int SERVICE_STARTED_NOTIFICATION_ID = 1;
    private static final int SESSION_ERROR_NOTIFICATION_ID = 3;
    private static final int NAT_ERROR_NOTIFICATION_ID = 3;
    private static final int SYNC_TIME = 1000; /* ms */
    private static final String TAG = TorrentTaskService.class.getSimpleName();
    public static final String TAG_ADD_TORRENT_PARAMS = "add_torrent_params";
    public static final String TAG_ADD_TORRENT_PARAMS_LIST = "add_torrent_params_list";
    public static final String TAG_SAVE_TORRENT_FILE = "save_torrnet_file";

    /* Reduces sending packets due skip cache duplicates */
    private StateParcelCache<BasicStateParcel> basicStateCache = new StateParcelCache<>();
    private final IBinder binder = new LocalBinder();
    private NotificationCompat.Builder foregroundNotification;
    private boolean isAlreadyRunning = false;
    private AtomicBoolean isNeededToUpdateNotification = new AtomicBoolean(false);
    private boolean isNetworkOnline = false;
    private NotificationManager notificationManager;
    private AtomicBoolean pauseTorrents = new AtomicBoolean(false);
    private TorrentStorage repo;
    private SharedPreferences sharedPreferences;
    private Thread shutdownThread;
    private TorrentStreamServer torrentStreamServer;
    private Handler updateForegroundNotificationHandler;
    private WifiReceiver wifiReceiver = new WifiReceiver();

    public class LocalBinder extends Binder {
        public TorrentTaskService getService() {
            return TorrentTaskService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onEngineStarted() {
        loadTorrents(repo.getAll());

        if (sharedPreferences.getBoolean(getString(R.string.pref_key_use_random_port), SettingsManager.Default.useRandomPort)) {
            TorrentEngine.getInstance().setRandomPort();
            sharedPreferences.edit().putInt(getString(R.string.pref_key_port), TorrentEngine.getInstance().getPort()).apply();
        }

        if (sharedPreferences.getBoolean(getString(R.string.pref_key_proxy_changed), SettingsManager.Default.proxyChanged)) {
            sharedPreferences.edit().putBoolean(getString(R.string.pref_key_proxy_changed), false).apply();
            setProxy();
        }

        if (sharedPreferences.getBoolean(getString(R.string.pref_key_enable_ip_filtering), SettingsManager.Default.enableIpFiltering)) {
            TorrentEngine.getInstance().enableIpFilter(
                    sharedPreferences.getString(getString(R.string.pref_key_ip_filtering_file), SettingsManager.Default.ipFilteringFile)
            );
        }

        boolean enableStreaming = sharedPreferences.getBoolean(getString(R.string.pref_key_streaming_enable), SettingsManager.Default.enableStreaming);
        if (enableStreaming) {
            startStreamingServer();
        }
    }

    @Override
    public void onNatError(String errorMsg) {
        Log.e(TAG, "NAT error: " + errorMsg);
        if (sharedPreferences.getBoolean(getString(R.string.pref_key_show_nat_errors), SettingsManager.Default.showNatErrors)) {
            makeNatErrorNotify(errorMsg);
        }
    }

    @Override
    public void onRestoreSessionError(String id) {
        if (id == null) {
            return;
        }

        try {
            Torrent torrent = repo.getTorrentByID(id);
            if (torrent != null) {
                makeTorrentErrorNotify(torrent.getName(), getString(R.string.restore_torrent_error));
                repo.delete(torrent);
            }
        } catch (Exception e) {
            /* Ignore */
        }
    }

    @Override
    public void onTorrentAdded(String id) {
        TorrentDownload task = TorrentEngine.getInstance().getTask(id);
        if (task != null && pauseTorrents.get()) {
            task.pause();
        }
    }

    @Override
    public void onTorrentFinished(String id) {
        Torrent torrent = repo.getTorrentByID(id);
        if (torrent == null) {
            return;
        }

        if (!torrent.isFinished()) {
            torrent.setFinished(true);
            makeFinishNotify(torrent);

            repo.update(torrent);

            TorrentDownload task = TorrentEngine.getInstance().getTask(id);
            if (task != null) {
                task.setTorrent(torrent);
            }
        }
    }

    @Override
    public void onTorrentPaused(String id) {
        TorrentDownload task = TorrentEngine.getInstance().getTask(id);
        sendBasicState(task);

        Torrent torrent = repo.getTorrentByID(id);
        if (torrent == null) {
            return;
        }

        if (!torrent.isPaused()) {
            torrent.setPaused(true);
            repo.update(torrent);

            if (task != null) {
                task.setTorrent(torrent);
            }
        }
    }

    @Override
    public void onTorrentRemoved(String id) {
        if (basicStateCache.contains(id)) {
            basicStateCache.remove(id);
        }
        TorrentTaskServiceReceiver.getInstance().post(TorrentStateMsg.makeTorrentRemovedBundle(id));
    }

    @Override
    public void onTorrentResumed(String id) {
        TorrentDownload task = TorrentEngine.getInstance().getTask(id);
        if (task != null && !task.getTorrent().isPaused()) {
            return;
        }

        Torrent torrent = repo.getTorrentByID(id);
        if (torrent == null) {
            return;
        }

        torrent.setPaused(false);
        torrent.setError(null);
        repo.update(torrent);

        if (task != null) {
            task.setTorrent(torrent);
        }
    }

    @Override
    public void onTorrentStateChanged(String id) {
        sendBasicState(TorrentEngine.getInstance().getTask(id));
    }

    @Override
    public void onTorrentMoved(String id, boolean success) {

    }

    @Override
    public void onIpFilterParsed(boolean success) {
        Toast.makeText(
                getApplicationContext(),
                (success ? getString(R.string.ip_filter_add_success) : getString(R.string.ip_filter_add_error)),
                Toast.LENGTH_LONG
        ).show();
    }

    @Override
    public void onMagnetLoaded(String hash, byte[] bencode) {
        TorrentMetaInfo info = null;
        try {
            info = new TorrentMetaInfo(bencode);

        } catch (DecodeException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        TorrentTaskServiceReceiver.getInstance().post(TorrentStateMsg.makeMagnetFetchedBundle(info));
    }

    @Override
    public void onSessionError(String errorMsg) {
        Log.e(TAG, errorMsg);
        makeSessionErrorNotify(errorMsg);
    }

    @Override
    public void onTorrentMetadataLoaded(String id, Exception err) {
        if (err != null) {
            Log.e(TAG, "Load metadata error: ");
            Log.e(TAG, Log.getStackTraceString(err));
            if (err instanceof FreeSpaceException) {
                makeTorrentErrorNotify(repo.getTorrentByID(id).getName(), getString(R.string.error_free_space));
                TorrentTaskServiceReceiver.getInstance().post(TorrentStateMsg.makeTorrentRemovedBundle(id));
            }
            repo.delete(id);
        } else {
            TorrentDownload task = TorrentEngine.getInstance().getTask(id);
            if (task != null) {
                Torrent torrent = task.getTorrent();
                repo.update(torrent);
                if (sharedPreferences.getBoolean(getString(R.string.pref_key_save_torrent_files), SettingsManager.Default.saveTorrentFiles)) {
                    TorrentHelper.saveTorrentFileIn(
                            getApplicationContext(),
                            torrent,
                            sharedPreferences.getString(
                                    getString(R.string.pref_key_save_torrent_files_in),
                                    torrent.getDownloadPath()
                            )
                    );
                }
            }
        }
    }

    @Override
    public void onTorrentError(String id, String errorMsg) {
        if (errorMsg != null) {
            Log.e(TAG, "Torrent " + id + ": " + errorMsg);
        }

        TorrentDownload task = TorrentEngine.getInstance().getTask(id);
        Torrent torrent = repo.getTorrentByID(id);
        if (torrent == null) {
            return;
        }

        torrent.setError(errorMsg);
        repo.update(torrent);

        if (task != null) {
            task.setTorrent(torrent);
            task.pause();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if (sharedPreferences == null)
            sharedPreferences = sp;

        if (key.equals(getString(R.string.pref_key_battery_control)) ||
                key.equals(getString(R.string.pref_key_custom_battery_control)) ||
                key.equals(getString(R.string.pref_key_custom_battery_control_value)) ||
                key.equals(getString(R.string.pref_key_download_and_upload_only_when_charging)) ||
                key.equals(getString(R.string.pref_key_wifi_only))) {
            if (checkPauseControl())
                TorrentEngine.getInstance().pauseAll();
            else
                TorrentEngine.getInstance().resumeAll();
        } else if (key.equals(getString(R.string.pref_key_max_download_speed))) {
            TorrentEngine.Settings s = TorrentEngine.getInstance().getSettings();
            s.downloadRateLimit = sharedPreferences.getInt(key, SettingsManager.Default.maxDownloadSpeedLimit);
            TorrentEngine.getInstance().setSettings(s);
        } else if (key.equals(getString(R.string.pref_key_max_upload_speed))) {
            TorrentEngine.Settings s = TorrentEngine.getInstance().getSettings();
            s.uploadRateLimit = sharedPreferences.getInt(key, SettingsManager.Default.maxUploadSpeedLimit);
            TorrentEngine.getInstance().setSettings(s);
        } else if (key.equals(getString(R.string.pref_key_max_connections))) {
            TorrentEngine.Settings s = TorrentEngine.getInstance().getSettings();
            s.connectionsLimit = sharedPreferences.getInt(key, SettingsManager.Default.maxConnections);
            s.maxPeerListSize = s.connectionsLimit;
            TorrentEngine.getInstance().setSettings(s);
        } else if (key.equals(getString(R.string.pref_key_max_connections_per_torrent))) {
            TorrentEngine.getInstance().setMaxConnectionsPerTorrent(sharedPreferences.getInt(key,
                    SettingsManager.Default.maxConnectionsPerTorrent));
        } else if (key.equals(getString(R.string.pref_key_max_uploads_per_torrent))) {
            TorrentEngine.getInstance().setMaxUploadsPerTorrent(sharedPreferences.getInt(key,
                    SettingsManager.Default.maxUploadsPerTorrent));
        } else if (key.equals(getString(R.string.pref_key_max_active_downloads))) {
            TorrentEngine.Settings s = TorrentEngine.getInstance().getSettings();
            s.activeDownloads = sharedPreferences.getInt(key, SettingsManager.Default.maxActiveDownloads);
            TorrentEngine.getInstance().setSettings(s);
        } else if (key.equals(getString(R.string.pref_key_max_active_uploads))) {
            TorrentEngine.Settings s = TorrentEngine.getInstance().getSettings();
            s.activeSeeds = sharedPreferences.getInt(key, SettingsManager.Default.maxActiveUploads);
            TorrentEngine.getInstance().setSettings(s);
        } else if (key.equals(getString(R.string.pref_key_max_active_torrents))) {
            TorrentEngine.Settings s = TorrentEngine.getInstance().getSettings();
            s.activeLimit = sharedPreferences.getInt(key, SettingsManager.Default.maxActiveTorrents);
            TorrentEngine.getInstance().setSettings(s);
        } else if (key.equals(getString(R.string.pref_key_enable_dht))) {
            TorrentEngine.Settings s = TorrentEngine.getInstance().getSettings();
            s.dhtEnabled = sharedPreferences.getBoolean(getString(R.string.pref_key_enable_dht),
                    SettingsManager.Default.enableDht);
            TorrentEngine.getInstance().setSettings(s);
        } else if (key.equals(getString(R.string.pref_key_enable_lsd))) {
            TorrentEngine.Settings s = TorrentEngine.getInstance().getSettings();
            s.lsdEnabled = sharedPreferences.getBoolean(getString(R.string.pref_key_enable_lsd),
                    SettingsManager.Default.enableLsd);
            TorrentEngine.getInstance().setSettings(s);
        } else if (key.equals(getString(R.string.pref_key_enable_utp))) {
            TorrentEngine.Settings s = TorrentEngine.getInstance().getSettings();
            s.utpEnabled = sharedPreferences.getBoolean(getString(R.string.pref_key_enable_utp),
                    SettingsManager.Default.enableUtp);
            TorrentEngine.getInstance().setSettings(s);
        } else if (key.equals(getString(R.string.pref_key_enable_upnp))) {
            TorrentEngine.Settings s = TorrentEngine.getInstance().getSettings();
            s.upnpEnabled = sharedPreferences.getBoolean(getString(R.string.pref_key_enable_upnp),
                    SettingsManager.Default.enableUpnp);
            TorrentEngine.getInstance().setSettings(s);
        } else if (key.equals(getString(R.string.pref_key_enable_natpmp))) {
            TorrentEngine.Settings s = TorrentEngine.getInstance().getSettings();
            s.natPmpEnabled = sharedPreferences.getBoolean(getString(R.string.pref_key_enable_natpmp),
                    SettingsManager.Default.enableNatPmp);
            TorrentEngine.getInstance().setSettings(s);
        } else if (key.equals(getString(R.string.pref_key_enc_mode))) {
            TorrentEngine.Settings s = TorrentEngine.getInstance().getSettings();
            s.encryptMode = getEncryptMode();
            TorrentEngine.getInstance().setSettings(s);
        } else if (key.equals(getString(R.string.pref_key_enc_in_connections))) {
            TorrentEngine.Settings s = TorrentEngine.getInstance().getSettings();
            int state = settings_pack.enc_policy.pe_disabled.swigValue();
            s.encryptInConnections = sharedPreferences.getBoolean(getString(R.string.pref_key_enc_in_connections),
                    SettingsManager.Default.encryptInConnections);
            if (s.encryptInConnections) {
                state = getEncryptMode();
            }
            s.encryptMode = state;
            TorrentEngine.getInstance().setSettings(s);
        } else if (key.equals(getString(R.string.pref_key_enc_out_connections))) {
            TorrentEngine.Settings s = TorrentEngine.getInstance().getSettings();
            int state = settings_pack.enc_policy.pe_disabled.swigValue();
            s.encryptOutConnections = sharedPreferences.getBoolean(getString(R.string.pref_key_enc_out_connections),
                    SettingsManager.Default.encryptOutConnections);
            if (s.encryptOutConnections) {
                state = getEncryptMode();
            }
            s.encryptMode = state;
            TorrentEngine.getInstance().setSettings(s);
        } else if (key.equals(getString(R.string.pref_key_use_random_port))) {
            if (sharedPreferences.getBoolean(getString(R.string.pref_key_use_random_port),
                    SettingsManager.Default.useRandomPort))
                TorrentEngine.getInstance().setRandomPort();
            else
                TorrentEngine.getInstance().setPort(sharedPreferences.getInt(getString(R.string.pref_key_port),
                        SettingsManager.Default.port));
        } else if (key.equals(getString(R.string.pref_key_port))) {
            TorrentEngine.getInstance().setPort(sharedPreferences.getInt(getString(R.string.pref_key_port),
                    SettingsManager.Default.port));
        } else if (key.equals(getString(R.string.pref_key_enable_ip_filtering))) {
            if (sharedPreferences.getBoolean(getString(R.string.pref_key_enable_ip_filtering),
                    SettingsManager.Default.enableIpFiltering))
                TorrentEngine.getInstance().enableIpFilter(
                        sharedPreferences.getString(getString(R.string.pref_key_ip_filtering_file),
                                SettingsManager.Default.ipFilteringFile));
            else
                TorrentEngine.getInstance().disableIpFilter();
        } else if (key.equals(getString(R.string.pref_key_ip_filtering_file))) {
            TorrentEngine.getInstance().enableIpFilter(
                    sharedPreferences.getString(getString(R.string.pref_key_ip_filtering_file),
                            SettingsManager.Default.ipFilteringFile));
        } else if (key.equals(getString(R.string.pref_key_apply_proxy))) {
            sharedPreferences.edit().putBoolean(getString(R.string.pref_key_proxy_changed), false).apply();
            setProxy();
            Toast.makeText(getApplicationContext(),
                    R.string.proxy_settings_applied,
                    Toast.LENGTH_SHORT)
                    .show();
        } else if (key.equals(getString(R.string.pref_key_auto_manage))) {
            TorrentEngine.getInstance().setAutoManaged(sharedPreferences.getBoolean(key,
                    SettingsManager.Default.autoManage));
        } else if (key.equals(getString(R.string.pref_key_streaming_enable))) {
            if (sharedPreferences.getBoolean(key, SettingsManager.Default.enableStreaming))
                startStreamingServer();
            else
                stopStreamingServer();
        } else if (key.equals(getString(R.string.pref_key_streaming_port)) ||
                key.equals(getString(R.string.pref_key_streaming_hostname))) {
            startStreamingServer();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancelAll();
        }

        if (!isAlreadyRunning) {
            isAlreadyRunning = true;
            init();
        }

        if (intent != null && intent.getAction() != null) {
            boolean wifiOnly = sharedPreferences.getBoolean(getString(R.string.pref_key_wifi_only), SettingsManager.Default.wifiOnly);

            switch (intent.getAction()) {
                case NotificationReceiver.NOTIFY_ACTION_SHUTDOWN_APP:
                case ACTION_SHUTDOWN:
                    if (shutdownThread != null && !shutdownThread.isAlive()) {
                        shutdownThread.start();
                    }
                    return START_NOT_STICKY;
                case WifiReceiver.ACTION_WIFI_ENABLED:
                    if (wifiOnly) {
                        pauseTorrents.set(false);
                        TorrentEngine.getInstance().resumeAll();
                    }
                    break;
                case WifiReceiver.ACTION_WIFI_DISABLED:
                    if (wifiOnly) {
                        pauseTorrents.set(true);
                        TorrentEngine.getInstance().pauseAll();
                    }
                    break;
                case ACTION_ADD_TORRENT: {
                    AddTorrentParams params = intent.getParcelableExtra(TAG_ADD_TORRENT_PARAMS);
                    boolean saveFile = intent.getBooleanExtra(TAG_SAVE_TORRENT_FILE, false);
                    try {
                        TorrentHelper.addTorrent(getApplicationContext(), params, !saveFile);
                    } catch (Throwable e) {
                        handleAddTorrentError(params, e);
                    }
                    break;
                }
            }
        }

        return START_STICKY;
    }

    /*
     * Return pause state
     */
    private boolean checkPauseControl() {
        Context context = getApplicationContext();
        boolean wifiOnly = sharedPreferences.getBoolean(getString(R.string.pref_key_wifi_only), SettingsManager.Default.wifiOnly);

        try {
            unregisterReceiver(wifiReceiver);
        } catch (IllegalArgumentException e) {
            /* Ignore non-registered receiver */
        }
        if (wifiOnly) {
            registerReceiver(wifiReceiver, WifiReceiver.getFilter());
        }

        boolean pause = false;
        if (wifiOnly) {
            pause = !Utils.isWifiEnabled(context);
        }
        pauseTorrents.set(pause);

        return pause;
    }

    private int getEncryptMode() {
        int mode = sharedPreferences.getInt(getString(R.string.pref_key_enc_mode), SettingsManager.Default.encryptMode(getApplicationContext()));

        if (mode == Integer.parseInt(getString(R.string.pref_enc_mode_prefer_value))) {
            return settings_pack.enc_policy.pe_enabled.swigValue();
        } else if (mode == Integer.parseInt(getString(R.string.pref_enc_mode_require_value))) {
            return settings_pack.enc_policy.pe_forced.swigValue();
        } else {
            return settings_pack.enc_policy.pe_disabled.swigValue();
        }
    }

    private void handleAddTorrentError(AddTorrentParams params, Throwable e) {
        if (e instanceof FileAlreadyExistsException) {
            makeTorrentInfoNotify(params.getName(), getString(R.string.torrent_exist));
            return;
        }
        Log.e(TAG, Log.getStackTraceString(e));
        String message;
        if (e instanceof FileNotFoundException)
            message = getString(R.string.error_file_not_found_add_torrent);
        else if (e instanceof IOException)
            message = getString(R.string.error_io_add_torrent);
        else
            message = getString(R.string.error_add_torrent);
        makeTorrentErrorNotify(params.getName(), message);
    }

    private void init() {
        shutdownThread = new Thread() {
            @Override
            public void run()
            {
                stopService();
            }
        };
        Context context = getApplicationContext();
        repo = new TorrentStorage(context);

        notificationManager =(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        makeNotificationChannels(notificationManager);
        sharedPreferences = SettingsManager.getPreferences(getApplicationContext());
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        TorrentEngine.getInstance().setContext(context);
        TorrentEngine.getInstance().setCallback(this);
        TorrentEngine.getInstance().setSettings(SettingsManager.readEngineSettings(context));
        TorrentEngine.getInstance().start();

        checkPauseControl();
        makeForegroundNotification();
        startUpdateForegroundNotification();
    }

    private NotificationCompat.InboxStyle makeDetailNotificationInboxStyle() {
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        String titleTemplate = getString(R.string.torrent_count_notify_template);
        int downloadingCount = 0;

        for (TorrentDownload task : TorrentEngine.getInstance().getTasks()) {
            if (task == null) {
                continue;
            }

            String template;
            TorrentStateCode code = task.getStateCode();

            if (code == TorrentStateCode.DOWNLOADING) {
                ++downloadingCount;
                template =  getString(R.string.downloading_torrent_notify_template);
                inboxStyle.addLine(
                        String.format(
                                template,
                                task.getProgress(),
                                (task.getETA() == -1) ? Utils.INFINITY_SYMBOL :
                                        DateUtils.formatElapsedTime(task.getETA()),
                                Formatter.formatFileSize(this, task.getDownloadSpeed()),
                                task.getTorrent().getName()));

            } else if (code == TorrentStateCode.SEEDING) {
                template = getString(R.string.seeding_torrent_notify_template);
                inboxStyle.addLine(
                        String.format(
                                template,
                                getString(R.string.torrent_status_seeding),
                                Formatter.formatFileSize(this, task.getUploadSpeed()),
                                task.getTorrent().getName()));
            } else {
                String stateString = "";

                switch (task.getStateCode()) {
                    case PAUSED:
                        stateString = getString(R.string.torrent_status_paused);
                        break;
                    case STOPPED:
                        stateString = getString(R.string.torrent_status_stopped);
                        break;
                    case CHECKING:
                        stateString = getString(R.string.torrent_status_checking);
                        break;
                    case DOWNLOADING_METADATA:
                        stateString = getString(R.string.torrent_status_downloading_metadata);
                }

                template = getString(R.string.other_torrent_notify_template);
                inboxStyle.addLine(
                        String.format(
                                template,
                                stateString,
                                task.getTorrent().getName()));
            }
        }

        inboxStyle.setBigContentTitle(String.format(
                titleTemplate,
                downloadingCount,
                TorrentEngine.getInstance().tasksCount()));

        inboxStyle.setSummaryText((isNetworkOnline ?
                getString(R.string.network_online) :
                getString(R.string.network_offline)));

        return inboxStyle;
    }

    private void loadTorrents(Collection<Torrent> torrents) {
        if (torrents == null) {
            return;
        }

        ArrayList<Torrent> loadList = new ArrayList<>();
        for (Torrent torrent : torrents) {
            if (!torrent.isDownloadingMetadata() &&
                    !TorrentUtils.torrentFileExists(getApplicationContext(), torrent.getId())) {
                Log.e(TAG, "Torrent doesn't exists: " + torrent);
                makeTorrentErrorNotify(torrent.getName(), getString(R.string.torrent_does_not_exists_error));
                repo.delete(torrent);
            } else {
                loadList.add(torrent);
            }
        }

        TorrentEngine.getInstance().restoreDownloads(loadList);
    }

    private void makeForegroundNotification() {
        Intent startupIntent = new Intent(getApplicationContext(), MainActivity.class);
        startupIntent.setAction(Intent.ACTION_MAIN);
        startupIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        startupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent startupPendingIntent = PendingIntent.getActivity(
            getApplicationContext(),
            0,
            startupIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        );

        foregroundNotification = new NotificationCompat.Builder(getApplicationContext(), FOREGROUND_NOTIFY_CHAN_ID)
                .setSmallIcon(R.mipmap.ic_captain_launcher_round)
                .setContentIntent(startupPendingIntent)
                .setContentTitle(getString(R.string.app_running_in_the_background))
                .setTicker(getString(R.string.app_running_in_the_background))
                .setContentText((isNetworkOnline ?
                        getString(R.string.network_online) :
                        getString(R.string.network_offline))
                )
                .setWhen(System.currentTimeMillis())
                .setCategory(Notification.CATEGORY_SERVICE);

        //foregroundNotification.addAction(makeFuncButtonAction());
        foregroundNotification.addAction(makeShutdownAction());

        startForeground(SERVICE_STARTED_NOTIFICATION_ID, foregroundNotification.build());
    }

    private void makeNotificationChannels(NotificationManager notifyManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        ArrayList<NotificationChannel> channels = new ArrayList<>();
        NotificationChannel defaultChannel = new NotificationChannel(DEFAULT_CHAN_ID, getString(R.string.def), NotificationManager.IMPORTANCE_DEFAULT);
        channels.add(defaultChannel);
        channels.add(new NotificationChannel(FOREGROUND_NOTIFY_CHAN_ID, getString(R.string.foreground_notification), NotificationManager.IMPORTANCE_LOW));
        notifyManager.createNotificationChannels(channels);
    }

    /*
     * For shutdown activity and service
     */
    private NotificationCompat.Action makeShutdownAction() {
        Intent shutdownIntent = new Intent(getApplicationContext(), NotificationReceiver.class);
        shutdownIntent.setAction(NotificationReceiver.NOTIFY_ACTION_SHUTDOWN_APP);
        PendingIntent shutdownPendingIntent =
                PendingIntent.getBroadcast(
                        getApplicationContext(),
                        0,
                        shutdownIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Action.Builder(
                R.drawable.ic_power_settings_new_white_24dp,
                getString(R.string.shutdown),
                shutdownPendingIntent
        ).build();
    }

    private void makeFinishNotify(Torrent torrent) {
        if (torrent == null || !sharedPreferences.getBoolean(
                getString(R.string.pref_key_torrent_finish_notify),
                SettingsManager.Default.torrentFinishNotify
        )) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(),
                DEFAULT_CHAN_ID)
                .setSmallIcon(R.drawable.ic_done_white_24dp)
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.primary))
                .setContentTitle(getString(R.string.torrent_finished_notify))
                .setTicker(getString(R.string.torrent_finished_notify))
                .setContentText(torrent.getName())
                .setWhen(System.currentTimeMillis());

        if (sharedPreferences.getBoolean(getString(R.string.pref_key_play_sound_notify),
                SettingsManager.Default.playSoundNotify)) {
            Uri sound = Uri.parse(sharedPreferences.getString(getString(R.string.pref_key_notify_sound),
                    SettingsManager.Default.notifySound));
            builder.setSound(sound);
        }

        if (sharedPreferences.getBoolean(getString(R.string.pref_key_vibration_notify),
                SettingsManager.Default.vibrationNotify))
            builder.setVibrate(new long[] {1000}); /* ms */

        if (sharedPreferences.getBoolean(getString(R.string.pref_key_led_indicator_notify),
                SettingsManager.Default.ledIndicatorNotify)) {
            int color = sharedPreferences.getInt(getString(R.string.pref_key_led_indicator_color_notify),
                    SettingsManager.Default.ledIndicatorColorNotify(getApplicationContext()));
            builder.setLights(color, 1000, 1000); /* ms */
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            builder.setCategory(Notification.CATEGORY_STATUS);

        notificationManager.notify(torrent.getId().hashCode(), builder.build());
    }

    private void makeTorrentErrorNotify(String name, String message) {
        if (name == null || message == null)
            return;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(),
                DEFAULT_CHAN_ID)
                .setSmallIcon(R.drawable.ic_error_white_24dp)
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.primary))
                .setContentTitle(name)
                .setTicker(getString(R.string.torrent_error_notify_title))
                .setContentText(String.format(getString(R.string.error_template), message))
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            builder.setCategory(Notification.CATEGORY_ERROR);

        notificationManager.notify(name.hashCode(), builder.build());
    }

    private void makeSessionErrorNotify(String message) {
        if (message == null)
            return;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(),
                DEFAULT_CHAN_ID)
                .setSmallIcon(R.drawable.ic_error_white_24dp)
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.primary))
                .setContentTitle(getString(R.string.session_error_title))
                .setTicker(getString(R.string.session_error_title))
                .setContentText(String.format(getString(R.string.error_template), message))
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            builder.setCategory(Notification.CATEGORY_ERROR);

        notificationManager.notify(SESSION_ERROR_NOTIFICATION_ID, builder.build());
    }

    private void makeNatErrorNotify(String message) {
        if (message == null)
            return;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(),
                DEFAULT_CHAN_ID)
                .setSmallIcon(R.drawable.ic_error_white_24dp)
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.primary))
                .setContentTitle(getString(R.string.nat_error_title))
                .setTicker(getString(R.string.nat_error_title))
                .setContentText(String.format(getString(R.string.error_template), message))
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            builder.setCategory(Notification.CATEGORY_ERROR);

        notificationManager.notify(NAT_ERROR_NOTIFICATION_ID, builder.build());
    }

    private void makeTorrentInfoNotify(String name, String message) {
        if (name == null || message == null) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(),
                DEFAULT_CHAN_ID)
                .setSmallIcon(R.drawable.ic_info_white_24dp)
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.primary))
                .setContentTitle(name)
                .setTicker(message)
                .setContentText(message)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            builder.setCategory(Notification.CATEGORY_STATUS);

        notificationManager.notify(name.hashCode(), builder.build());
    }

    private void makeTorrentAddedNotify(String name) {
        if (name == null)
            return;

        String title = getString(R.string.torrent_added_notify);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(),
                DEFAULT_CHAN_ID)
                .setSmallIcon(R.drawable.ic_done_white_24dp)
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.primary))
                .setContentTitle(title)
                .setTicker(title)
                .setContentText(name)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            builder.setCategory(Notification.CATEGORY_STATUS);

        notificationManager.notify(name.hashCode(), builder.build());
    }

    private void sendBasicState(TorrentDownload task) {
        if (task == null) {
            return;
        }
        Torrent torrent = task.getTorrent();
        if (torrent == null) {
            return;
        }

        BasicStateParcel state = TorrentHelper.makeBasicStateParcel(task);
        if (basicStateCache.contains(state)) {
            return;
        }
        basicStateCache.put(state);
        TorrentTaskServiceReceiver.getInstance().post(TorrentStateMsg.makeUpdateTorrentBundle(state));
    }

    private void setProxy() {
        ProxySettingsPack proxy = new ProxySettingsPack();
        ProxySettingsPack.ProxyType type = ProxySettingsPack.ProxyType.fromValue(
                sharedPreferences.getInt(getString(R.string.pref_key_proxy_type), SettingsManager.Default.proxyType)
        );
        proxy.setType(type);
        if (type == ProxySettingsPack.ProxyType.NONE) {
            TorrentEngine.getInstance().setProxy(getApplicationContext(), proxy);
        }
        proxy.setAddress(
                sharedPreferences.getString(
                        getString(R.string.pref_key_proxy_address),
                        SettingsManager.Default.proxyAddress
                )
        );
        proxy.setPort(
                sharedPreferences.getInt(
                        getString(R.string.pref_key_proxy_port),
                        SettingsManager.Default.proxyPort
                )
        );
        proxy.setProxyPeersToo(
                sharedPreferences.getBoolean(
                        getString(R.string.pref_key_proxy_peers_too),
                        SettingsManager.Default.proxyPeersToo
                )
        );
        if (sharedPreferences.getBoolean(
                getString(R.string.pref_key_proxy_requires_auth), SettingsManager.Default.proxyRequiresAuth
        )) {
            proxy.setLogin(
                    sharedPreferences.getString(getString(R.string.pref_key_proxy_login),
                    SettingsManager.Default.proxyLogin)
            );
            proxy.setPassword(
                    sharedPreferences.getString(getString(R.string.pref_key_proxy_password),
                    SettingsManager.Default.proxyPassword)
            );
        }
        TorrentEngine.getInstance().setProxy(getApplicationContext(), proxy);
    }

    private void startStreamingServer() {
        stopStreamingServer();

        String hostname = sharedPreferences.getString(getString(R.string.pref_key_streaming_hostname), SettingsManager.Default.streamingHostname);
        int port = sharedPreferences.getInt(getString(R.string.pref_key_streaming_port), SettingsManager.Default.streamingPort);

        torrentStreamServer = new TorrentStreamServer(hostname, port);
        try {
            torrentStreamServer.start();
        } catch (IOException e) {
            makeTorrentErrorNotify(null, getString(R.string.pref_streaming_error));
        }
    }

    private void startUpdateForegroundNotification() {
        if (updateForegroundNotificationHandler != null) {
            return;
        }
        updateForegroundNotificationHandler = new Handler();
        updateForegroundNotificationHandler.postDelayed(updateForegroundNotification, SYNC_TIME);
    }

    private void stopService() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        try {
            unregisterReceiver(wifiReceiver);
        } catch (IllegalArgumentException e) {
            /* Ignore non-registered receiver */
        }
        stopUpdateForegroundNotification();
        TorrentEngine.getInstance().stop();
        isAlreadyRunning = false;
        repo = null;
        sharedPreferences = null;
        stopForeground(true);
        stopSelf();
    }

    private void stopStreamingServer() {
        if (torrentStreamServer != null) {
            torrentStreamServer.stop();
        }
        torrentStreamServer = null;
    }

    private void stopUpdateForegroundNotification() {
        if (updateForegroundNotificationHandler == null) {
            return;
        }
        updateForegroundNotificationHandler.removeCallbacks(updateForegroundNotification);
    }

    private Runnable updateForegroundNotification = new Runnable() {
        @Override
        public void run() {
            if (isAlreadyRunning) {
                boolean online = TorrentEngine.getInstance().isConnected();
                if (isNetworkOnline != online) {
                    isNetworkOnline = online;
                    isNeededToUpdateNotification.set(true);
                }

                if (isNeededToUpdateNotification.get()) {
                    try {
                        isNeededToUpdateNotification.set(false);
                        if (foregroundNotification != null) {
                            foregroundNotification.setContentText((isNetworkOnline ? getString(R.string.network_online) : getString(R.string.network_offline)));
                            if (!TorrentEngine.getInstance().hasTasks())
                                foregroundNotification.setStyle(makeDetailNotificationInboxStyle());
                            else
                                foregroundNotification.setStyle(null);
                            /* Disallow killing the service process by system */
                            startForeground(SERVICE_STARTED_NOTIFICATION_ID, foregroundNotification.build());
                        }

                    } catch (Exception e) {
                        /* Ignore */
                    }
                }
            }
            updateForegroundNotificationHandler.postDelayed(this, SYNC_TIME);
        }
    };

    private void updateForegroundNotificationActions() {
        if (foregroundNotification == null) {
            return;
        }
        foregroundNotification.mActions.clear();
        //foregroundNotification.addAction(makeFuncButtonAction());
        foregroundNotification.addAction(makeShutdownAction());
        startForeground(SERVICE_STARTED_NOTIFICATION_ID, foregroundNotification.build());
    }
}
