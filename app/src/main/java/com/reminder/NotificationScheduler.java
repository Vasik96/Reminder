package com.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.icu.util.Calendar;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.util.concurrent.TimeUnit;

public class NotificationScheduler {


    public static void scheduleNotification(int year, int month, int day, int hour, int minute, Context context, String reminderTitle) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!canScheduleExactAlarms(context)) {
                Toast.makeText(context, "Permission to schedule exact alarms is required.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        JSONArray jsonArray = loadRemindersFromFile(context);
        String reminderName = null;

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                ReminderItem reminderItem = ReminderItem.fromJSON(jsonObject);

                if (reminderItem.getYear() == year &&
                        reminderItem.getMonth() == month &&
                        reminderItem.getDay() == day &&
                        reminderItem.getHour() == hour &&
                        reminderItem.getMinute() == minute) {
                    reminderName = reminderItem.getName();
                    break;
                }
            } catch (JSONException e) {
                Log.e("NotificationScheduler", "Error parsing reminder from JSON", e);
            }
        }

        if (reminderName == null) {
            reminderName = "Unset Reminder";
        }

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("reminder_name", reminderName);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, reminderTitle.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                Log.d("PAST_TIME", "Scheduled time is in the past!");
                return;
            }

            AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), pendingIntent);
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
            
            scheduleMissedReminderWork(context);
        }
    }


    public static void scheduleMissedReminderWork(Context context) {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                MissedReminderWorker.class,
                15, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                        "missed_reminders",
                        ExistingPeriodicWorkPolicy.KEEP,
                        workRequest
                );
    }

    private static boolean canScheduleExactAlarms(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return context.getSystemService(AlarmManager.class).canScheduleExactAlarms();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return true;
        }
        return true;
    }




    public static JSONArray loadRemindersFromFile(Context context) {
        File file = new File(context.getExternalFilesDir(null), Constants.REMINDER_FILE);
        if (!file.exists()) {
            Log.d("LoadReminders", "File not found: " + file.getAbsolutePath());
            return new JSONArray();
        }

        StringBuilder stringBuilder = new StringBuilder();
        try (FileReader reader = new FileReader(file)) {
            int character;
            while ((character = reader.read()) != -1) {
                stringBuilder.append((char) character);
            }
            return new JSONArray(stringBuilder.toString());
        } catch (Exception e) {
            Log.e("LoadReminders", "Error reading file: ", e);
            return new JSONArray();
        }
    }

    public static void rescheduleNotification(int year, int month, int day, int hour, int minute, Context context, String reminderTitle) {
        cancelNotification(context, reminderTitle);


        scheduleNotification(year, month, day, hour, minute, context, reminderTitle);
    }

    public static void cancelNotification(Context context, String reminderTitle) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("reminder_name", reminderTitle);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, reminderTitle.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.d("NotificationScheduler", "Previous notification canceled: " + reminderTitle);
        }
    }


    public static void scheduleNotificationsAfterBoot(Context context) {
        Log.d("BOOT", "Scheduling notifications...");

        JSONArray jsonArray = loadRemindersFromFile(context);

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                ReminderItem reminderItem = ReminderItem.fromJSON(jsonObject);
                int hour = reminderItem.getHour();
                int minute = reminderItem.getMinute();
                int year = reminderItem.getYear();
                int month = reminderItem.getMonth();
                int day = reminderItem.getDay();

                String reminderTitle = reminderItem.getName();

                scheduleNotification(year, month, day, hour, minute, context, reminderTitle);
            } catch (JSONException e) {
                Log.e("NotificationScheduler", "Error parsing reminder from JSON", e);
            }
        }
    }

}