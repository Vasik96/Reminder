package com.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            NotificationScheduler.scheduleNotificationsAfterBoot(context);
            ClosestReminderWidget.updateWidgetDetails(context);
        }
    }
}
