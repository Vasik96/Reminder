package com.reminder;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class ReminderApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Context context = getApplicationContext();


        Log.d("reminder_application", "checking and updating file");

        NotificationScheduler.scheduleNotificationsAfterBoot(this);
        ClosestReminderWidget.updateWidgetDetails(context);


        FileHandling.checkFileAndUpdate(context);
    }
}