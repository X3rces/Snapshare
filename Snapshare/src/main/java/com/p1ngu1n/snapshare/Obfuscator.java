package com.p1ngu1n.snapshare;

/**
 * Obfuscator.java created on 2013-12-12.
 *
 * Copyright (C) 2013 Alec McGavin <alec.mcgavin@gmail.com>
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
 * This helps with the new obfuscation in snapchat version 4.0.21+
 */
public enum Obfuscator {
    // com.snapchat.android.camera.Camera(Preview)Fragment // Deprecated since 5.0.36.0
    CAMERA_LOAD (new String[] {"refreshFlashButton", "k", "l", "e", "e", "p", "q", "q", "r", null}),
    // com.snapchat.android.util.eventbus.BusProvider
    GET_BUS (new String[] {"getInstance", "a", "a", "a", "a", "a", "a", "a", "a", "a"}),
    // com.squareup.otto.Bus
    BUS_POST (new String[] {"post", "c", "c", "c", "c", "a", "a", "a", "a", "a"}),
    // com.snapchat.android.model.Snapbryo.Builder // Parameters depend on media type (Uri or Bitmap)
    BUILDER_CONSTRUCTOR (new String[] {null, null, null, "a", "a", "a", "a", "a", "a", "a"}),
    // com.snapchat.android.model.Snapbryo.Builder
    CREATE_SNAPBRYO (new String[] {null, null, null, "a", "a", "a", "a", "a", "a", "a"});

    // com.snapchat.android.camera.Camera(Preview)Fragment
    // Called from onReady event, which is called from
    public static final String CAMERA_STATE_EVENT = "onCameraStateEvent";

    public static final int FOUR_20 = 0;
    public static final int FOUR_21 = 1;
    public static final int FOUR_22 = 2;
    public static final int FOUR_ONE_TEN = 3;
    public static final int FOUR_ONE_TWELVE = 4;
    public static final int FIVE_ZERO_TWO = 5;
    public static final int FIVE_ZERO_NINE = 6;
    public static final int FIVE_ZERO_TWENTYTHREE = 7;
    public static final int FIVE_ZERO_THIRTYTWO = 8;
    public static final int FIVE_ZERO_THIRTYSIX = 9;

    private final String[] v;

    private Obfuscator(String[] v) {
        this.v = v;
    }

    /** 
     * Gets the method name to hook
     * @param version snapchat version
     * @return the actual method name
     */
    public String getValue(int version) {
        return this.v[version];
    }

    public static int getVersion(int version) {
        if (version >= 420) {
            return Obfuscator.FIVE_ZERO_THIRTYSIX;
        }
        else if (version >= 352) {
            return Obfuscator.FIVE_ZERO_THIRTYTWO;
        }
        else if (version >= 323) {
            return Obfuscator.FIVE_ZERO_TWENTYTHREE;
        }
        else if (version >= 298) {
            return Obfuscator.FIVE_ZERO_NINE;
        }
        else if (version >= 274) {
            return Obfuscator.FIVE_ZERO_TWO;
        }
        else if (version >= 222) {
            return Obfuscator.FOUR_ONE_TWELVE;
        }
        else if (version >= 218) {
            return Obfuscator.FOUR_ONE_TEN;
        }
        else if (version >= 181) {
            return Obfuscator.FOUR_22;
        }
        else if (version >= 175) {
            return Obfuscator.FOUR_21;
        }
        else { // version < 175
            return Obfuscator.FOUR_20;
        }
    }
}
