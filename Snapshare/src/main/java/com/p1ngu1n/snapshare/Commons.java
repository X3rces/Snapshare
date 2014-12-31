package com.p1ngu1n.snapshare;

/**
 * Commons.java created on 2014-07-11.
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

/**
 * Fields containing static information.
 *
 */
public class Commons {
    // Debugging settings
    public static final String LOG_TAG = "Snapshare: ";

    // Adjustment methods
    public static final int ADJUST_CROP = 0;
    public static final int ADJUST_SCALE = 1;
    public static final int ADJUST_NONE = 2;

    // Rotation modes
    public static final int ROTATION_NONE = 0;
    public static final int ROTATION_CW = 90;
    public static final int ROTATION_CCW = 270;

    // Preferences and their default values
    public static int ROTATION_MODE = ROTATION_CW;
    public static int ADJUST_METHOD = ADJUST_CROP;
    public static boolean CAPTION_UNLIMITED_VANILLA = false;
    public static boolean CAPTION_UNLIMITED_FAT = false;
    public static boolean DEBUGGING = false;
    public static boolean CHECK_SIZE = true;
    public static boolean TIMBER = false;

    // Maximum video size setting (2.50 MB)
    public static final int MAX_VIDEO_SIZE = (int) (2.50 * 1024 * 1024);
}
