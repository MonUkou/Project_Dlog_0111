package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


/** Read-only admin dashboard backed by the existing Firestore collections. */
public class AdminActivity extends AppCompatActivity {
    private static final int BLUE = Color.rgb(36, 86, 155);
    private static final int NAVY = Color.rgb(23, 43, 77);
    private static final int MUTED = Color.rgb(102, 122, 152);
    private static final int GREEN = Color.rgb(14, 160, 106);
    private static final int RED = Color.rgb(221, 67, 77);

    private LinearLayout content;
    private TextView title;
    private TextView activeTab;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        content = findViewById(R.id.adminContent);
        title = findViewById(R.id.tvAdminTitle);
        db = FirebaseFirestore.getInstance();
        findViewById(R.id.btnAdminBack).setOnClickListener(v -> finish());

        com.example.myapplication.service.impl.AuthService auth =
                com.example.myapplication.service.impl.AuthService.getInstance();
        auth.init(this);
        String uid = auth.getCurrentUserId();
        if (uid == null) {
            denyAccess();
            return;
        }
        db.collection("users").document(uid).get()
                .addOnSuccessListener(user -> {
                    if (!"admin".equalsIgnoreCase(user.getString("role"))) {
                        denyAccess();
                        return;
                    }
                    bindNavigation();
                    showOverview();
                })
                .addOnFailureListener(error -> denyAccess());
    }

    private void denyAccess() {
        Toast.makeText(this, "Tài khoản chưa được cấp quyền quản trị.", Toast.LENGTH_LONG).show();
        finish();
    }

    private void bindNavigation() {
        TextView overview = findViewById(R.id.adminNavOverview);
        TextView users = findViewById(R.id.adminNavUsers);
        TextView posts = findViewById(R.id.adminNavPosts);
        TextView comments = findViewById(R.id.adminNavComments);
        TextView reports = findViewById(R.id.adminNavReports);
        TextView settings = findViewById(R.id.adminNavSettings);
        overview.setOnClickListener(v -> { setActiveTab(overview); showOverview(); });
        users.setOnClickListener(v -> { setActiveTab(users); showCollection("users", "Quản lý người dùng"); });
        posts.setOnClickListener(v -> { setActiveTab(posts); showCollection("posts", "Quản lý bài viết"); });
        comments.setOnClickListener(v -> { setActiveTab(comments); showCollection("comments", "Quản lý bình luận"); });
        reports.setOnClickListener(v -> { setActiveTab(reports); showCollection("reports", "Báo cáo & Thống kê"); });
        settings.setOnClickListener(v -> { setActiveTab(settings); showSettings(); });
        setActiveTab(overview);
    }

    private void setActiveTab(TextView tab) {
        if (activeTab != null) {
            activeTab.setBackgroundColor(Color.TRANSPARENT);
            activeTab.setTextColor(Color.rgb(214, 228, 247));
            activeTab.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        }
        activeTab = tab;
        GradientDrawable selected = new GradientDrawable();
        selected.setColor(Color.rgb(36, 86, 155));
        selected.setCornerRadius(dp(12));
        tab.setBackground(selected);
        tab.setTextColor(Color.WHITE);
        tab.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    }

    private void showOverview() {
        title.setText("Tổng quan");
        content.removeAllViews();
        addHeading("Xin chào, Admin 👋", "Dữ liệu trực tiếp từ DLog");

        GridLayout metrics = new GridLayout(this);
        metrics.setColumnCount(2);
        metrics.setUseDefaultMargins(false);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        content.addView(metrics, gridParams);
        addMetric(metrics, "Người dùng", "users", "👥", GREEN);
        addMetric(metrics, "Bài viết", "posts", "▣", BLUE);
        addMetric(metrics, "Bình luận", "comments", "◉", BLUE);
        addMetric(metrics, "Báo cáo", "reports", "⚑", RED);

        addSectionTitle("Bài viết gần đây", "Từ Firestore");
        db.collection("posts").orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(5).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) addEmpty("Chưa có bài viết nào.");
                    else for (DocumentSnapshot document : snapshot.getDocuments()) addPostRow(document);
                })
                .addOnFailureListener(error -> addEmpty("Không thể tải bài viết. Kiểm tra quyền đọc Firestore."));
    }

    private void addMetric(GridLayout grid, String label, String collection, String symbol, int accent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(15), dp(14), dp(15), dp(14));
        card.setBackgroundResource(R.drawable.bg_admin_card);
        card.setElevation(dp(3));

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(dp(3), dp(3), dp(6), dp(8));
        grid.addView(card, params);

        TextView icon = new TextView(this);
        icon.setText(symbol);
        icon.setTextColor(accent);
        icon.setTextSize(20);
        icon.setGravity(Gravity.CENTER);
        icon.setBackgroundResource(R.drawable.bg_story_add);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(42), dp(42));
        card.addView(icon, iconParams);

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(MUTED);
        labelView.setTextSize(12);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, -2);
        labelParams.topMargin = dp(11);
        card.addView(labelView, labelParams);

        TextView value = new TextView(this);
        value.setText("…");
        value.setTextColor(NAVY);
        value.setTextSize(22);
        value.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(value);

        db.collection(collection).get()
                .addOnSuccessListener(snapshot -> value.setText(String.valueOf(snapshot.size())))
                .addOnFailureListener(error -> value.setText("—"));
    }

    private void showCollection(String collection, String label) {
        title.setText(label);
        content.removeAllViews();
        addHeading(label, "Dữ liệu mới nhất từ Firestore");
        TextView loading = createText("Đang tải dữ liệu…", 14, MUTED, false);
        content.addView(loading);

        db.collection(collection).limit(100).get()
                .addOnSuccessListener(snapshot -> {
                    content.removeView(loading);
                    if (snapshot.isEmpty()) {
                        addEmpty(collection.equals("comments")
                                ? "Chưa có bình luận. Bình luận sẽ xuất hiện sau khi người dùng tương tác với bài viết."
                                : "Chưa có dữ liệu trong mục này.");
                        return;
                    }
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        switch (collection) {
                            case "users": addUserRow(document); break;
                            case "posts": addPostRow(document); break;
                            case "comments": addCommentRow(document); break;
                            default: addReportRow(document); break;
                        }
                    }
                })
                .addOnFailureListener(error -> {
                    content.removeView(loading);
                    addEmpty("Không thể tải dữ liệu. Hãy kiểm tra quyền đọc Firestore cho tài khoản quản trị.");
                });
    }

    private void addUserRow(DocumentSnapshot doc) {
        LinearLayout row = createRow();
        ImageView avatar = createAvatar();
        String avatarUrl = doc.getString("avatarUrl");
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            Glide.with(this).load(avatarUrl).circleCrop().into(avatar);
        }
        row.addView(avatar);
        String name = firstValue(doc, "fullName", "username");
        String email = firstValue(doc, "email");
        String role = "admin".equalsIgnoreCase(doc.getString("role")) ? "Quản trị viên" : "Thành viên";
        row.addView(createTextColumn(name, email, role, GREEN));
        row.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.putExtra("userId", doc.getId());
            startActivity(intent);
        });
        content.addView(row);
    }

    private void addPostRow(DocumentSnapshot doc) {
        LinearLayout row = createRow();
        ImageView photo = new ImageView(this);
        LinearLayout.LayoutParams photoParams = new LinearLayout.LayoutParams(dp(78), dp(78));
        photoParams.setMarginEnd(dp(12));
        photo.setLayoutParams(photoParams);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photo.setBackgroundResource(R.drawable.bg_rounded_edittext);
        String imageUrl = doc.getString("imageUrl");
        if (imageUrl != null && !imageUrl.trim().isEmpty()) Glide.with(this).load(imageUrl).centerCrop().into(photo);
        row.addView(photo);
        String author = firstValue(doc, "authorUsername", "authorId");
        String caption = firstValue(doc, "caption");
        long likes = numberValue(doc, "likesCount");
        long comments = numberValue(doc, "commentsCount");
        row.addView(createTextColumn(caption, author, likes + " lượt thích · " + comments + " bình luận", BLUE));
        row.setOnClickListener(v -> {
            Intent intent = new Intent(this, PostDetailActivity.class);
            intent.putExtra(PostDetailActivity.EXTRA_POST_ID, doc.getId());
            startActivity(intent);
        });
        content.addView(row);
    }

    private void addCommentRow(DocumentSnapshot doc) {
        LinearLayout row = createRow();
        ImageView avatar = createAvatar();
        String avatarUrl = doc.getString("avatarUrl");
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) Glide.with(this).load(avatarUrl).circleCrop().into(avatar);
        row.addView(avatar);
        row.addView(createTextColumn(firstValue(doc, "username", "authorId"),
                firstValue(doc, "content"), "Bài viết " + firstValue(doc, "postId"), BLUE));
        content.addView(row);
    }

    private void addReportRow(DocumentSnapshot doc) {
        LinearLayout row = createRow();
        row.addView(createTextColumn(firstValue(doc, "title", "type", "reason"),
                firstValue(doc, "description", "message"), "Mã: " + doc.getId(), RED));
        content.addView(row);
    }

    private LinearLayout createRow() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(11), dp(12), dp(11));
        row.setBackgroundResource(R.drawable.bg_admin_card);
        row.setElevation(dp(2));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(8);
        row.setLayoutParams(params);
        return row;
    }

    private ImageView createAvatar() {
        ImageView avatar = new ImageView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(48), dp(48));
        params.setMarginEnd(dp(12));
        avatar.setLayoutParams(params);
        avatar.setBackgroundResource(R.drawable.bg_circle_avatar);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return avatar;
    }

    private LinearLayout createTextColumn(String primary, String secondary, String meta, int accent) {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        column.addView(createText(primary, 14, NAVY, true));
        column.addView(createText(secondary, 12, MUTED, false));
        TextView badge = createText(meta, 11, accent, true);
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(-2, -2);
        badgeParams.topMargin = dp(4);
        column.addView(badge, badgeParams);
        return column;
    }

    private void showSettings() {
        title.setText("Cài đặt hệ thống");
        content.removeAllViews();
        addHeading("Cài đặt hệ thống", "DLog · Phiên bản 1.0.0");
        addSetting("Thông tin ứng dụng", "Phiên bản 1.0.0");
        addSetting("Quản lý nội dung", "Quyền truy cập do Firestore Rules kiểm soát");
        addSetting("Bình luận", "Dữ liệu được tải từ collection comments");
        addSetting("Sao lưu dữ liệu", "Backup & Restore");
        addSetting("Ngôn ngữ", "Tiếng Việt");
    }

    private void addSetting(String title, String subtitle) {
        LinearLayout row = createRow();
        row.addView(createTextColumn(title, subtitle, "›", BLUE));
        content.addView(row);
    }

    private void addHeading(String text, String subtitle) {
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18), dp(17), dp(18), dp(17));
        hero.setBackgroundResource(R.drawable.bg_admin_welcome);
        hero.setElevation(dp(3));
        LinearLayout.LayoutParams heroParams = new LinearLayout.LayoutParams(-1, -2);
        heroParams.bottomMargin = dp(16);
        content.addView(hero, heroParams);

        TextView heading = createText(text, 20, Color.WHITE, true);
        hero.addView(heading);
        TextView sub = createText(subtitle, 12, Color.rgb(217, 231, 249), false);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.topMargin = dp(5);
        hero.addView(sub, subParams);
    }

    private void addSectionTitle(String text, String trailing) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(8));
        TextView left = createText(text, 16, NAVY, true);
        row.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(createText(trailing, 11, MUTED, false));
        content.addView(row);
    }

    private void addEmpty(String message) {
        TextView empty = createText(message, 13, MUTED, false);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(18), dp(26), dp(18), dp(26));
        empty.setBackgroundResource(R.drawable.bg_admin_card);
        content.addView(empty);
    }

    private TextView createText(String text, int sizeSp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text == null ? "—" : text);
        view.setTextColor(color);
        view.setTextSize(sizeSp);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private String firstValue(DocumentSnapshot document, String... fields) {
        for (String field : fields) {
            String value = document.getString(field);
            if (value != null && !value.trim().isEmpty()) return value;
        }
        return "—";
    }

    private long numberValue(DocumentSnapshot document, String field) {
        Long value = document.getLong(field);
        return value == null ? 0 : value;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
