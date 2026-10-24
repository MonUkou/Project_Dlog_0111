package com.example.myapplication.util;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.content.Intent;
import android.graphics.Typeface;
import android.widget.TextView;

import com.example.myapplication.CreatePostActivity;
import com.example.myapplication.HomeActivity;
import com.example.myapplication.MenuActivity;
import com.example.myapplication.NotificationActivity;
import com.example.myapplication.R;
import com.example.myapplication.SearchActivity;

public class BottomNavHelper {

    public enum Tab {
        FEED, SEARCH, CREATE, NOTIFICATION, PROFILE, MENU
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

        setTabStyle(navFeed, false);
        setTabStyle(navSearch, false);
        setTabStyle(navCreate, false);
        setTabStyle(navNotification, false);
        setTabStyle(navProfile, false);

        // Highlight active tab
        switch (activeTab) {
            case FEED:
                setTabStyle(navFeed, true);
                break;
            case SEARCH:
                setTabStyle(navSearch, true);
                break;
            case CREATE:
                setTabStyle(navCreate, true);
                break;
            case NOTIFICATION:
                setTabStyle(navNotification, true);
                break;
            case PROFILE:
            case MENU:
                setTabStyle(navProfile, true);
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
            if (!(activity instanceof MenuActivity)) {
                Intent intent = new Intent(activity, MenuActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                activity.startActivity(intent);
            }
        });
    }

    private static void setTabStyle(TextView tab, boolean active) {
        int color = active ? 0xFF0756B5 : 0xFF667A98;
        tab.setTextColor(color);
        tab.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
        // The raised create action uses a white plus over its own blue circle.
        if (tab.getId() != R.id.navCreate) {
            tab.setCompoundDrawableTintList(ColorStateList.valueOf(color));
        }
    }
}
