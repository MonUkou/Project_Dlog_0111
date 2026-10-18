package com.example.myapplication.util;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.widget.TextView;

import com.example.myapplication.CreatePostActivity;
import com.example.myapplication.HomeActivity;
import com.example.myapplication.NotificationActivity;
import com.example.myapplication.ProfileActivity;
import com.example.myapplication.R;
import com.example.myapplication.SearchActivity;

public class BottomNavHelper {

    public enum Tab {
        FEED, SEARCH, CREATE, NOTIFICATION, PROFILE
    }

    public static void setupBottomNav(Activity activity, Tab activeTab) {
        TextView navFeed = activity.findViewById(R.id.navFeed);
        TextView navSearch = activity.findViewById(R.id.navSearch);
        TextView navCreate = activity.findViewById(R.id.navCreate);
        TextView navNotification = activity.findViewById(R.id.navNotification);
        TextView navProfile = activity.findViewById(R.id.navProfile);

        if (navFeed == null || navSearch == null || navCreate == null || navNotification == null || navProfile == null) {
            return;
        }

        // Reset styles
        navFeed.setTextColor(0xFF666666);
        navFeed.setTypeface(null, Typeface.NORMAL);

        navSearch.setTextColor(0xFF666666);
        navSearch.setTypeface(null, Typeface.NORMAL);

        navCreate.setTextColor(0xFF666666);
        navCreate.setTypeface(null, Typeface.NORMAL);

        navNotification.setTextColor(0xFF666666);
        navNotification.setTypeface(null, Typeface.NORMAL);

        navProfile.setTextColor(0xFF666666);
        navProfile.setTypeface(null, Typeface.NORMAL);

        // Highlight active tab
        switch (activeTab) {
            case FEED:
                navFeed.setTextColor(0xFF000000);
                navFeed.setTypeface(null, Typeface.BOLD);
                break;
            case SEARCH:
                navSearch.setTextColor(0xFF000000);
                navSearch.setTypeface(null, Typeface.BOLD);
                break;
            case CREATE:
                navCreate.setTextColor(0xFF000000);
                navCreate.setTypeface(null, Typeface.BOLD);
                break;
            case NOTIFICATION:
                navNotification.setTextColor(0xFF000000);
                navNotification.setTypeface(null, Typeface.BOLD);
                break;
            case PROFILE:
                navProfile.setTextColor(0xFF000000);
                navProfile.setTypeface(null, Typeface.BOLD);
                break;
        }

        navFeed.setOnClickListener(v -> {
            if (activeTab != Tab.FEED) {
                Intent intent = new Intent(activity, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                activity.startActivity(intent);
            }
        });

        navSearch.setOnClickListener(v -> {
            if (activeTab != Tab.SEARCH) {
                Intent intent = new Intent(activity, SearchActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                activity.startActivity(intent);
            }
        });

        navCreate.setOnClickListener(v -> {
            Intent intent = new Intent(activity, CreatePostActivity.class);
            activity.startActivity(intent);
        });

        navNotification.setOnClickListener(v -> {
            if (activeTab != Tab.NOTIFICATION) {
                Intent intent = new Intent(activity, NotificationActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                activity.startActivity(intent);
            }
        });

        navProfile.setOnClickListener(v -> {
            if (activeTab != Tab.PROFILE) {
                Intent intent = new Intent(activity, ProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                activity.startActivity(intent);
            }
        });
    }
}
