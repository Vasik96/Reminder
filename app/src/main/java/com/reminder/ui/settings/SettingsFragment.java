package com.reminder.ui.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.reminder.AppThemeDialog;
import com.reminder.ClosestReminderWidget;
import com.reminder.FileHandling;
import com.reminder.NotificationScheduler;
import com.reminder.R;
import com.reminder.databinding.FragmentSettingsBinding;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private Context context;
    AppCompatButton languageButton;
    AppCompatButton appThemeButton;
    AppCompatButton printJsonButton;

    AppCompatButton backupButton;
    AppCompatButton restoreButton;

    private ActivityResultLauncher<Intent> exportJsonLauncher;
    private ActivityResultLauncher<Intent> importJsonLauncher;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        context = getContext();

        languageButton = binding.languageSetting;
        appThemeButton = binding.appTheme;
        printJsonButton = binding.printJson;
        backupButton = binding.exportFile;
        restoreButton = binding.importFile;

        MaterialToolbar toolbar = binding.toolbar;
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());





        setupButtonListeners();

        return root;
    }

    private void setupButtonListeners() {
        languageButton.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                startLanguageIntent();
            } else {
                Toast.makeText(context, "Unsupported Android version", Toast.LENGTH_SHORT).show();
            }
        });

        appThemeButton.setOnClickListener(v -> {
            AppThemeDialog appThemeDialog = new AppThemeDialog(requireContext(), () -> {
                requireActivity().runOnUiThread(() -> {
                    requireActivity().recreate();
                });
            });
            appThemeDialog.showThemeDialog();
        });

        printJsonButton.setOnClickListener(v -> {
            JSONArray reminders = FileHandling.loadRemindersFromFile(context);
            FileHandling.printPrettyJSON(reminders);
        });

        exportJsonLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        exportJsonToUri(uri);
                    }
                });

        importJsonLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        importJsonFromUri(uri);
                    }
                });


        backupButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, "reminders.json");
            exportJsonLauncher.launch(intent);
        });

        restoreButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            importJsonLauncher.launch(intent);
        });



    }

    private void exportJsonToUri(Uri uri) {
        try (OutputStream os = requireContext().getContentResolver().openOutputStream(uri)) {
            if (os != null) {
                JSONArray jsonArray = FileHandling.loadRemindersFromFile(requireContext());
                String json = jsonArray.toString();
                os.write(json.getBytes(StandardCharsets.UTF_8));
                os.flush();
                Toast.makeText(requireContext(), context.getString(R.string.export_success), Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Backup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    private void importJsonFromUri(Uri uri) {
        try (InputStream is = requireContext().getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }

            String importedJson = jsonBuilder.toString();

            try {
                JSONArray jsonArray = new JSONArray(importedJson);

                FileHandling.overwriteRemindersToFile(requireContext(), jsonArray);
                NotificationScheduler.scheduleNotificationsAfterBoot(context);
                ClosestReminderWidget.updateWidgetDetails(context);
                FileHandling.checkFileAndUpdate(context);

                Toast.makeText(requireContext(), context.getString(R.string.import_success), Toast.LENGTH_SHORT).show();
            } catch (JSONException e) {
                Toast.makeText(requireContext(), "Invalid JSON format", Toast.LENGTH_LONG).show();
                Log.e("ImportJSON", "JSON Parsing error", e);
            }

        } catch (IOException e) {
            Toast.makeText(requireContext(), "Import failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }




    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void startLanguageIntent() {
        Intent intent = new Intent(Settings.ACTION_APP_LOCALE_SETTINGS);
        intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
        startActivity(intent);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}