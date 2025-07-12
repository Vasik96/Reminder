package com.reminder;

import static com.reminder.Constants.REMINDER_TYPE;

import android.util.Log;

import androidx.fragment.app.FragmentManager;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.reminder.ui.home.HomeFragment;

import java.util.Calendar;

public class DatePickerDialog {

    private HomeFragment homeFragment;
    private TimePickerDialog timePickerDialog;

    public DatePickerDialog(HomeFragment homeFragment, TimePickerDialog timePickerDialog) {
        this.homeFragment = homeFragment;
        this.timePickerDialog = timePickerDialog;
    }

    public void openDatePicker(FragmentManager fragmentManager, String reminderName, ReminderItem.ReminderType type) {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker().build();
        datePicker.setCancelable(false);

        datePicker.show(fragmentManager, "datePicker");


        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(selection);

            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);


            timePickerDialog.openTimePicker(fragmentManager, reminderName, year, month, day, type);
        });
    }
}
