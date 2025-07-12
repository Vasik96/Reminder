package com.reminder;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class AppThemeDialog {

    public static final String PREF_THEME_KEY = "selected_theme";
    static final String SYSTEM_DEFAULT = "system_default";
    static final String DARK_THEME = "dark";
    static final String LIGHT_THEME = "light";

    private Context context;
    private Runnable onThemeApplied;

    public AppThemeDialog(Context context, Runnable onThemeApplied) {
        this.context = context;
        this.onThemeApplied = onThemeApplied;
    }

    public void showThemeDialog() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String currentTheme = sharedPreferences.getString(PREF_THEME_KEY, SYSTEM_DEFAULT);

        
        android.view.LayoutInflater inflater = android.view.LayoutInflater.from(context);
        android.view.View dialogView = inflater.inflate(R.layout.app_theme_dialog, null);


        RadioGroup themeOptionsGroup = dialogView.findViewById(R.id.themeOptionsGroup);
        assert themeOptionsGroup != null;

        switch (currentTheme) {
            case DARK_THEME:
                themeOptionsGroup.check(R.id.themeDark);
                break;
            case LIGHT_THEME:
                themeOptionsGroup.check(R.id.themeLight);
                break;
            case SYSTEM_DEFAULT:
            default:
                themeOptionsGroup.check(R.id.themeSystemDefault);
                break;
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context, com.google.android.material.R.style.ThemeOverlay_Material3Expressive_MaterialAlertDialog_Centered)
                .setTitle(R.string.theme_dialog)
                .setView(dialogView)
                .setIcon(R.drawable.dark_mode)
                .setPositiveButton("OK", (dialog, which) -> {
                    int selectedId = themeOptionsGroup.getCheckedRadioButtonId();
                    String selectedTheme = SYSTEM_DEFAULT;

                    if (selectedId == R.id.themeDark) {
                        selectedTheme = DARK_THEME;
                    } else if (selectedId == R.id.themeLight) {
                        selectedTheme = LIGHT_THEME;
                    } else if (selectedId == R.id.themeSystemDefault) {
                        selectedTheme = SYSTEM_DEFAULT;
                    }

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(PREF_THEME_KEY, selectedTheme);
                    editor.apply();

                    applyTheme(selectedTheme);

                    if (onThemeApplied != null) {
                        onThemeApplied.run();
                    }
                })
                .setNegativeButton(R.string.cancel_txt, null);

        builder.show();
    }


    private void applyTheme(String selectedTheme) {
        switch (selectedTheme) {
            case DARK_THEME:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case LIGHT_THEME:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case SYSTEM_DEFAULT:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}
