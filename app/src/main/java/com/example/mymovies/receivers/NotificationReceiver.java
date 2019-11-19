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

package com.example.mymovies.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.mymovies.activities.MainActivity;
import com.example.mymovies.services.TorrentTaskService;

/*
 * The receiver for actions from the app notification
 */

public class NotificationReceiver extends BroadcastReceiver {

    public static final String NOTIFY_ACTION_SHUTDOWN_APP = "com.example.mymovies.receivers.NotificationReceiver.NOTIFY_ACTION_SHUTDOWN_APP";
    public static final String NOTIFY_ACTION_ADD_TORRENT = "com.example.mymovies.receivers.NotificationReceiver.NOTIFY_ACTION_ADD_TORRENT";
    public static final String NOTIFY_ACTION_PAUSE_RESUME = "com.example.mymovies.receivers.NotificationReceiver.NOTIFY_ACTION_PAUSE_RESUME";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        Intent mainIntent, serviceIntent;
        switch (action) {
            /* Send action to the already running service */
            case NOTIFY_ACTION_SHUTDOWN_APP:
                mainIntent = new Intent(context.getApplicationContext(), MainActivity.class);
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                mainIntent.setAction(NOTIFY_ACTION_SHUTDOWN_APP);
                context.startActivity(mainIntent);

                serviceIntent = new Intent(context.getApplicationContext(), TorrentTaskService.class);
                serviceIntent.setAction(NOTIFY_ACTION_SHUTDOWN_APP);
                context.startService(serviceIntent);
                break;
            case NOTIFY_ACTION_ADD_TORRENT:
                mainIntent = new Intent(context.getApplicationContext(), MainActivity.class);
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                mainIntent.setAction(NOTIFY_ACTION_ADD_TORRENT);
                context.startActivity(mainIntent);
                break;
            /*case NOTIFY_ACTION_PAUSE_RESUME:
                serviceIntent = new Intent(context.getApplicationContext(), TorrentTaskService.class);
                serviceIntent.setAction(NOTIFY_ACTION_PAUSE_RESUME);
                context.startService(serviceIntent);
                break;*/
        }
    }
}
