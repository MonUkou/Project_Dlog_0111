package com.example.myapplication.util;

import android.app.Activity;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/** Keeps the Android navigation keys hidden while retaining the status bar. */
public final class UiChrome {
    private UiChrome() { }

    public static void hideNavigationBar(Activity activity) {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                activity.getWindow(), activity.getWindow().getDecorView());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        controller.hide(WindowInsetsCompat.Type.navigationBars());
    }
}
