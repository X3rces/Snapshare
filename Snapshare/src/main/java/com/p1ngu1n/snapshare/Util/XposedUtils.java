package com.p1ngu1n.snapshare.Util;

/**
 * XposedUtils.java created on 2014-01-14.
 *
 * Copyright (C) 2014 P1nGu1n
 *
 * This file is part of Snapshare.
 *
 * Snapshare is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Snapshare is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * a gazillion times. If not, see <http://www.gnu.org/licenses/>.
 */

import com.p1ngu1n.snapshare.BuildConfig;
import com.p1ngu1n.snapshare.Commons;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

/**
 * A set of commonly used utilities using the Xposed Framework.
 */
public class XposedUtils {
    private static final XSharedPreferences preferences = new XSharedPreferences(BuildConfig.APPLICATION_ID);

    /**
     * Restrict instantiation of this class, it only contains static methods.
     */
    private XposedUtils() { }

    /**
     * Refreshes preferences
     */
    public static void refreshPreferences() {
        preferences.reload();
        Commons.ROTATION_MODE = Integer.parseInt(preferences.getString("pref_rotation", Integer.toString(Commons.ROTATION_MODE)));
        Commons.ADJUST_METHOD = Integer.parseInt(preferences.getString("pref_adjustment", Integer.toString(Commons.ADJUST_METHOD)));
        Commons.DEBUGGING = preferences.getBoolean("pref_debug", Commons.DEBUGGING);
        Commons.CHECK_SIZE = !preferences.getBoolean("pref_size_disabled", !Commons.CHECK_SIZE);
        Commons.TIMBER = preferences.getBoolean("pref_timber", Commons.TIMBER);
    }


    /**
     * Write debug information to the Xposed Log if enabled in the settings
     * @param message The message you want to log
     * @param prefix Whether it should be prefixed by the log-tag
     */
    public static void log(String message, boolean prefix) {
        if (Commons.DEBUGGING) {
            if (prefix) {
                message = Commons.LOG_TAG + message;
            }
            XposedBridge.log(message);
        }
    }

    /**
     * Write debug information to the Xposed Log if enabled in the settings
     * This method always prefixes the message by the log-tag
     * @param message The message you want to log
     */
    public static void log(String message) {
        log(message, true);
    }

    /**
     * Write a throwable to the Xposed Log, even when debugging is disabled.
     * @param throwable The throwable to log
     */
    public static void log(Throwable throwable) {
        XposedBridge.log(throwable);
    }

    /**
     * Write a throwable with a message to the Xposed Log, even when debugging is disabled.
     * @param message The message to log
     * @param throwable The throwable to log after the message
     */
    public static void log(String message, Throwable throwable) {
        log(message);
        log(throwable);
    }
}
