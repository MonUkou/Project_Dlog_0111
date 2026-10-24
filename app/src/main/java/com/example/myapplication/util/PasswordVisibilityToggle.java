package com.example.myapplication.util;

import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;

/** Gives password fields a crisp, tappable eye control without shrinking the input area. */
public final class PasswordVisibilityToggle {
    private PasswordVisibilityToggle() {}

    public static void bind(@NonNull EditText input) {
        input.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View view, MotionEvent event) {
            if (event.getAction() != MotionEvent.ACTION_UP) return false;
            android.graphics.drawable.Drawable eye = input.getCompoundDrawablesRelative()[2];
            if (eye == null) return false;
            float touchAreaStart = input.getWidth() - input.getPaddingEnd()
                    - Math.max(eye.getBounds().width(), eye.getIntrinsicWidth()) - 12f;
            if (event.getX() < touchAreaStart) return false;

            int selection = input.getSelectionStart();
            boolean hidden = input.getTransformationMethod() instanceof PasswordTransformationMethod;
            input.setTransformationMethod(hidden
                    ? HideReturnsTransformationMethod.getInstance()
                    : PasswordTransformationMethod.getInstance());
            if (selection >= 0) input.setSelection(Math.min(selection, input.length()));
            return true;
            }
        });
    }
}
