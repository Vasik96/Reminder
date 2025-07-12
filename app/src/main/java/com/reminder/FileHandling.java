package com.reminder;

import static com.reminder.NotificationReceiver.sendReminderNotification;

import android.content.Context;
import android.icu.util.Calendar;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

public class FileHandling {

    private static final String TAG = "FileHandling";

    public static JSONArray loadRemindersFromFile(Context context) {
        File file = new File(context.getExternalFilesDir(null), Constants.REMINDER_FILE);
        if (!file.exists()) {
            Log.d(TAG, "File not found: " + file.getAbsolutePath());
            return new JSONArray();
        }

        StringBuilder stringBuilder = new StringBuilder();
        try (FileReader reader = new FileReader(file)) {
            int character;
            while ((character = reader.read()) != -1) {
                stringBuilder.append((char) character);
            }

            String jsonContent = stringBuilder.toString();

            try {
                JSONArray jsonArray = new JSONArray(jsonContent);
                Log.d(TAG, "File content: " + jsonArray.toString(4));
                return jsonArray;
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing JSON", e);
            }

            return new JSONArray(jsonContent);
        } catch (Exception e) {
            Log.e(TAG, "Error reading file: ", e);
            return new JSONArray();
        }
    }



    //unused
    public static void saveReminderToFile(Context context, ReminderItem reminderItem) {
        File file = new File(context.getExternalFilesDir(null), Constants.REMINDER_FILE);
        JSONArray jsonArray = loadRemindersFromFile(context);

        try {
            jsonArray.put(reminderItem.toJSON());

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(jsonArray.toString());
            }
            Log.d(TAG, "Reminder saved successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Error saving reminder to file: ", e);
        }
    }

    public static void overwriteRemindersToFile(Context context, JSONArray jsonArray) {
        //Log.w(TAG, "CALL STACK:\n" + Log.getStackTraceString(new Exception()));


        File file = new File(context.getExternalFilesDir(null), Constants.REMINDER_FILE);
        File tempFile = new File(file.getAbsolutePath() + ".tmp");


        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(jsonArray.toString());
            writer.flush();


            if (!tempFile.renameTo(file)) {
                Log.e(TAG, "Failed to rename temp file to " + file.getAbsolutePath());
            } else {
                Log.d(TAG, "Reminders overwritten successfully.");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error overwriting reminders to file: ", e);
        } finally {
            if (tempFile.exists() && !tempFile.delete()) {
                Log.e(TAG, "Failed to delete temp file: " + tempFile.getAbsolutePath());
            }
        }
    }


    public static void printPrettyJSON(JSONArray jsonArray) {
        try {
            Log.d(TAG, "JSON Content:\n" + jsonArray.toString(4));
        } catch (Exception e) {
            Log.e(TAG, "Error formatting JSON: ", e);
        }
    }

    public static void checkFileAndUpdate(Context context) {
        JSONArray jsonArray = loadRemindersFromFile(context);
        long currentTime = System.currentTimeMillis();

        try {
            Log.d(TAG, "Loaded reminders: " + jsonArray.toString());

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

                if (reminderItem.getReminderType() != ReminderItem.ReminderType.ONE_TIME) {
                    reminderItem.setDelivered(false);
                }

                if (currentTime - reminderTime >= 5 * 60 * 1000 && !reminderItem.isDelivered()) {
                    Log.e("ReminderUpdate", "A reminder has not been delivered, and is in the past, info -> \n item: " + reminderItem.getName() + "\n date: " + reminderItem.getFormattedDate() + "\n type: " + reminderItem.getReminderType());

                    sendReminderNotification(context, reminderItem);

                    reminderItem.setDelivered(true);
                    jsonArray.put(i, reminderItem.toJSON());

                    new NotificationReceiver().rescheduleReminder(context, reminderItem);
                }
            }

        } catch (JSONException e) {
            Log.e(TAG, "Error processing reminders during checkFileAndUpdate", e);
        }
    }




    public static void deleteEntryById(Context context, String reminderId) {
        File file = new File(context.getExternalFilesDir(null), Constants.REMINDER_FILE);
        JSONArray jsonArray = loadRemindersFromFile(context);

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String id = jsonObject.optString("id");

                if (id.equals(reminderId)) {
                    jsonArray.remove(i);
                    Log.d(TAG, "Reminder with ID: " + reminderId + " has been deleted.");
                    break;
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error processing reminder during deletion", e);
            }
        }

        overwriteRemindersToFile(context, jsonArray);
    }





    /**
     * generate random id for reminders
     * @return unique random id (string)
     */
    public static String generateUniqueId() {
        return UUID.randomUUID().toString();
    }



}
