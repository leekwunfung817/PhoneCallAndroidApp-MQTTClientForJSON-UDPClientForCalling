package com.cpos.cposmonitor;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.util.Calendar;
import java.util.TimeZone;


public class MyApplication extends Application {
    public static final int MESSAGE_STATE_CHANGE = 1;

    public static Context mContext;
    public static MyApplication _this;
//    public SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);


    public static String DEFAULT_IP = "115.160.170.198";
    public static String DEFAULT_USERNAME = "cpostest";
    public static String DEFAULT_PASSWORD = "test";

    public static String DEFAULT_MQTT_PORT = "1394";
    public static String DEFAULT_UDP_PORT = "1395";
    public static String DEFAULT_LOCAL_PORT = "12345";
    ;
    public static String DEFAULT_SSL_PASSWORD = "client1392419926";


//    public static String m_strDeviceId;
//    public static String m_strUserName;
//    public static String m_strPassword;
//    public static String m_mqtt_server_ip;
//    public static String m_sound_server_ip;
//    public static String m_sound_server_port;
//    public static String m_sound_local_port;

//    public static boolean mRing = true;
//    public static boolean mVibrate = false;
 
    @Override
    public void onCreate() {
        super.onCreate();
        _this = this;
        mContext = getApplicationContext();

        String device_id = getDeviceID();
        Log.i("Application","Device ID initialize:"+device_id);
        set(SettingKey.SETTINGS_DEVICE_ID, device_id);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        {
            SharedPreferences.Editor editor = preferences.edit();
            int testCount = preferences.getInt("TestCount", 0);
            testCount += 1;
            Log.i("TestCount", "TestCount=" + testCount);
            if (preferences.contains("TestCount")) {
                editor.putInt("TestCount", testCount);
            } else {
                editor.putInt("TestCount", 1);
            }
            editor.commit();
        }



        String strValue = "";
        String strDeviceId = "";

        strDeviceId = preferences.getString("settings_device_id", "");
        if (strDeviceId == "") {

            SharedPreferences.Editor edit = preferences.edit();
            if (edit != null) {
                edit.putString("settings_device_id", strDeviceId);
                edit.commit();
            }
        }
//        m_strDeviceId = strDeviceId;
//
//        m_strUserName = preferences.getString("settings_username", "");
//        m_strPassword = preferences.getString("settings_password", "");
//        m_mqtt_server_ip = preferences.getString("settings_server_ip", "");
//        m_sound_server_ip = preferences.getString("settings_sound_server_ip", "");
//        m_sound_server_port = preferences.getString("settings_sound_server_port", "");
//        m_sound_local_port = preferences.getString("settings_sound_local_port", "");
//        mRing = preferences.getBoolean("settings_alert_ring", true);
//        mVibrate = preferences.getBoolean("settings_alert_vibrate", false);


    }

    public String getDeviceID() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Calendar calendar = Calendar.getInstance();
        long timezone = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / (3600 * 1000);
        long time = calendar.getTimeInMillis() / 1000 + timezone * 3600;
        time -= 946684800;
        String strDeviceId = Long.toString(time);
        return strDeviceId;
    }


    public enum SettingKey {
        SETTINGS_USERNAME("settings_username"),
        SETTINGS_PASSWORD("settings_password"),
        SETTINGS_SERVER_IP("settings_server_ip"),
        SETTINGS_MQTT_PORT("settings_mqtt_server_port"),
        SETTINGS_UDP_PORT("settings_sound_server_port"),
        SETTINGS_LOCAL_PORT("settings_sound_local_port"),
        SETTINGS_ALERT_RING("settings_alert_ring"),
        SETTINGS_ALERT_VIBRATE("settings_alert_vibrate"),


        SETTINGS_DEVICE_ID("settings_device_id");

        public String value;
        SettingKey(String settings_value) {
            value = settings_value;
        }
    }


    public static boolean exist(SettingKey strKey) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(_this);
        return preferences.contains(strKey.value);
    }

    public static String get(SettingKey strKey) {
        return get(strKey, null);
    }

    public static String get(SettingKey strKey, String defauleValule) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(_this);
        String value = preferences.getString(strKey.value,defauleValule);
        if (value != null && defauleValule == null) {
            set(strKey, value);
        }
        return value;
    }

    public static void set(SettingKey strKey, String strVale) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(_this);
        SharedPreferences.Editor edit = preferences.edit();
        if (edit != null) {
            edit.putString(strKey.value, strVale);
            edit.commit();
        }
    }
    public static void set(SettingKey strKey, boolean strVale) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(_this);
        SharedPreferences.Editor edit = preferences.edit();
        if (edit != null) {
            edit.putBoolean(strKey.value, strVale);
            edit.commit();
        }
    }


}