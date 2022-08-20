package com.cpos.cposmonitor.data;

import com.cpos.cposmonitor.MQTTService;
import com.cpos.cposmonitor.MainActivity;
import com.cpos.cposmonitor.MyApplication;
import com.cpos.cposmonitor.MyServiceConnection;
import com.cpos.cposmonitor.data.model.LoggedInUser;

import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
public class LoginDataSource {

    public Result<LoggedInUser> login(String username, String password, String device_id) {

        MyApplication.set(MyApplication.SettingKey.SETTINGS_USERNAME, username);
        MyApplication.set(MyApplication.SettingKey.SETTINGS_PASSWORD, password);
        try {
            Thread t1 = new Thread(new Runnable() {
                public void run() {
                    MQTTService.login();
                }
            });
            t1.start();
            try {
                t1.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // TODO: handle loggedInUser authentication
            LoggedInUser fakeUser =
                    new LoggedInUser(
                            java.util.UUID.randomUUID().toString(),
                            username);
            return new Result.Success<>(fakeUser);

        } catch (Exception e) {
            return new Result.Error(new IOException("Error logging in", e));
        }
    }

    public void logout() {
        // TODO: revoke authentication
    }
}