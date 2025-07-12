package com.reminder;

import static com.reminder.Constants.REMINDER_TYPE;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.reminder.ui.home.HomeFragment;

import java.util.Objects;

public class TimePickerDialog {

    private HomeFragment homeFragment;
    public static boolean isEditing = false;
    String reminder_id = null;

    public TimePickerDialog(HomeFragment homeFragment) {
        this.homeFragment = homeFragment;
    }

    public void openTimePicker(FragmentManager fragmentManager, String reminderName, int year, int month, int day, ReminderItem.ReminderType type) {
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .build();


        timePicker.show(fragmentManager, "timePicker");

        timePicker.addOnPositiveButtonClickListener(v -> {
            int hour = timePicker.getHour();
            int minute = timePicker.getMinute();


            if (REMINDER_TYPE == null) {
                Log.d("TYPE", "type is null, setting temporary");
                REMINDER_TYPE = ReminderItem.ReminderType.ONE_TIME;
            }


            homeFragment.saveReminderDetails(reminderName, hour, minute, year, month, day, type);


            LinearLayout snackbarLayout = (LinearLayout) LayoutInflater.from(homeFragment.getContext())
                    .inflate(R.layout.snackbar_layout, null);

            TextView snackbarMessage = snackbarLayout.findViewById(R.id.snackbar_message);
            ImageView snackbarClose = snackbarLayout.findViewById(R.id.snackbar_close);


            if (isEditing) {
                snackbarMessage.setText(R.string.snackbar_reminder_edited);
            } else {
                snackbarMessage.setText(R.string.snackbar_reminder_scheduled);
            }


            Snackbar snackbar = Snackbar.make(homeFragment.requireView(), null, Snackbar.LENGTH_SHORT);


            View snackbarView = snackbar.getView();


            TextView defaultTextView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
            if (defaultTextView != null) {
                defaultTextView.setVisibility(View.INVISIBLE);
            }


            if (snackbarView instanceof ViewGroup) {
                ViewGroup snackbarRoot = (ViewGroup) snackbarView;
                snackbarRoot.removeAllViews();
                snackbarRoot.addView(snackbarLayout);
            }


            snackbar.show();

            isEditing = false;


            snackbarClose.setOnClickListener(v1 -> snackbar.dismiss());
        });
        


        timePicker.addOnNegativeButtonClickListener(v -> timePicker.dismiss());

        REMINDER_TYPE = null; //reset the "REMINDER_TYPE"
    }
}
