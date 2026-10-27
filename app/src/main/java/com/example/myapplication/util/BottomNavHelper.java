package com.example.myapplication.util;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.myapplication.ChatsActivity;
import com.example.myapplication.HomeActivity;
import com.example.myapplication.MenuActivity;
import com.example.myapplication.NotificationActivity;
import com.example.myapplication.ProfileActivity;
import com.example.myapplication.R;
import com.example.myapplication.SearchActivity;

public class BottomNavHelper {

    public enum Tab {
        FEED, SEARCH, INBOX, PROFILE, MENU, NOTIFICATION
    }

    public static void setupBottomNav(Activity activity, Tab activeTab) {
        View navFeed = activity.findViewById(R.id.navFeed);
        View navSearch = activity.findViewById(R.id.navSearch);
        View navInbox = activity.findViewById(R.id.navInbox);
        View navProfile = activity.findViewById(R.id.navProfile);

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
            if (activeTab != Tab.SEARCH) {
                Intent intent = new Intent(activity, SearchActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                activity.startActivity(intent);
            }
            }
        });

        navInbox.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
            if (activeTab != Tab.INBOX) {
                Intent intent = new Intent(activity, ChatsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                activity.startActivity(intent);
            }
            }
        });

        navProfile.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
            if (activeTab != Tab.PROFILE) {
                Intent intent = new Intent(activity, ProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                activity.startActivity(intent);
            }
            }
        });
    }

    private static void setTabStyle(View tab, boolean active) {
        int color = ContextCompat.getColor(tab.getContext(),
                active ? R.color.dlog_blue : R.color.dlog_nav_inactive);
        if (tab instanceof TextView) {
            TextView tv = (TextView) tab;
            tv.setTextColor(color);
            tv.setTypeface(ResourcesCompat.getFont(tv.getContext(),
                    active ? R.font.inter_bold : R.font.inter_regular));
            tv.setCompoundDrawableTintList(ColorStateList.valueOf(color));
        } else if (tab instanceof ImageView) {
            ImageView iv = (ImageView) tab;
            iv.setImageTintList(ColorStateList.valueOf(color));
        }
    }
}
