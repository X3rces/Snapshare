package com.p1ngu1n.snapshare;
/**
 SettingsFragment.java created on 22/12/13.

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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

/**
 * Class to hold all the regular settings
 *
 */
public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {
    private static final int CLICKS_REQUIRED = 3;
    private int hitCounter;
    private long firstHitTimestamp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesMode(1);
        addPreferencesFromResource(R.xml.regular_settings);

        Preference launcherPref = findPreference("pref_launcher");
        launcherPref.setOnPreferenceChangeListener(launcherChangeListener);

        Preference aboutPreference = findPreference("pref_about");
        aboutPreference.setOnPreferenceClickListener(this);

        try {
            String versionName = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
            aboutPreference.setTitle(getString(R.string.pref_about_title, versionName));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        updateSummary("pref_adjustment");
        updateSummary("pref_rotation");
    }

    /**
     * Set the selected value as summary for a fragment
     * @param key the preference's key
     */
    private void updateSummary(String key) {
        Preference pref = findPreference(key);

        if(pref instanceof ListPreference) {
            ListPreference lp = (ListPreference) pref;
            lp.setSummary(lp.getEntry());
        }
    }

    private final Preference.OnPreferenceChangeListener launcherChangeListener = new Preference.OnPreferenceChangeListener() {

        public boolean onPreferenceChange(Preference preference, Object newValue) {
            int state = ((Boolean) newValue ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED);

            Activity activity = getActivity();
            ComponentName alias = new ComponentName(activity, "com.p1ngu1n.snapshare.SettingsActivity-Alias");
            PackageManager p = activity.getPackageManager();
            p.setComponentEnabledSetting(alias, state, PackageManager.DONT_KILL_APP);
            return true;
        }
    };

    @Override
    public boolean onPreferenceClick(Preference preference) {
        long currentTimestamp = System.currentTimeMillis();
        if (firstHitTimestamp < (currentTimestamp - 500)) {
            hitCounter = 1;
            firstHitTimestamp = currentTimestamp;
        } else {
            hitCounter++;
        }

        if (hitCounter == CLICKS_REQUIRED) {
            hitCounter = 0;
            Intent myIntent = new Intent(getActivity(), DeveloperSettingsActivity.class);
            getActivity().startActivity(myIntent);
        }

        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateSummary(key);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

}
