package com.reminder;

import android.content.Context;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import org.json.JSONArray;

public class MissedReminderWorker extends Worker {

    public MissedReminderWorker(Context context, WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        JSONArray jsonArray = FileHandling.loadRemindersFromFile(context);
        NotificationReceiver.deliverMissedReminders(context, jsonArray);
        return Result.success();
    }
}
