package com.reminder;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.icu.util.Calendar;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String reminderName = intent.getStringExtra("reminder_name");
        if (reminderName == null) {
            reminderName = "Unset Reminder";
        }

        ReminderItem reminderItem = getReminderByName(context, reminderName);

        if (reminderItem != null) {
            sendReminderNotification(context, reminderItem);


            rescheduleReminder(context, reminderItem);
        }
    }

    private ReminderItem getReminderByName(Context context, String reminderName) {
        JSONArray jsonArray = FileHandling.loadRemindersFromFile(context);

        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                ReminderItem reminderItem = ReminderItem.fromJSON(jsonObject);

                if (reminderItem.getName().equals(reminderName)) {
                    return reminderItem;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    void rescheduleReminder(Context context, ReminderItem reminderItem) {
        ReminderItem.ReminderType type = reminderItem.getReminderType();
        Log.e("rescheduleReminder", "method called, reminder name: " + reminderItem.getName());

        if (type == ReminderItem.ReminderType.ONE_TIME) {
            Log.e("rescheduleReminder", "reminder is one_time setting as delivered, exiting method...");
            setAsDelivered(context, reminderItem);
            return;
        }

        Log.e("rescheduleReminder", "reminder is not one_time, proceeding");

        Calendar nextReminderTime = Calendar.getInstance();
        nextReminderTime.set(reminderItem.getYear(), reminderItem.getMonth(), reminderItem.getDay(),
                reminderItem.getHour(), reminderItem.getMinute(), 0);
        nextReminderTime.set(Calendar.MILLISECOND, 0);

        long currentTime = System.currentTimeMillis();
        while (nextReminderTime.getTimeInMillis() <= currentTime) {
            switch (type) {
                case DAILY:
                    nextReminderTime.add(Calendar.DAY_OF_MONTH, 1);
                    break;
                case WEEKLY:
                    nextReminderTime.add(Calendar.WEEK_OF_YEAR, 1);
                    break;
                case MONTHLY:
                    nextReminderTime.add(Calendar.MONTH, 1);
                    break;
                case YEARLY:
                    nextReminderTime.add(Calendar.YEAR, 1);
                    break;
                case MONTHLY_3:
                    nextReminderTime.add(Calendar.MONTH, 3);
                    break;
            }
        }

        reminderItem.setYear(nextReminderTime.get(Calendar.YEAR));
        reminderItem.setMonth(nextReminderTime.get(Calendar.MONTH));
        reminderItem.setDay(nextReminderTime.get(Calendar.DAY_OF_MONTH));
        reminderItem.setHour(nextReminderTime.get(Calendar.HOUR_OF_DAY));
        reminderItem.setMinute(nextReminderTime.get(Calendar.MINUTE));
        reminderItem.setDelivered(false);

        synchronized (FileHandling.class) {
            JSONArray updatedJsonArray = FileHandling.loadRemindersFromFile(context);
            for (int i = 0; i < updatedJsonArray.length(); i++) {
                try {
                    JSONObject jsonObject = updatedJsonArray.getJSONObject(i);
                    ReminderItem existingItem = ReminderItem.fromJSON(jsonObject);

                    if (existingItem.getName().equals(reminderItem.getName())) {
                        updatedJsonArray.put(i, reminderItem.toJSON());
                        break;
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error updating reminder in rescheduleReminder", e);
                }
            }

            FileHandling.overwriteRemindersToFile(context, updatedJsonArray);
        }

        Log.e("rescheduleReminder", "after synchronized block, reminder details: reminder date " + reminderItem.getFormattedDate());

        NotificationScheduler.scheduleNotification(
                reminderItem.getYear(), reminderItem.getMonth(), reminderItem.getDay(),
                reminderItem.getHour(), reminderItem.getMinute(), context,
                reminderItem.getName()
        );

        Log.e("rescheduleReminder", "rescheduling notification");

        ClosestReminderWidget.updateWidgetDetails(context);
    }





    private static final String TAG = "NotificationReceiver";
    public void saveReminderDetails(Context context, String name, int hour, int minute,
                                    int year, int month, int day, ReminderItem.ReminderType type) {
        JSONArray jsonArray = FileHandling.loadRemindersFromFile(context);

        JSONArray updatedJsonArray = new JSONArray();
        boolean isUpdated = false;

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                ReminderItem existingItem = ReminderItem.fromJSON(jsonObject);

                if (existingItem.getName().equals(name)) {
                    existingItem.setHour(hour);
                    existingItem.setMinute(minute);
                    existingItem.setYear(year);
                    existingItem.setMonth(month);
                    existingItem.setDay(day);
                    existingItem.setDelivered(false);
                    updatedJsonArray.put(existingItem.toJSON());
                    isUpdated = true;
                } else {
                    updatedJsonArray.put(jsonObject);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error updating reminder in saveReminderDetails", e);
            }
        }

        if (!isUpdated) {
            String id = FileHandling.generateUniqueId();
            ReminderItem newReminder = new ReminderItem(name, id, hour, minute, year, month, day, false, type);

            try {
                updatedJsonArray.put(newReminder.toJSON());
            } catch (JSONException e) {
                Log.e(TAG, "Error adding new reminder in saveReminderDetails", e);
            }
        }


        FileHandling.overwriteRemindersToFile(context, updatedJsonArray);
    }


    public static void setAsDelivered(Context context, ReminderItem reminderItem) {
        synchronized (FileHandling.class) {
            JSONArray jsonArray = FileHandling.loadRemindersFromFile(context);

            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    ReminderItem currentReminder = ReminderItem.fromJSON(jsonObject);

                    if (currentReminder.getName().equals(reminderItem.getName())) {
                        currentReminder.setDelivered(true);
                        jsonArray.put(i, currentReminder.toJSON());
                        break;
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error updating reminder as delivered", e);
                }
            }

            FileHandling.overwriteRemindersToFile(context, jsonArray);
        }
    }

    public static void deliverMissedReminders(Context context, JSONArray jsonArray) {
        long currentTime = System.currentTimeMillis();

        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                ReminderItem reminderItem = ReminderItem.fromJSON(jsonObject);

                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.YEAR, reminderItem.getYear());
                calendar.set(Calendar.MONTH, reminderItem.getMonth());
                calendar.set(Calendar.DAY_OF_MONTH, reminderItem.getDay());
                calendar.set(Calendar.HOUR_OF_DAY, reminderItem.getHour());
                calendar.set(Calendar.MINUTE, reminderItem.getMinute());
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);

                long reminderTime = calendar.getTimeInMillis();

                if (currentTime - reminderTime >= 5 * 60 * 1000) {
                    if (!reminderItem.isDelivered()) {
                        sendReminderNotification(context, reminderItem);
                        reminderItem.setDelivered(true);
                        jsonArray.put(i, reminderItem.toJSON());


                        if (reminderItem.getReminderType() != ReminderItem.ReminderType.ONE_TIME) {
                            new NotificationReceiver().rescheduleReminder(context, reminderItem);
                        }
                    }
                }
            }

            FileHandling.overwriteRemindersToFile(context, jsonArray);
        } catch (JSONException e) {
            Log.e(TAG, "Error processing missed reminders", e);
        }
    }


    static void sendReminderNotification(Context context, ReminderItem reminderItem) {
        String reminderName = reminderItem.getName();

        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.app_icon)
                .setContentTitle("Reminder")
                .setContentText(reminderName)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(reminderItem.getName().hashCode(), builder.build());

        ClosestReminderWidget.updateWidgetDetails(context);
    }
}