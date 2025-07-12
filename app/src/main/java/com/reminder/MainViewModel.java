/*package com.reminder;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.os.Handler;
import android.os.Looper;

public class MainViewModel extends ViewModel {
    private final MutableLiveData<Boolean> isReady = new MutableLiveData<>(false);
    private static int SPLASH_SCREEN_DELAY = 900;

    public MainViewModel() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            isReady.setValue(true);
        }, SPLASH_SCREEN_DELAY);
    }

    public LiveData<Boolean> getIsReady() {
        return isReady;
    }
}
*/