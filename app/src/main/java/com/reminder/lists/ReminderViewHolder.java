package com.reminder.lists;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.reminder.R;
import com.reminder.ReminderItem;

import java.util.Calendar;

public class ReminderViewHolder extends RecyclerView.ViewHolder {

    private final MaterialCardView cardView;
    private final TextView reminderTitle;
    private final TextView reminderTime;
    private final TextView reminderDate;
    private final Context context;

    public ReminderViewHolder(@NonNull View itemView) {
        super(itemView);
        context = itemView.getContext();
        cardView = itemView.findViewById(R.id.reminder_card);
        reminderTitle = itemView.findViewById(R.id.reminder_title);
        reminderTime = itemView.findViewById(R.id.reminder_time);
        reminderDate = itemView.findViewById(R.id.reminder_date);
    }

    public void bind(ReminderItem item) {
        String reminderName = item.getName();

        if (isTimePassed(item) || item.isDelivered()) {
            if (reminderName.length() > 12) {
                reminderName = reminderName.substring(0, 9) + "...";
            }
        }
        else if (reminderName.length() > 16) {
            reminderName = reminderName.substring(0, 13) + "...";
        }


        reminderTitle.setText(reminderName);

        reminderTime.setText(item.getFormattedTime());
        reminderDate.setText(item.getFormattedDate());

        Button doneButton = itemView.findViewById(R.id.done_indicator);
        if (item.isDelivered() || isTimePassed(item)) {
            doneButton.setVisibility(View.VISIBLE);
        } else {
            doneButton.setVisibility(View.GONE);
        }
    }

    private boolean isTimePassed(ReminderItem item) {
        Calendar currentTime = Calendar.getInstance();
        Calendar reminderTime = Calendar.getInstance();
        reminderTime.set(item.getYear(), item.getMonth(), item.getDay(), item.getHour(), item.getMinute(), 0);
        return currentTime.after(reminderTime);
    }
}
