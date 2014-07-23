package com.p1ngu1n.snapshare;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

/**
 Commons.java created on 7/11/14.

 Copyright (C) 2014 P1nGu1n

 This file is part of Snapshare.

 Snapshare is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Snapshare is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 a gazillion times. If not, see <http://www.gnu.org/licenses/>.
 */
public class Utils {
    private static final XSharedPreferences preferences = new XSharedPreferences("com.p1ngu1n.snapshare");

    /**
     * Converts the content:// scheme to the file:// scheme
     * @param contentResolver
     * @param contentUri The URI to be converted using content:// scheme
     * @return The converted URI using file:// scheme
     */
    public static Uri convertContentToFileUri(ContentResolver contentResolver, Uri contentUri) {
        String [] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = contentResolver.query(contentUri, projection, null, null, null);

        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String filePath = cursor.getString(column_index);
            cursor.close();

            // Convert filepath to URI
            return Uri.fromFile(new File(filePath));
        } else {
            return null;
        }
    }

    /**
     * Refreshes preferences
     */
    public static void refreshPreferences() {
        preferences.reload();
        Commons.ROTATION_MODE = Integer.parseInt(preferences.getString("pref_rotation", Integer.toString(Commons.ROTATION_MODE)));
        Commons.ADJUST_METHOD = Integer.parseInt(preferences.getString("pref_adjustment", Integer.toString(Commons.ADJUST_METHOD)));
        Commons.DEBUGGING = preferences.getBoolean("pref_debug", Commons.DEBUGGING);
    }

    public static String formatBytes(long bytes) {
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String[] prefixes = new String[]{ "", "K", "M", "G", "T", "P", "E" };
        String prefix = prefixes[exp];
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), prefix);
    }

    /**
     * Write debug information to the Xposed Log if enabled in the settings
     * @param message The message you want to log
     * @param prefix Whether it should be prefixed by the log-tag
     */
    public static void xposedDebug(String message, boolean prefix) {
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
    public static void xposedDebug(String message) {
        xposedDebug(message, true);
    }
}
