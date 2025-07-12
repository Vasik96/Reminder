package com.reminder.lists;


import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.reminder.Categorization;
import com.reminder.ClosestReminderWidget;
import com.reminder.R;
import com.reminder.ReminderItem;
import com.reminder.ui.home.HomeFragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReminderAdapter extends ListAdapter<ReminderItem, ReminderViewHolder> {
    private final Context context;
    private HomeFragment homeFragment;
    private boolean isInitialLoad = true;
    private SparseBooleanArray animatedItems = new SparseBooleanArray();
    private static final long ANIMATION_DELAY = 100;
    private static final int MAX_ANIMATED_ITEMS = 5;
    private final List<ReminderItem> fullReminderList = new ArrayList<>();
    private Categorization.Category selectedCategory = Categorization.Category.CATEGORY_ALL;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final Handler sumbitList_handler = new Handler(Looper.getMainLooper());

    private static final DiffUtil.ItemCallback<ReminderItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<ReminderItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull ReminderItem oldItem, @NonNull ReminderItem newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ReminderItem oldItem, @NonNull ReminderItem newItem) {
            return oldItem.equals(newItem);
        }
    };


    public ReminderAdapter(Context context) {
        super(DIFF_CALLBACK);
        this.context = context;
    }


    public void setHomeFragment(HomeFragment homeFragment) {
        this.homeFragment = homeFragment;
    }
    public void setSelectedCategory(Categorization.Category category) {
        this.selectedCategory = category;
        filterByCategory();
    }
    public Categorization.Category getCurrentCategory() {
        return selectedCategory;
    }

    public void filterByCategory() {
        executor.submit(() -> {
            List<ReminderItem> filtered = new ArrayList<>();
            for (ReminderItem item : fullReminderList) {
                if (matchesCategory(item)) {
                    filtered.add(item);
                }
            }
            sortReminders(filtered);
        });
    }




    private boolean matchesCategory(ReminderItem item) {
        switch (selectedCategory) {
            case CATEGORY_ALL:
                return true;
            case CATEGORY_DAILY:
                return item.getReminderType() == ReminderItem.ReminderType.DAILY;
            case CATEGORY_WEEKLY:
                return item.getReminderType() == ReminderItem.ReminderType.WEEKLY;
            case CATEGORY_MONTHLY:
                return item.getReminderType() == ReminderItem.ReminderType.MONTHLY;
            case CATEGORY_QUARTERLY:
                return item.getReminderType() == ReminderItem.ReminderType.MONTHLY_3;
            case CATEGORY_YEARLY:
                return item.getReminderType() == ReminderItem.ReminderType.YEARLY;
            case CATEGORY_ONETIME:
                return item.getReminderType() == ReminderItem.ReminderType.ONE_TIME;
            default:
                return false;
        }
    }




    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.reminder_item, parent, false);
        return new ReminderViewHolder(view);
    }



    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        ReminderItem item = getItem(position);

        holder.bind(item);

        holder.itemView.setAlpha(0f);
        holder.itemView.animate()
                .alpha(1f)
                .setDuration(300)
                .setStartDelay(position * 50L)
                .start();

        holder.itemView.findViewById(R.id.reminder_card).setOnClickListener(v -> {
            showReminderDetailsDialog(item);
        });

        holder.itemView.findViewById(R.id.delete_button).setOnClickListener(v -> {
            String reminderId = item.getId();
            String reminderName = item.getName();

            Log.d("ReminderAdapter", "Delete button clicked for reminder ID: " + reminderId + ", Name: " + reminderName);

            if (homeFragment != null) {
                homeFragment.showDeleteConfirmationDialog(reminderId, reminderName);
            } else {
                Log.e("ReminderAdapter", "homeFragment is not initialized");
                Toast.makeText(context, R.string.common_error, Toast.LENGTH_SHORT).show();
            }
        });


        holder.itemView.findViewById(R.id.edit_button).setOnClickListener(v -> {
            String reminderId = item.getId();
            if (homeFragment != null) {
                homeFragment.editReminder(reminderId);
            } else {
                Log.e("ReminderAdapter", "homeFragment is not initialized");
                Toast.makeText(context, R.string.common_error, Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void showReminderDetailsDialog(ReminderItem item) {
        String reminderField = context.getString(R.string.reminder_field);
        String reminderDetailsDate = context.getString(R.string.reminder_details_date);
        String reminderDetailsTime = context.getString(R.string.reminder_details_time);
        String reminderDetailsDialog = context.getString(R.string.reminder_details_dialog);
        String reminderStatus = context.getString(R.string.reminder_status_dialog);
        String rStatusDone = context.getString(R.string.done_txt);
        String rStatusIncomplete = context.getString(R.string.not_done_txt);
        String reminderType = context.getString(R.string.reminder_type);
        String dailyType = context.getString(R.string.txt_daily);
        String weeklyType = context.getString(R.string.txt_weekly);
        String monthlyType = context.getString(R.string.txt_monthly);
        String yearlyType = context.getString(R.string.txt_yearly);
        String oneTimeType = context.getString(R.string.txt_onetime);
        String every3months = context.getString(R.string.txt_3months);

        String type;
        String status;

        if (item.isDelivered() || isTimePassed(item)) {
            status = rStatusDone;
        } else {
            status = rStatusIncomplete;
        }

        switch (item.getReminderType()) {
            case DAILY:
                type = dailyType;
                break;
            case WEEKLY:
                type = weeklyType;
                break;
            case MONTHLY:
                type = monthlyType;
                break;
            case YEARLY:
                type = yearlyType;
                break;
            case ONE_TIME:
                type = oneTimeType;
                break;
            case MONTHLY_3:
                type = every3months;
                break;
            default:
                type = oneTimeType;
                break;
        }

        SpannableStringBuilder messageBuilder = new SpannableStringBuilder();

        append_bold(messageBuilder, reminderField);
        messageBuilder.append(item.getName()).append("\n");

        append_bold(messageBuilder, reminderDetailsDate);
        messageBuilder.append(item.getFormattedDate()).append("\n");

        append_bold(messageBuilder, reminderDetailsTime);
        messageBuilder.append(item.getFormattedTime()).append("\n");

        append_bold(messageBuilder, reminderType);
        messageBuilder.append(type).append("\n");

        append_bold(messageBuilder, reminderStatus);

        SpannableString statusText = new SpannableString(status);

        if (status.equalsIgnoreCase(context.getString(R.string.not_done_txt))) {
            statusText.setSpan(new ForegroundColorSpan(Color.RED), 0, status.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if (status.equalsIgnoreCase(context.getString(R.string.done_txt))) {
            statusText.setSpan(new ForegroundColorSpan(Color.GREEN), 0, status.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        messageBuilder.append(statusText);

        new MaterialAlertDialogBuilder(context, com.google.android.material.R.style.ThemeOverlay_Material3Expressive_MaterialAlertDialog_Centered)
                .setIcon(R.drawable.ic_info)
                .setTitle(reminderDetailsDialog)
                .setMessage(messageBuilder)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void append_bold(SpannableStringBuilder builder, String label) {
        SpannableString boldLabel = new SpannableString(label + ": ");
        boldLabel.setSpan(new StyleSpan(Typeface.BOLD), 0, boldLabel.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append(boldLabel);
    }


    private boolean isTimePassed(ReminderItem item) {
        Calendar currentTime = Calendar.getInstance();
        Calendar reminderTime = Calendar.getInstance();
        reminderTime.set(item.getYear(), item.getMonth(), item.getDay(), item.getHour(), item.getMinute(), 0);


        return currentTime.after(reminderTime);
    }




    @Override
    public int getItemCount() {
        //Log.d("ReminderAdapter", "Item count: " + reminderList.size());
        return getCurrentList().size();
    }

    public void addItem(ReminderItem item) {
        List<ReminderItem> updated = new ArrayList<>(getCurrentList());
        updated.add(item);
        sumbitList_handler.post(() -> submitList(updated));
    }


    public void removeItem(String reminderId) {
        Log.d("removeItem", "Attempting to delete reminder with ID: " + reminderId);

        ReminderItem toRemove = null;

        for (ReminderItem item : fullReminderList) {
            if (item.getId().equals(reminderId)) {
                toRemove = item;
                break;
            }
        }

        if (toRemove != null) {
            Log.d("removeItem", "Reminder found, removing it: " + toRemove.getId());
            Log.d("removeItem", "Before deletion: " + fullReminderList.size());

            fullReminderList.remove(toRemove);
            List<ReminderItem> updatedList = new ArrayList<>(getCurrentList());
            updatedList.remove(toRemove);

            Log.d("removeItem", "After deletion: " + fullReminderList.size());

            sortReminders(updatedList);
        } else {
            Log.d("removeItem", "Reminder with ID " + reminderId + " not found.");
        }

        ClosestReminderWidget.updateWidgetDetails(context);
    }

    public void editItem(int position, ReminderItem newItem) {
        List<ReminderItem> updated = new ArrayList<>(getCurrentList());
        if (position >= 0 && position < updated.size()) {
            updated.set(position, newItem);
            sumbitList_handler.post(() -> submitList(updated));
        }
    }


    public void setReminders(List<ReminderItem> reminders) {
        executor.submit(() -> {
            fullReminderList.clear();
            fullReminderList.addAll(reminders);
            sortReminders(reminders);
        });
    }



    private void sortReminders(List<ReminderItem> listToSort) {
        executor.submit(() -> {
            listToSort.sort((item1, item2) -> {
                boolean isDone1 = item1.isDelivered() || isTimePassed(item1);
                boolean isDone2 = item2.isDelivered() || isTimePassed(item2);
                if (isDone1 != isDone2) return isDone1 ? 1 : -1;
                return item1.getName().compareToIgnoreCase(item2.getName());
            });
            sumbitList_handler.post(() -> submitList(listToSort));
        });
    }


}
