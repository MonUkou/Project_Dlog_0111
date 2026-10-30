package com.example.myapplication.util;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.myapplication.ChatsActivity;
import com.example.myapplication.HomeActivity;
import com.example.myapplication.MenuActivity;
import com.example.myapplication.NotificationActivity;
import com.example.myapplication.R;
import com.example.myapplication.SearchActivity;

public class BottomNavHelper {

    public enum Tab {
        FEED, SEARCH, INBOX, PROFILE, MENU, NOTIFICATION
    }

    public static void setupBottomNav(Activity activity, Tab activeTab) {
        TextView navFeed = activity.findViewById(R.id.navFeed);
        TextView navSearch = activity.findViewById(R.id.navSearch);
        TextView navInbox = activity.findViewById(R.id.navInbox);
        TextView navProfile = activity.findViewById(R.id.navProfile);

        if (navFeed == null || navSearch == null || navInbox == null || navProfile == null) {
            return;
        }

        setTabStyle(navFeed, false);
        setTabStyle(navSearch, false);
        setTabStyle(navInbox, false);
        setTabStyle(navProfile, false);

        // Highlight active tab
        switch (activeTab) {
            case FEED:
                setTabStyle(navFeed, true);
                break;
            case SEARCH:
                setTabStyle(navSearch, true);
                break;
            case INBOX:
                setTabStyle(navInbox, true);
                break;
            case PROFILE:
            case MENU:
                setTabStyle(navProfile, true);
                break;
        }

        navFeed.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                if (activeTab != Tab.FEED) {
                    Intent intent = new Intent(activity, HomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    activity.startActivity(intent);
                }
            }
        });

        navSearch.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
            if (!NetworkState.isOnline(activity)) {
                android.widget.Toast.makeText(activity,
                        "Khám phá cần kết nối mạng.", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            if (activeTab != Tab.SEARCH) {
                Intent intent = new Intent(activity, SearchActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                activity.startActivity(intent);
            }
            }
        });

        navInbox.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
            if (!NetworkState.isOnline(activity)) {
                android.widget.Toast.makeText(activity,
                        "Hộp thư cần kết nối mạng.", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            if (activeTab != Tab.INBOX) {
                Intent intent = new Intent(activity, ChatsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                activity.startActivity(intent);
            }
            }
        });

        navProfile.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
            if (activeTab != Tab.PROFILE || (activity instanceof com.example.myapplication.ProfileActivity
                    && !((com.example.myapplication.ProfileActivity) activity).isSelfProfile())) {
                Intent intent = new Intent(activity, com.example.myapplication.ProfileActivity.class);
                intent.putExtra("openOwnProfile", true);
                activity.startActivity(intent);
            }
            }
        });
    }

    private static void setTabStyle(TextView tab, boolean active) {
        int color = ContextCompat.getColor(tab.getContext(),
                active ? R.color.dlog_blue : R.color.dlog_nav_inactive);
        tab.setTextColor(color);
        tab.setTypeface(ResourcesCompat.getFont(tab.getContext(),
                active ? R.font.inter_bold : R.font.inter_regular));
        tab.setCompoundDrawableTintList(ColorStateList.valueOf(color));
    }
}
