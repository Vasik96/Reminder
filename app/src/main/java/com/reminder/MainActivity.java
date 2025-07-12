package com.reminder;

import static com.reminder.AppThemeDialog.DARK_THEME;
import static com.reminder.AppThemeDialog.LIGHT_THEME;
import static com.reminder.AppThemeDialog.PREF_THEME_KEY;
import static com.reminder.AppThemeDialog.SYSTEM_DEFAULT;
import static com.reminder.Constants.WIDGET_LAUNCH;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.core.splashscreen.SplashScreen;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreenViewProvider;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.floatingtoolbar.FloatingToolbarLayout;
import com.reminder.databinding.ActivityMainBinding;
import com.reminder.ui.NavigationBar;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    PowerManager pm;
    String pkg;
    private NavigationBar navigationBar;
    private NavController navController;
    private boolean isThemeApplied = false;
    Context context;
    //private MainViewModel mainViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*boolean isWidgetLaunch = getIntent().getBooleanExtra("WIDGET_LAUNCH", false);



        ///splash screen stuff
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        splashScreen.setKeepOnScreenCondition(() -> {
            Boolean isReady = mainViewModel.getIsReady().getValue();
            return isReady == null || !isReady;

        });

        if (!isWidgetLaunch) {
            splashScreen.setOnExitAnimationListener(screen -> {
                View iconView = screen.getIconView();


                ObjectAnimator zoomX = ObjectAnimator.ofFloat(
                        iconView,
                        View.SCALE_X,
                        0.5f,
                        0.0f
                );
                zoomX.setInterpolator(new OvershootInterpolator());
                zoomX.setDuration(500L);


                ObjectAnimator zoomY = ObjectAnimator.ofFloat(
                        iconView,
                        View.SCALE_Y,
                        0.5f,
                        0.0f
                );
                zoomY.setInterpolator(new OvershootInterpolator());
                zoomY.setDuration(500L);

                zoomX.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        screen.remove();
                    }
                });

                zoomX.start();
                zoomY.start();
            });
        }*/


        context = getApplicationContext();

        if (!isThemeApplied) {
            applySavedTheme(this);
            isThemeApplied = true;
        }

        pkg = getPackageName();
        pm = getSystemService(PowerManager.class);


        createNotificationChannel(this);


        binding = ActivityMainBinding.inflate(getLayoutInflater());

        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        FragmentManager fragmentManager = getSupportFragmentManager();
        navigationBar = new NavigationBar(binding.navView, this, fragmentManager);
        navigationBar.setupNavController(navController);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }


    public void createNotificationChannel(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                "Reminder Notification",
                NotificationManager.IMPORTANCE_HIGH
        );
        notificationManager.createNotificationChannel(channel);
    }

    @Override
    public void onResume() {
        super.onResume();
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
            startActivity(intent);
        }
        requestPerms();
        NotificationScheduler.scheduleMissedReminderWork(getApplicationContext());
    }

    @Override
    public void onPause() {
        ClosestReminderWidget.updateWidgetDetails(context);
        super.onPause();
    }


    @SuppressLint("BatteryLife")
    public void requestPerms() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS},101);
            }
        }

        if (!pm.isIgnoringBatteryOptimizations(pkg)) {
            Intent intent = new
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:" + pkg));

            startActivity(intent);
        }

    }


    public void applySavedTheme(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String selectedTheme = sharedPreferences.getString(PREF_THEME_KEY, SYSTEM_DEFAULT);

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