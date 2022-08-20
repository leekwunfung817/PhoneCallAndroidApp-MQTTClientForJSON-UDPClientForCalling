package com.cpos.cposmonitor;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

public class SettingsActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment(getApplication()))
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

//        findViewById(R.id.)



    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        private static MyApplication thApp;

        SettingsFragment(Context context)
        {
            thApp = (MyApplication)context;
        }
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            SwitchPreferenceCompat preferenceRing = (SwitchPreferenceCompat)findPreference(MyApplication.SettingKey.SETTINGS_ALERT_RING.value);

            preferenceRing.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener()
                    {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue)
                        {
                            boolean isChecked = preferenceRing.isChecked();
                            MyApplication._this.set(MyApplication.SettingKey.SETTINGS_ALERT_RING, isChecked);
                            return true;
                        }
                    }
            );

            SwitchPreferenceCompat preferenceVibrate = (SwitchPreferenceCompat)findPreference(MyApplication.SettingKey.SETTINGS_ALERT_VIBRATE.value);

            preferenceVibrate.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener()
                    {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue)
                        {
                            boolean isChecked = preferenceVibrate.isChecked();
                            MyApplication._this.set(MyApplication.SettingKey.SETTINGS_ALERT_VIBRATE, isChecked);
                            return true;
                        }
                    }
            );



        }

        public void onSharedPreferenceChanged(SharedPreferences prefs, String s) {
            Preference preference = findPreference(s);
            if (s.equals("settings_alert_ring")) {
                boolean isChecked = ((SwitchPreferenceCompat) (findPreference(MyApplication.SettingKey.SETTINGS_ALERT_RING.value))).isChecked();
                MyApplication._this.set(MyApplication.SettingKey.SETTINGS_ALERT_RING, isChecked);
            } else if (s.equals("settings_alert_vibrate")) {
                boolean isChecked = ((SwitchPreferenceCompat) (findPreference(MyApplication.SettingKey.SETTINGS_ALERT_VIBRATE.value))).isChecked();
                MyApplication._this.set(MyApplication.SettingKey.SETTINGS_ALERT_VIBRATE, isChecked);
            } else { //do whatever you want for other Preferences }

            }
        }
    }

}