package com.reminder.ui.home;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.loadingindicator.LoadingIndicator;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.search.SearchView;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.reminder.Categorization;
import com.reminder.Constants;
import com.reminder.DatePickerDialog;
import com.reminder.FileHandling;
import com.reminder.ClosestReminderWidget;
import com.reminder.MainActivity;
import com.reminder.NotificationScheduler;
import com.reminder.R;
import com.reminder.ReminderBottomSheetDialog;
import com.reminder.ReminderItem;
import com.reminder.TimePickerDialog;
import com.reminder.databinding.FragmentHomeBinding;
import com.reminder.lists.ReminderAdapter;
import static com.google.android.material.search.SearchView.TransitionState;
import static com.reminder.Categorization.Category.CATEGORY_ALL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

public class HomeFragment extends Fragment implements ReminderBottomSheetDialog.ReminderCreationListener {

    private FragmentHomeBinding binding;
    private View root;
    private FloatingActionButton addReminderButton;
    private ReminderAdapter reminderAdapter;
    private Context context;
    private TimePickerDialog timePickerDialog;
    private DatePickerDialog datePickerDialog;
    private boolean missedNotificationsDelivered = false;
    private List<ReminderItem> reminderList;
    private RecyclerView searchResultsRecyclerView;
    private ReminderAdapter searchResultsAdapter;
    private RecyclerView recyclerViewMain;
    private LoadingIndicator loadingIndicator;
    private ChipGroup chipGroup;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        root = binding.getRoot();
        context = requireContext();

        addReminderButton = binding.addReminderButton;

        recyclerViewMain = binding.recyclerView;

        loadingIndicator = binding.loadingIndicator;
        chipGroup = binding.categoriesChipgroup;

        setupRecyclerView();
        setupDialogs();
        setupSearchView();
        checkAndAssignIdsToReminders(getContext());

        select_ALLcategory();

        addReminderButton.setOnClickListener(v -> openReminderDialog());

        chipGroup.setVisibility(View.GONE);
        loadingIndicator.setVisibility(View.VISIBLE);

        ChipGroup chipGroup = root.findViewById(R.id.categories_chipgroup);

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            for (int i = 0; i < chipGroup.getChildCount(); i++) {
                Chip chip = (Chip) chipGroup.getChildAt(i);
                if (checkedIds.contains(chip.getId())) {
                    chip.setChipIcon(ContextCompat.getDrawable(context, R.drawable.ic_check));
                } else {
                    chip.setChipIcon(null);
                }
            }

            int checkedId = checkedIds.get(0);
            Categorization.Category selectedCategory = getCategoryFromChipId(checkedId);

            if (selectedCategory != null) {
                reminderAdapter.setSelectedCategory(selectedCategory);
            }
            reloadRecyclerView(selectedCategory);
        });

        MaterialToolbar toolbar = binding.toolbar;
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_clean) {
                    new MaterialAlertDialogBuilder(context, R.style.Theme_Reminder_DeleteDialog)
                            .setIcon(R.drawable.ic_delete_sweep)
                            .setTitle(context.getString(R.string.clean_reminders))
                            .setMessage(context.getString(R.string.description_cleanup))
                            .setPositiveButton(context.getString(R.string.delete_button), (dialog, which) -> cleanReminders(reminderAdapter))
                            .setNegativeButton(context.getString(R.string.cancel_txt), (dialog, which) -> dialog.dismiss())
                            .show();

                    return true;
                }
                return false;
            }
        });

        return root;
    }

    private void cleanReminders(ReminderAdapter adapter) {
        boolean was_something_deleted = false;

        Categorization.Category current_category = adapter.getCurrentCategory();
        JSONArray reminders = FileHandling.loadRemindersFromFile(context);

        for (int i = 0; i < reminders.length(); i++) {
            try {
                JSONObject reminderJSON = reminders.getJSONObject(i);
                ReminderItem reminder_item = ReminderItem.fromJSON(reminderJSON);

                if (reminder_item.isDelivered() && reminder_item.getReminderType() == ReminderItem.ReminderType.ONE_TIME) {
                    Log.d("cleanReminders", "reminder is delivered/done: " + reminder_item.getName());
                    FileHandling.deleteEntryById(context, reminder_item.getId());
                    was_something_deleted = true;
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (was_something_deleted) {
            reloadRecyclerViewNow(current_category);
            Toast.makeText(context, context.getString(R.string.nothing_to_delete), Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(context, context.getString(R.string.nothing_to_delete), Toast.LENGTH_SHORT).show();
        }
    }

    private Categorization.Category getCategoryFromChipId(int chipId) {
        if (chipId == R.id.categorization_all) {
            return CATEGORY_ALL;
        } else if (chipId == R.id.categorization_daily) {
            return Categorization.Category.CATEGORY_DAILY;
        } else if (chipId == R.id.categorization_weekly) {
            return Categorization.Category.CATEGORY_WEEKLY;
        } else if (chipId == R.id.categorization_monthly) {
            return Categorization.Category.CATEGORY_MONTHLY;
        } else if (chipId == R.id.categorization_3months) {
            return Categorization.Category.CATEGORY_QUARTERLY;
        } else if (chipId == R.id.categorization_yearly) {
            return Categorization.Category.CATEGORY_YEARLY;
        } else if (chipId == R.id.categorization_onetime) {
            return Categorization.Category.CATEGORY_ONETIME;
        }
        return null;
    }


    @Override
    public void onReminderCreated(String name, ReminderItem.ReminderType type) {
        createDatePickerDialog(name, type);
    }



    private void setupRecyclerView() {
        recyclerViewMain.setLayoutManager(new LinearLayoutManager(context));
        reminderAdapter = new ReminderAdapter(context);
        reminderAdapter.setHomeFragment(this);
        recyclerViewMain.setAdapter(reminderAdapter);
        OverScrollDecoratorHelper.setUpOverScroll(recyclerViewMain, OverScrollDecoratorHelper.ORIENTATION_VERTICAL);
        loadReminders(CATEGORY_ALL);
    }

    private void reloadRecyclerView(Categorization.Category category) {
        recyclerViewMain.setLayoutManager(new LinearLayoutManager(context));
        reminderAdapter = new ReminderAdapter(context);
        reminderAdapter.setHomeFragment(this);
        recyclerViewMain.setAdapter(reminderAdapter);
        OverScrollDecoratorHelper.setUpOverScroll(recyclerViewMain, OverScrollDecoratorHelper.ORIENTATION_VERTICAL);

        loadReminders(category);
    }

    private void reloadRecyclerViewNow(Categorization.Category category) {
        recyclerViewMain.setLayoutManager(new LinearLayoutManager(context));
        reminderAdapter = new ReminderAdapter(context);
        reminderAdapter.setHomeFragment(this);
        recyclerViewMain.setAdapter(reminderAdapter);
        OverScrollDecoratorHelper.setUpOverScroll(recyclerViewMain, OverScrollDecoratorHelper.ORIENTATION_VERTICAL);

        loadRemindersNoDelay(category);
    }



    private void setupDialogs() {
        timePickerDialog = new TimePickerDialog(this);
        datePickerDialog = new DatePickerDialog(this, timePickerDialog);
    }

    public void editReminder(String reminderId) {
        ReminderItem reminderToEdit = null;
        for (ReminderItem reminder : reminderList) {
            if (reminder.getId().equals(reminderId)) {
                reminderToEdit = reminder;
                break;
            }
        }

        if (reminderToEdit != null) {
            Context context = requireContext();
            JSONArray jsonArray = FileHandling.loadRemindersFromFile(context);
            Log.d("EditReminder", "current id" + reminderId);

            Log.d("EditReminder", "Editing Reminder: Name = " + reminderToEdit.getName() +
                    ", Type = " + reminderToEdit.getReminderType().name());

            TimePickerDialog.isEditing = true;

            DatePickerDialog datePickerDialog = new DatePickerDialog(this, new TimePickerDialog(this));
            datePickerDialog.openDatePicker(requireActivity().getSupportFragmentManager(),
                    reminderToEdit.getName(), reminderToEdit.getReminderType());
        } else {
            Toast.makeText(context, R.string.edit_error, Toast.LENGTH_SHORT).show();
        }
    }




    public void checkAndAssignIdsToReminders(Context context) {
        JSONArray jsonArray = FileHandling.loadRemindersFromFile(context);

        boolean isUpdated = false;

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                String id = jsonObject.optString("id", "");
                if (id.isEmpty()) {
                    id = FileHandling.generateUniqueId();
                    jsonObject.put("id", id);
                    isUpdated = true;
                }
            } catch (JSONException e) {
                Log.e("HomeFragment", "Error checking and assigning IDs to reminders", e);
            }
        }


        if (isUpdated) {
            FileHandling.overwriteRemindersToFile(context, jsonArray);
        }
    }




    private void openReminderDialog() {
        ReminderBottomSheetDialog bottomSheetDialog = new ReminderBottomSheetDialog();
        List<String> reminderNames = new ArrayList<>();
        for (ReminderItem item : reminderList) {
            reminderNames.add(item.getName().toLowerCase());
        }
        bottomSheetDialog.setExistingReminderNames(reminderNames);
        bottomSheetDialog.setListener(this);
        bottomSheetDialog.show(getParentFragmentManager(), "ReminderBottomSheetDialog");
    }



    public void createDatePickerDialog(String name, ReminderItem.ReminderType type) {
        datePickerDialog.openDatePicker(getParentFragmentManager(), name, type);
    }


    public boolean isNameDuplicate(String name) {
        for (ReminderItem item : reminderList) {
            if (item.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }

        JSONArray jsonArray = FileHandling.loadRemindersFromFile(context);
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                ReminderItem existingItem = ReminderItem.fromJSON(jsonObject);
                if (existingItem.getName().equalsIgnoreCase(name)) {
                    return true;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return false;
    }


    public void saveReminderDetails(String name, int hour, int minute, int year, int month, int day, ReminderItem.ReminderType type) {
        Context context = requireContext();
        JSONArray jsonArray = FileHandling.loadRemindersFromFile(context);
        JSONArray updatedJsonArray = new JSONArray();
        boolean reminderExists = false;
        String reminderId = null;

        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                ReminderItem existingItem = ReminderItem.fromJSON(jsonObject);


                if (existingItem.getName().equals(name)) {
                    reminderExists = true;
                    reminderId = existingItem.getId();
                } else {
                    updatedJsonArray.put(jsonObject);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }


        if (!reminderExists) {
            reminderId = FileHandling.generateUniqueId();
        }


        ReminderItem reminderItem = new ReminderItem(name, reminderId, hour, minute, year, month, day, false, type);


        try {
            updatedJsonArray.put(reminderItem.toJSON());
        } catch (JSONException e) {
            e.printStackTrace();
        }


        FileHandling.overwriteRemindersToFile(context, updatedJsonArray);


        int position = -1;
        for (int i = 0; i < reminderList.size(); i++) {
            if (reminderList.get(i).getName().equals(name)) {
                position = i;
                break;
            }
        }


        if (position != -1) {
            reminderAdapter.editItem(position, reminderItem);
        } else {
            reminderAdapter.addItem(reminderItem);
        }


        if (reminderExists) {
            NotificationScheduler.rescheduleNotification(year, month, day, hour, minute, context, name);
        } else {
            NotificationScheduler.scheduleNotification(year, month, day, hour, minute, context, name);
        }
        reloadRecyclerViewNow(CATEGORY_ALL);

        // update widget
        ClosestReminderWidget.updateWidgetDetails(context);
    }


    private void setupSearchView() {
        SearchView searchView = binding.searchView;

        searchResultsRecyclerView = searchView.findViewById(R.id.search_results_recycler_view);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(context));

        searchResultsAdapter = new ReminderAdapter(context);
        searchResultsRecyclerView.setAdapter(searchResultsAdapter);

        EditText searchEditText = searchView.getEditText();


        OverScrollDecoratorHelper.setUpOverScroll(searchResultsRecyclerView, OverScrollDecoratorHelper.ORIENTATION_VERTICAL);

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSearchResults(s.toString());
                Log.d("Search", "Text changed: " + s);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        searchView.addTransitionListener((searchView1, previousState, newState) -> {
            if (newState == TransitionState.HIDDEN || newState == TransitionState.HIDING) {
                searchResultsAdapter.setReminders(new ArrayList<>());
                addReminderButton.show();
            }
            if (newState == TransitionState.SHOWING || newState == TransitionState.SHOWN) {
                addReminderButton.hide();
            }
        });
    }


    private void filterSearchResults(String query) {
        if (query.isEmpty()) {
            searchResultsAdapter.setReminders(new ArrayList<>());
            Log.d("Search", "query empty");
            return;
        }

        List<ReminderItem> filteredList = new ArrayList<>();
        for (ReminderItem item : reminderList) {
            if (item.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(item);
            }
        }

        Log.d("Search", "Filtered reminders: " + filteredList.size());
        searchResultsAdapter.setReminders(filteredList);
    }


    private void loadReminders(Categorization.Category category) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        mainHandler.postDelayed(() -> {
            executor.execute(() -> {
                JSONArray jsonArray = FileHandling.loadRemindersFromFile(context);
                List<ReminderItem> tempReminderList = new ArrayList<>();

                if (jsonArray.length() == 0) {
                    Log.d("LoadReminders", "No reminders found in file.");
                }

                try {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        ReminderItem reminder = ReminderItem.fromJSON(jsonObject);

                        if (category == CATEGORY_ALL || reminder.getCategory() == category) {
                            tempReminderList.add(reminder);
                        }
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> {
                        Toast.makeText(context, "Error loading reminders", Toast.LENGTH_SHORT).show();
                    });
                    Log.e("LoadReminders", "JSON Parsing Error: ", e);
                }

                mainHandler.post(() -> {
                    reminderList = tempReminderList;
                    reminderAdapter.setReminders(reminderList);

                    if (!missedNotificationsDelivered) {
                        missedNotificationsDelivered = true;
                    }

                    loadingIndicator.setVisibility(View.GONE);
                    chipGroup.setVisibility(View.VISIBLE);
                });

                executor.shutdown();
            });
        }, 200);
    }

    private void loadRemindersNoDelay(Categorization.Category category) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            JSONArray jsonArray = FileHandling.loadRemindersFromFile(context);
            List<ReminderItem> tempReminderList = new ArrayList<>();

            if (jsonArray.length() == 0) {
                Log.d("LoadReminders", "No reminders found in file.");
            }

            try {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    ReminderItem reminder = ReminderItem.fromJSON(jsonObject);

                    if (category == CATEGORY_ALL || reminder.getCategory() == category) {
                        tempReminderList.add(reminder);
                    }
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    Toast.makeText(context, "Error loading reminders", Toast.LENGTH_SHORT).show();
                });
                Log.e("LoadReminders", "JSON Parsing Error: ", e);
            }

            mainHandler.post(() -> {
                reminderList = tempReminderList;
                reminderAdapter.setReminders(reminderList);

                if (!missedNotificationsDelivered) {
                    missedNotificationsDelivered = true;
                }

                loadingIndicator.setVisibility(View.GONE);
            });

            executor.shutdown();
        });
    }


    public void showDeleteConfirmationDialog(String reminderId, String reminderName) {
        Log.d("HomeFragment", "Showing delete confirmation dialog for reminder ID: " + reminderId + ", Name: " + reminderName);

        new MaterialAlertDialogBuilder(context, R.style.Theme_Reminder_DeleteDialog)
                .setTitle(R.string.delete_reminder_dialog_title)
                .setIcon(R.drawable.ic_delete)
                .setMessage(R.string.question_txt_delete)
                .setPositiveButton(R.string.delete_button, (dialog, which) -> {
                    Log.d("HomeFragment", "Confirmed deletion for reminder id: " + reminderId);

                    reminderAdapter.removeItem(reminderId);
                    NotificationScheduler.cancelNotification(context, reminderName);

                    FileHandling.deleteEntryById(context, reminderId);
                    Toast.makeText(context, "Reminder deleted", Toast.LENGTH_SHORT).show();
                    loadRemindersNoDelay(CATEGORY_ALL);
                    select_ALLcategory();
                })
                .setNegativeButton(R.string.cancel_txt, null)
                .show();
    }



    private void select_ALLcategory() {
        Chip allChip = root.findViewById(R.id.categorization_all);
        allChip.setChipIcon(ContextCompat.getDrawable(context, R.drawable.ic_check));
        allChip.setChecked(true);

        reminderAdapter.setSelectedCategory(Categorization.Category.CATEGORY_ALL);
        reloadRecyclerView(Categorization.Category.CATEGORY_ALL);
    }




    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
