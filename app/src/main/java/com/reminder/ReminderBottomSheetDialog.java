package com.reminder;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.reminder.ui.home.HomeFragment;

import java.util.List;
import java.util.Objects;

public class ReminderBottomSheetDialog extends BottomSheetDialogFragment {

    public interface ReminderCreationListener {
        void onReminderCreated(String name, ReminderItem.ReminderType type);
    }

    private List<String> existingReminderNames;
    private ReminderCreationListener listener;

    public void setExistingReminderNames(List<String> names) {
        this.existingReminderNames = names;
    }

    public void setListener(ReminderCreationListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.name_dialog, container, false);

        TextInputLayout reminderNameLayout = view.findViewById(R.id.reminder_name_field);
        TextInputEditText reminderNameInput = view.findViewById(R.id.reminder_name_input);
        RadioGroup radioGroup = view.findViewById(R.id.radio_group);

        RadioButton justOnceRadioButton = view.findViewById(R.id.onetime_radiobtn);
        justOnceRadioButton.setChecked(true);

        view.findViewById(R.id.btn_positive).setOnClickListener(v -> {
            String reminderName = Objects.requireNonNull(reminderNameInput.getText()).toString().trim();

            if (reminderName.isEmpty()) {
                reminderNameLayout.setError(getString(R.string.required_field_error));
                return;
            }

            if (existingReminderNames != null && existingReminderNames.contains(reminderName.toLowerCase())) {
                reminderNameLayout.setError(getString(R.string.already_exists));
                return;
            }

            ReminderItem.ReminderType type;
            int checkedId = radioGroup.getCheckedRadioButtonId();
            if (checkedId == R.id.onetime_radiobtn) {
                type = ReminderItem.ReminderType.ONE_TIME;
            } else if (checkedId == R.id.daily_radiobtn) {
                type = ReminderItem.ReminderType.DAILY;
            } else if (checkedId == R.id.weekly_radiobtn) {
                type = ReminderItem.ReminderType.WEEKLY;
            } else if (checkedId == R.id.monthly_radiobtn) {
                type = ReminderItem.ReminderType.MONTHLY;
            } else if (checkedId == R.id.yearly_radiobtn) {
                type = ReminderItem.ReminderType.YEARLY;
            } else if (checkedId == R.id.months3_radiobtn) {
                type = ReminderItem.ReminderType.MONTHLY_3; // every 3 months
            }
            else {
                type = ReminderItem.ReminderType.ONE_TIME;
            }

            if (listener != null) {
                listener.onReminderCreated(reminderName, type);
            }

            dismiss();
        });

        view.findViewById(R.id.btn_negative).setOnClickListener(v -> dismiss());

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if (dialog != null) {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);

                
                bottomSheet.setOnTouchListener(new View.OnTouchListener() {
                    private float startY;
                    private static final int SWIPE_THRESHOLD = 450;

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                startY = event.getY();
                                return true;
                            case MotionEvent.ACTION_MOVE:
                                float endY = event.getY();
                                float deltaY = endY - startY;
                                if (deltaY > SWIPE_THRESHOLD) {
                                    dismiss();
                                    return true;
                                }
                                return false;
                            default:
                                return false;
                        }
                    }
                });
            }


            View dragHandle = dialog.findViewById(R.id.drag_handle);
            if (dragHandle != null) {
                dragHandle.setOnTouchListener((v, event) -> true);
            }
        }
    }

}
