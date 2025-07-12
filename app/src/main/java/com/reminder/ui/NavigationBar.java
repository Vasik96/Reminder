package com.reminder.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.reminder.R;

import java.util.Objects;

public class NavigationBar {
    private final BottomNavigationView navView;
    private final Context context;
    private final FragmentManager fragmentManager;

    public NavigationBar(BottomNavigationView navView, Context context, FragmentManager fragmentManager) {
        this.navView = navView;
        this.context = context;
        this.fragmentManager = fragmentManager;
    }

    public void setupNavController(NavController navController) {
        navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController controller, @NonNull NavDestination destination, @Nullable Bundle arguments) {
                updateMenuItemIcons(destination.getId());
            }
        });

        updateMenuItemIcons(Objects.requireNonNull(navController.getCurrentDestination()).getId());
    }

    private void updateMenuItemIcons(int destinationId) {
        resetMenuItemIcons();

        if (destinationId == R.id.navigation_home) {
            setMenuItemIcon(R.id.navigation_home, R.drawable.ic_home_fill);
        }
        else if (destinationId == R.id.navigation_notes) {
            setMenuItemIcon(R.id.navigation_notes, R.drawable.ic_notes_fill);
        }
        else if (destinationId == R.id.navigation_settings) {
            setMenuItemIcon(R.id.navigation_settings, R.drawable.ic_settings_fill);
        }
    }

    private void resetMenuItemIcons() {

        navView.getMenu().findItem(R.id.navigation_home).setIcon(R.drawable.ic_home);
        navView.getMenu().findItem(R.id.navigation_settings).setIcon(R.drawable.ic_settings);
        navView.getMenu().findItem(R.id.navigation_notes).setIcon(R.drawable.ic_notes);
    }

    public void setMenuItemIcon(int menuItemId, int iconResId) {
        Drawable icon = ContextCompat.getDrawable(context, iconResId);
        if (icon != null) {
            navView.getMenu().findItem(menuItemId).setIcon(icon);
        }
    }
}
