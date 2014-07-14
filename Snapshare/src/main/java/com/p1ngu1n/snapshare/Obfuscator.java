package com.p1ngu1n.snapshare;
/**
Obfuscator.java created on 12/12/13.

Copyright (C) 2013 Alec McGavin <alec.mcgavin@gmail.com>

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

/** 
 * 
 * This helps with the new obfuscation in snapchat version 4.0.21+
 *
 */
public enum Obfuscator {
    // com.snapchat.android.camera.Camera(Preview)Fragment
    CAMERA_LOAD (new String[] {"refreshFlashButton", "k", "l", "e", "e", "p", "q", "q"}),
    // com.snapchat.android.util.eventbus.BusProvider
    GET_BUS (new String[] {"getInstance", "a", "a", "a", "a", "a", "a", "a"}),
    // com.squareup.otto.Bus
    BUS_POST (new String[] {"post", "c", "c", "c", "c", "a", "a", "a"}),
    // com.snapchat.android.SnapPreviewFragment
    M_SNAP_C_EVENT (new String[] {"mSnapCapturedEvent", "w", "w", "v", "v", "u", "u", "u"}),
    // com.snapchat.android.SnapPreviewFragment
    ON_BACK_PRESS (new String[] {"onDelegatedBackPress", "m", "m", "c", "o" ,"l", "l", "q"}),
    // com.snapchat.android.model.Snapbryo.Builder
    BUILDER_CONSTRUCTOR (new String[] {"a", "a", "a", "a", "a", "a", "a", "a"}),
    // com.snapchat.android.model.Snapbryo.Builder
    CREATE_SNAPBRYO (new String[] {"a", "a", "a", "a", "a", "a", "a", "a"});

    public static final int FOUR_20 = 0;
    public static final int FOUR_21 = 1;
    public static final int FOUR_22 = 2;
    public static final int FOUR_ONE_TEN = 3;
    public static final int FOUR_ONE_TWELVE = 4;
    public static final int FIVE_ZERO_TWO = 5;
    public static final int FIVE_ZERO_NINE = 6;
    public static final int FIVE_ZERO_TWENTYTHREE = 7;

    private String[] v;

    Obfuscator(String[] v) {
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
}
