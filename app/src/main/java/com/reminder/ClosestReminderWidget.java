package com.reminder;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.icu.util.Calendar;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.TypedValue;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Implementation of App Widget functionality.
 */
public class ClosestReminderWidget extends AppWidgetProvider {

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        JSONArray reminders = FileHandling.loadRemindersFromFile(context);
        ReminderItem closestReminder = getClosestReminder(reminders);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.closest_reminder_widget);

        views.setTextViewText(R.id.appwidget_label, context.getString(R.string.closest_reminder));

        if (closestReminder != null) {
            views.setTextViewText(R.id.appwidget_reminder_name, closestReminder.getName());
            views.setTextViewText(R.id.appwidget_reminder_date, closestReminder.getFormattedDate());
            views.setTextViewText(R.id.appwidget_reminder_time, closestReminder.getFormattedTime());

            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra("WIDGET_LAUNCH", true);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            views.setOnClickPendingIntent(android.R.id.background, pendingIntent);
            views.setOnClickPendingIntent(R.id.appwidget_reminder_name, pendingIntent);
            views.setOnClickPendingIntent(R.id.appwidget_reminder_date, pendingIntent);
            views.setOnClickPendingIntent(R.id.appwidget_reminder_time, pendingIntent);
        } else {
            views.setTextViewText(R.id.appwidget_reminder_name, "None");
            views.setTextViewText(R.id.appwidget_reminder_date, "");
            views.setTextViewText(R.id.appwidget_reminder_time, "");
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }







    //returns the closest reminder from JSON
    private static ReminderItem getClosestReminder(JSONArray reminders) {
        ReminderItem closestReminder = null;
        long closestTimeDifference = Long.MAX_VALUE;

        Calendar now = Calendar.getInstance();

        for (int i = 0; i < reminders.length(); i++) {
            try {
                JSONObject reminderJSON = reminders.getJSONObject(i);
                ReminderItem reminder = ReminderItem.fromJSON(reminderJSON);


                if (reminder.isDelivered()) {
                    continue;
                }

                Calendar reminderTime = Calendar.getInstance();
                reminderTime.set(reminder.getYear(), reminder.getMonth(), reminder.getDay(),
                        reminder.getHour(), reminder.getMinute(), 0);

                long timeDifference = reminderTime.getTimeInMillis() - now.getTimeInMillis();

                if (timeDifference > 0 && timeDifference < closestTimeDifference) {
                    closestTimeDifference = timeDifference;
                    closestReminder = reminder;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return closestReminder;
    }


    //method to update the widget
    public static void updateWidgetDetails(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        ComponentName componentName = new ComponentName(context, ClosestReminderWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);

        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}