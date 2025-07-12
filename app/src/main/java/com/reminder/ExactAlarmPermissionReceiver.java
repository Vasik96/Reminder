package com.reminder;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

public class ExactAlarmPermissionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean hasPermission = canScheduleExactAlarms(context);

        if (hasPermission) {
            Log.d("ExactAlarmReceiver", "Permission granted. Rescheduling alarms...");
            NotificationScheduler.scheduleNotificationsAfterBoot(context);
        } else {
            Log.d("ExactAlarmReceiver", "Permission revoked.");
            Toast.makeText(context, "Permission Denied! App will not work", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean canScheduleExactAlarms(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return context.getSystemService(AlarmManager.class).canScheduleExactAlarms();
        } else {
            return true;
        }
    }
}
