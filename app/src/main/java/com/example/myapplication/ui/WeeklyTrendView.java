package com.example.myapplication.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.myapplication.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** Small, accessible seven-day trend chart with no additional library dependency. */
public final class WeeklyTrendView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int[] users = new int[7];
    private final int[] posts = new int[7];
    private final int[] interactions = new int[7];
    private final String[] labels = new String[7];
    private final int muted;
    private final int grid;
    private final int blue;
    private final int green;
    private final int purple;
    private final float density;

    public WeeklyTrendView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        muted = ContextCompat.getColor(context, R.color.dlog_muted);
        grid = ContextCompat.getColor(context, R.color.dlog_border);
        blue = ContextCompat.getColor(context, R.color.dlog_blue);
        green = ContextCompat.getColor(context, R.color.dlog_success);
        purple = ContextCompat.getColor(context, R.color.dlog_chart_interaction);
        Typeface inter = ResourcesCompat.getFont(context, R.font.inter_medium);
        paint.setTypeface(inter);
        SimpleDateFormat format = new SimpleDateFormat("dd/MM", Locale.getDefault());
        Calendar day = Calendar.getInstance();
        day.add(Calendar.DAY_OF_YEAR, -6);
        for (int i = 0; i < 7; i++) {
            labels[i] = format.format(day.getTime());
            day.add(Calendar.DAY_OF_YEAR, 1);
        }
        setContentDescription("Biểu đồ người dùng mới, bài viết và tương tác trong 7 ngày");
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    public void setData(@NonNull int[] newUsers, @NonNull int[] newPosts,
                        @NonNull int[] newInteractions) {
        System.arraycopy(newUsers, 0, users, 0, Math.min(7, newUsers.length));
        System.arraycopy(newPosts, 0, posts, 0, Math.min(7, newPosts.length));
        System.arraycopy(newInteractions, 0, interactions, 0, Math.min(7, newInteractions.length));
        StringBuilder description = new StringBuilder("Thống kê 7 ngày. ");
        for (int i = 0; i < 7; i++) {
            description.append(labels[i]).append(": ")
                    .append(users[i]).append(" người dùng mới, ")
                    .append(posts[i]).append(" bài viết, ")
                    .append(interactions[i]).append(" tương tác. ");
        }
        setContentDescription(description.toString());
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = (int) (210 * density);
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),
                resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float left = 36 * density;
        float right = getWidth() - 14 * density;
        float top = 18 * density;
        float bottom = getHeight() - 35 * density;
        if (right <= left) return;
        int max = 1;
        for (int i = 0; i < 7; i++) {
            max = Math.max(max, Math.max(users[i], Math.max(posts[i], interactions[i])));
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(density);
        paint.setColor(grid);
        for (int i = 0; i <= 3; i++) {
            float y = bottom - (bottom - top) * i / 3f;
            canvas.drawLine(left, y, right, y, paint);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(muted);
        paint.setTextSize(10 * getResources().getDisplayMetrics().scaledDensity);
        for (int i = 0; i <= 3; i++) {
            float y = bottom - (bottom - top) * i / 3f;
            canvas.drawText(String.valueOf(Math.round(max * i / 3f)), 4 * density,
                    y + 3 * density, paint);
        }
        for (int i = 0; i < 7; i++) {
            float x = left + (right - left) * i / 6f;
            canvas.drawText(labels[i], x - 13 * density, getHeight() - 11 * density, paint);
        }
        drawSeries(canvas, users, blue, max, left, right, top, bottom);
        drawSeries(canvas, posts, green, max, left, right, top, bottom);
        drawSeries(canvas, interactions, purple, max, left, right, top, bottom);
    }

    private void drawSeries(Canvas canvas, int[] values, int color, int max,
                            float left, float right, float top, float bottom) {
        Path line = new Path();
        for (int i = 0; i < 7; i++) {
            float x = left + (right - left) * i / 6f;
            float y = bottom - (bottom - top) * values[i] / max;
            if (i == 0) line.moveTo(x, y); else line.lineTo(x, y);
        }
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f * density);
        canvas.drawPath(line, paint);
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 7; i++) {
            float x = left + (right - left) * i / 6f;
            float y = bottom - (bottom - top) * values[i] / max;
            canvas.drawCircle(x, y, 3.5f * density, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            performClick();
            float left = 36 * density;
            float right = getWidth() - 14 * density;
            int index = Math.max(0, Math.min(6,
                    Math.round((event.getX() - left) * 6 / Math.max(1, right - left))));
            Toast.makeText(getContext(), labels[index] + ": " + users[index]
                    + " người dùng mới, " + posts[index] + " bài viết, "
                    + interactions[index] + " tương tác", Toast.LENGTH_SHORT).show();
            return true;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }
}
