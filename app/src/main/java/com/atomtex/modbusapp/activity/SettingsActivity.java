package com.atomtex.modbusapp.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;

import com.atomtex.modbusapp.R;

public class SettingsActivity extends AppCompatActivity {

    public static final String PREFERENCES_CRC_ORDER = "crcOrder";
    private static final String TAG_SETTINGS_FRAGMENT = "settingsFragment";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        FragmentManager mFragmentManager = getSupportFragmentManager();
        Fragment mFragment = mFragmentManager.findFragmentByTag(TAG_SETTINGS_FRAGMENT);
        if (mFragment == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.settings_container, new SettingsFragment(), TAG_SETTINGS_FRAGMENT).commit();
        }
    }


    public static class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

        SharedPreferences sharedPreferences;

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            getPreferenceManager().setSharedPreferencesName(MainActivity.APP_PREFERENCES);
            addPreferencesFromResource(R.xml.preferences);
            sharedPreferences = getPreferenceManager().getSharedPreferences();
            onSharedPreferenceChanged(sharedPreferences, getString(R.string.preferences_crc_order));
        }

        @Override
        public void onResume() {
            super.onResume();
            //unregister the preferenceChange listener
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference preference = findPreference(key);
        }

        @Override
        public void onPause() {
            super.onPause();
            //unregister the preference change listener
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }
    }
}
