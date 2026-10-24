package com.example.myapplication.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

public class ZoomDismissImageView extends AppCompatImageView {
    public interface GestureCallback {
        void onDismiss();
        void onPrevious();
        void onNext();
    }

    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;
    private float scale = 1f;
    private float downY;
    private GestureCallback callback;

    public ZoomDismissImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.FIT_CENTER);
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(@NonNull ScaleGestureDetector detector) {
                scale = Math.max(1f, Math.min(4f, scale * detector.getScaleFactor()));
                setPivotX(detector.getFocusX());
                setPivotY(detector.getFocusY());
                setScaleX(scale);
                setScaleY(scale);
                if (scale > 1f) setTranslationY(0f);
                return true;
            }
        });
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDoubleTap(@NonNull MotionEvent event) {
                scale = scale > 1f ? 1f : 2.5f;
                setPivotX(event.getX());
                setPivotY(event.getY());
                animate().scaleX(scale).scaleY(scale).translationY(0f).setDuration(180).start();
                return true;
            }

            @Override public boolean onFling(MotionEvent first, MotionEvent second, float velocityX, float velocityY) {
                if (callback == null || scale > 1.05f || first == null || second == null) return false;
                float dx = second.getX() - first.getX();
                float dy = second.getY() - first.getY();
                if (dy > 160 && Math.abs(dy) > Math.abs(dx)) {
                    callback.onDismiss();
                    return true;
                }
                if (Math.abs(dx) > 120 && Math.abs(dx) > Math.abs(dy)) {
                    if (dx > 0) callback.onPrevious(); else callback.onNext();
                    return true;
                }
                return false;
            }
        });
    }

    public void setGestureCallback(GestureCallback value) { callback = value; }

    public void resetZoom() {
        scale = 1f;
        setScaleX(1f);
        setScaleY(1f);
        setTranslationY(0f);
        setAlpha(1f);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        if (scale <= 1.05f && !scaleDetector.isInProgress()) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) downY = event.getY();
            if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                float offset = Math.max(0f, event.getY() - downY);
                if (offset > 0f) {
                    setTranslationY(offset);
                    setAlpha(Math.max(0.35f, 1f - offset / Math.max(1f, getHeight())));
                }
            }
            if (event.getActionMasked() == MotionEvent.ACTION_UP
                    || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                if (getTranslationY() > getHeight() * 0.2f && callback != null) callback.onDismiss();
                else animate().translationY(0f).alpha(1f).setDuration(160).start();
            }
        }
        return true;
    }
}
