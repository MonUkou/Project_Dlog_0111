package com.example.myapplication.util;

import android.app.Activity;
import android.content.Context;
import android.provider.Settings;
import android.view.View;

import com.example.myapplication.R;

/** Shared transitions that follow the device's reduced-motion setting. */
public final class UiMotion {
    private UiMotion() { }

    public static boolean isEnabled(Context context) {
        try {
            return Settings.Global.getFloat(context.getContentResolver(),
                    Settings.Global.TRANSITION_ANIMATION_SCALE, 1f) > 0f;
        } catch (Exception ignored) {
            return true;
        }
    }

    public static void forward(Activity activity) {
        if (isEnabled(activity)) {
            activity.overridePendingTransition(R.anim.dlog_enter, R.anim.dlog_exit);
        }
    }

    public static void backward(Activity activity) {
        if (isEnabled(activity)) {
            activity.overridePendingTransition(R.anim.dlog_back_enter, R.anim.dlog_back_exit);
        }
    }

    public static void reveal(View view, int position) {
        view.animate().cancel();
        view.setAlpha(1f);
        view.setTranslationY(0f);
        if (!isEnabled(view.getContext()) || position > 2) return;
        view.setAlpha(0f);
        view.setTranslationY(12f * view.getResources().getDisplayMetrics().density);
        view.animate().alpha(1f).translationY(0f)
                .setDuration(180L + position * 35L).start();
    }
}
