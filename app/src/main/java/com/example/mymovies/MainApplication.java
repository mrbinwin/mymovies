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

package com.example.mymovies;

import android.app.Application;
import android.content.Context;

import org.greenrobot.eventbus.EventBus;


public class MainApplication extends Application {

    private static Application instance;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        EventBus.builder().logNoSubscriberMessages(false).installDefaultEventBus();
    }

    public static Context getContext() {
        return instance.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}
