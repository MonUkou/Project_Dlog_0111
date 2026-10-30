package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.text.format.DateUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.bumptech.glide.Glide;
import com.example.myapplication.ui.WeeklyTrendView;
import com.example.myapplication.view.PhotoCollageView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;

import java.util.Calendar;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.text.Normalizer;
import java.util.Collections;
import java.util.Comparator;


/** Admin dashboard backed by the existing Firestore collections. */
public class AdminActivity extends AppCompatActivity {
    private int BLUE;
    private int NAVY;
    private int MUTED;
    private int GREEN;
    private int RED;
    private Typeface interRegular;
    private Typeface interBold;

    private LinearLayout content;
    private TextView title;
    private TextView activeTab;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        BLUE = ContextCompat.getColor(this, R.color.dlog_blue);
        NAVY = ContextCompat.getColor(this, R.color.dlog_ink);
        MUTED = ContextCompat.getColor(this, R.color.dlog_muted);
        GREEN = ContextCompat.getColor(this, R.color.dlog_success);
        RED = ContextCompat.getColor(this, R.color.dlog_error);
        interRegular = ResourcesCompat.getFont(this, R.font.inter_regular);
        interBold = ResourcesCompat.getFont(this, R.font.inter_bold);
        content = findViewById(R.id.adminContent);
        title = findViewById(R.id.tvAdminTitle);
        db = FirebaseFirestore.getInstance();

        findViewById(R.id.btnAdminBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        com.example.myapplication.service.impl.AuthService auth =
                com.example.myapplication.service.impl.AuthService.getInstance();
        auth.init(this);
        String uid = auth.getCurrentUserId();
        if (uid == null) {
            denyAccess();
            return;
        }
        db.collection("users").document(uid).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot user) {
                        if (!"admin".equalsIgnoreCase(user.getString("role"))) {
                            denyAccess();
                            return;
                        }
                        normalizeReadableCodes();
                        bindNavigation();
                        showOverview();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@androidx.annotation.NonNull Exception error) {
                        denyAccess();
                    }
                });
    }

    private void denyAccess() {
        Toast.makeText(this, "Tài khoản chưa được cấp quyền quản trị.", Toast.LENGTH_LONG).show();
        finish();
    }

    private void normalizeReadableCodes() {
        com.example.myapplication.service.impl.ReadableCodeMigration.run(db,
                new com.example.myapplication.service.impl.ReadableCodeMigration.Callback() {
                    @Override public void onSuccess(int users, int posts) {
                        Toast.makeText(AdminActivity.this,
                                "Đã chuẩn hóa " + users + " người dùng và " + posts + " bài đăng.",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override public void onError(Exception error) {
                        Toast.makeText(AdminActivity.this,
                                "Không thể gán mã quản lý: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void bindNavigation() {
        final TextView overview = findViewById(R.id.adminNavOverview);
        final TextView users = findViewById(R.id.adminNavUsers);
        final TextView posts = findViewById(R.id.adminNavPosts);
        final TextView reports = findViewById(R.id.adminNavReports);
        final TextView settings = findViewById(R.id.adminNavSettings);

        overview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setActiveTab(overview);
                showOverview();
            }
        });
        users.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setActiveTab(users);
                showCollection("users", "Quản lý người dùng");
            }
        });
        posts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setActiveTab(posts);
                showCollection("posts", "Quản lý bài viết");
            }
        });
        reports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setActiveTab(reports);
                showCollection("reports", "Báo cáo & Thống kê");
            }
        });
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setActiveTab(settings);
                showSettings();
            }
        });
        setActiveTab(overview);
    }

    private void setActiveTab(TextView tab) {
        if (activeTab != null) {
            activeTab.setBackgroundColor(Color.TRANSPARENT);
            activeTab.setTextColor(ContextCompat.getColor(this, R.color.dlog_on_drawer_muted));
            activeTab.setTypeface(interRegular);
        }
        activeTab = tab;

        // Sử dụng hiệu ứng gạch chân dưới tab đang chọn
        tab.setBackgroundResource(R.drawable.bg_tab_underline);
        tab.setTextColor(Color.WHITE);
        tab.setTypeface(interBold);
    }

    private void showOverview() {
        title.setText("Tổng quan");
        content.removeAllViews();

        GridLayout metrics = new GridLayout(this);
        metrics.setColumnCount(2);
        metrics.setUseDefaultMargins(false);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        content.addView(metrics, gridParams);
        addMetric(metrics, "Người dùng", "users", R.drawable.ic_person_outline, GREEN);
        addMetric(metrics, "Bài viết", "posts", R.drawable.ic_bookmark_24, BLUE);
        addMetric(metrics, "Bình luận", "comments", R.drawable.ic_comment_24, BLUE);
        addMetric(metrics, "Báo cáo", "reports", R.drawable.ic_info_outline_24, RED);

        addWeeklyChart();

        addSectionTitle("Bài viết gần đây", "Từ Firestore");
        db.collection("posts").orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(5).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot snapshot) {
                        if (snapshot.isEmpty()) {
                            addEmpty("Chưa có bài viết nào.");
                        } else {
                            for (DocumentSnapshot document : snapshot.getDocuments()) {
                                addPostRow(document);
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@androidx.annotation.NonNull Exception error) {
                        addEmpty("Không thể tải bài viết. Kiểm tra quyền đọc Firestore.");
                    }
                });
    }

    private void addMetric(GridLayout grid, String label, String collection, int iconRes, int accent) {
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

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setImageTintList(android.content.res.ColorStateList.valueOf(accent));
        icon.setPadding(dp(10), dp(10), dp(10), dp(10));
        icon.setContentDescription(label);
        icon.setBackgroundResource(R.drawable.bg_profile_stats);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(42), dp(42));
        card.addView(icon, iconParams);

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(MUTED);
        labelView.setTextSize(12);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, -2);
        labelParams.topMargin = dp(11);
        card.addView(labelView, labelParams);

        final TextView value = new TextView(this);
        value.setText("…");
        value.setTextColor(NAVY);
        value.setTextSize(22);
        value.setTypeface(interBold);
        card.addView(value);

        db.collection(collection).count().get(AggregateSource.SERVER)
                .addOnSuccessListener(new OnSuccessListener<AggregateQuerySnapshot>() {
                    @Override
                    public void onSuccess(AggregateQuerySnapshot snapshot) {
                        value.setText(String.valueOf(snapshot.getCount()));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@androidx.annotation.NonNull Exception error) {
                        value.setText("—");
                    }
                });
    }

    private void showCollection(final String collection, String label) {
        title.setText(label);
        content.removeAllViews();

        final TextView loading = createText("Đang tải dữ liệu…", 14, MUTED, false);
        content.addView(loading);

        Query collectionQuery = db.collection(collection);
        if ("posts".equals(collection)) {
            collectionQuery = collectionQuery
                    .orderBy("createdAt", Query.Direction.DESCENDING);
        }
        collectionQuery.limit(100).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot snapshot) {
                        content.removeView(loading);
                        if (snapshot.isEmpty()) {
                            addEmpty(collection.equals("comments")
                                    ? "Chưa có bình luận. Bình luận sẽ xuất hiện sau khi người dùng tương tác với bài viết."
                                    : "Chưa có dữ liệu trong mục này.");
                            return;
                        }
                        if (collection.equals("users")) {
                            showUserManagement(snapshot.getDocuments());
                            return;
                        }
                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            if (collection.equals("posts")) {
                                addPostRow(document);
                            } else if (collection.equals("comments")) {
                                addCommentRow(document);
                            } else {
                                addReportRow(document);
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@androidx.annotation.NonNull Exception error) {
                        content.removeView(loading);
                        addEmpty("Không thể tải dữ liệu. Hãy kiểm tra quyền đọc Firestore cho tài khoản quản trị.");
                    }
                });
    }

    private void showUserManagement(List<DocumentSnapshot> sourceDocuments) {
        final List<DocumentSnapshot> users = new ArrayList<>(sourceDocuments);
        Collections.sort(users, new Comparator<DocumentSnapshot>() {
            @Override public int compare(DocumentSnapshot first, DocumentSnapshot second) {
                int codeOrder = Integer.compare(userCodeNumber(second), userCodeNumber(first));
                if (codeOrder != 0) return codeOrder;
                return Long.compare(numberValue(second, "createdAt"), numberValue(first, "createdAt"));
            }
        });

        final EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Tìm theo tên, USR hoặc trạng thái");
        search.setContentDescription("Tìm kiếm người dùng theo tên, mã hoặc trạng thái");
        search.setTextColor(NAVY);
        search.setHintTextColor(MUTED);
        search.setTextSize(14);
        search.setPadding(dp(14), 0, dp(14), 0);
        search.setBackgroundResource(R.drawable.bg_rounded_edittext);
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(48));
        searchParams.bottomMargin = dp(12);
        content.addView(search, searchParams);

        final LinearLayout results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        content.addView(results, new LinearLayout.LayoutParams(-1, -2));
        renderUserResults(users, "", results);

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) {
                renderUserResults(users, text == null ? "" : text.toString(), results);
            }
            @Override public void afterTextChanged(Editable editable) { }
        });
    }

    private void renderUserResults(List<DocumentSnapshot> users, String rawQuery,
                                   LinearLayout results) {
        results.removeAllViews();
        String query = normalizeSearch(rawQuery);
        int shown = 0;
        for (DocumentSnapshot user : users) {
            if (!matchesUserSearch(user, query)) continue;
            addUserRow(user, results);
            shown++;
        }
        if (shown == 0) {
            TextView empty = createText("Không tìm thấy người dùng phù hợp.", 13, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(18), dp(24), dp(18), dp(24));
            empty.setBackgroundResource(R.drawable.bg_admin_card);
            results.addView(empty);
        }
    }

    private boolean matchesUserSearch(DocumentSnapshot user, String query) {
        if (query.isEmpty()) return true;
        boolean locked = Boolean.TRUE.equals(user.getBoolean("locked"));
        String searchable = firstValue(user, "fullName", "username") + " "
                + firstValue(user, "username") + " "
                + firstValue(user, "userCode") + " "
                + userStatus(user) + " "
                + (locked ? "đã khóa đã ẩn khóa ẩn" : "");
        return normalizeSearch(searchable).contains(query);
    }

    private String normalizeSearch(String value) {
        String text = value == null ? "" : value.trim().toLowerCase(new Locale("vi", "VN"));
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd');
    }

    private int userCodeNumber(DocumentSnapshot user) {
        String code = user.getString("userCode");
        if (code == null) return 0;
        String digits = code.replaceAll("\\D+", "");
        if (digits.isEmpty()) return 0;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String userStatus(DocumentSnapshot doc) {
        boolean locked = Boolean.TRUE.equals(doc.getBoolean("locked"));
        boolean online = Boolean.TRUE.equals(doc.getBoolean("online"));
        boolean admin = "admin".equalsIgnoreCase(doc.getString("role"));
        if (locked) return "Đã khóa";
        if (online) return admin ? "Quản trị viên · Đang hoạt động" : "Đang hoạt động";
        return admin ? "Quản trị viên · Ngoại tuyến" : offlineLabel(doc.getLong("lastSeenAt"));
    }

    private void addUserRow(final DocumentSnapshot doc, LinearLayout target) {
        LinearLayout row = createRow();
        ImageView avatar = createAvatar();
        String avatarUrl = doc.getString("avatarUrl");
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            Glide.with(this).load(avatarUrl).placeholder(R.drawable.bg_circle_avatar).error(R.drawable.bg_circle_avatar).circleCrop().into(avatar);
        }
        row.addView(avatar);
        String name = firstValue(doc, "fullName", "username");
        String userCode = firstValue(doc, "userCode");
        boolean locked = Boolean.TRUE.equals(doc.getBoolean("locked"));
        boolean online = Boolean.TRUE.equals(doc.getBoolean("online"));
        String status = userStatus(doc);
        int statusColor = locked ? RED : online ? GREEN : MUTED;
        row.addView(createTextColumn(name, userCode, status, statusColor));
        row.setBackgroundResource(R.drawable.bg_admin_card_clickable);
        row.setClickable(true);
        row.setFocusable(true);
        row.setContentDescription("Xem hồ sơ " + name + ", " + userCode + ", " + status);
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminActivity.this, ProfileActivity.class);
                intent.putExtra("userId", doc.getId());
                startActivity(intent);
            }
        });
        if (!doc.getId().equals(com.example.myapplication.service.impl.AuthService.getInstance().getCurrentUserId())) {
            row.addView(actionMenu(doc, "users", name, locked));
        }
        target.addView(row);
    }

    private void addPostRow(final DocumentSnapshot doc) {
        LinearLayout row = createRow();
        ImageView photo = new ImageView(this);
        LinearLayout.LayoutParams photoParams = new LinearLayout.LayoutParams(dp(78), dp(78));
        photoParams.setMarginEnd(dp(12));
        photo.setLayoutParams(photoParams);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photo.setBackgroundResource(R.drawable.bg_rounded_edittext);
        String imageUrl = doc.getString("imageUrl");
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Glide.with(this).load(imageUrl).centerCrop().into(photo);
        }
        row.addView(photo);
        String author = firstValue(doc, "authorUsername", "authorId");
        String caption = firstValue(doc, "caption");
        long likes = numberValue(doc, "likesCount");
        long comments = numberValue(doc, "commentsCount");
        boolean hidden = Boolean.TRUE.equals(doc.getBoolean("hidden"));
        row.addView(createTextColumn(caption, author,
                hidden ? firstValue(doc, "postCode") + " · Đã ẩn"
                        : firstValue(doc, "postCode") + " · " + likes + " lượt thích · " + comments + " bình luận",
                hidden ? MUTED : BLUE));
        row.addView(actionMenu(doc, "posts", caption, hidden));
        row.setBackgroundResource(R.drawable.bg_admin_card_clickable);
        row.setClickable(true);
        row.setFocusable(true);
        row.setContentDescription("Mở bài đăng " + firstValue(doc, "postCode"));
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { showPostManagementDialog(doc); }
        });
        content.addView(row);
    }

    private String offlineLabel(Long lastSeenAt) {
        if (lastSeenAt == null || lastSeenAt <= 0L) return "Ngoại tuyến";
        CharSequence relative = DateUtils.getRelativeTimeSpanString(lastSeenAt,
                System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE);
        return "Ngoại tuyến · hoạt động " + relative;
    }

    private void showPostManagementDialog(final DocumentSnapshot postDoc) {
        final LinearLayout dialogContent = new LinearLayout(this);
        dialogContent.setOrientation(LinearLayout.VERTICAL);
        dialogContent.setPadding(dp(16), dp(8), dp(16), dp(16));

        String postCode = firstValue(postDoc, "postCode");
        String author = firstValue(postDoc, "authorUsername", "authorId");
        dialogContent.addView(createText(postCode + " · " + author, 16, NAVY, true));

        final List<String> images = new ArrayList<>();
        Object rawImages = postDoc.get("imageUrls");
        if (rawImages instanceof List) {
            for (Object value : (List<?>) rawImages) {
                if (value instanceof String && !((String) value).trim().isEmpty()) {
                    images.add((String) value);
                }
            }
        }
        if (images.isEmpty()) {
            String imageUrl = postDoc.getString("imageUrl");
            if (imageUrl != null && !imageUrl.trim().isEmpty()) images.add(imageUrl);
        }

        PhotoCollageView collage = new PhotoCollageView(this, null);
        LinearLayout.LayoutParams collageParams = new LinearLayout.LayoutParams(-1, dp(240));
        collageParams.topMargin = dp(12);
        dialogContent.addView(collage, collageParams);
        collage.show(images, null);

        TextView caption = createText(firstValue(postDoc, "caption"), 14, NAVY, false);
        caption.setPadding(0, dp(12), 0, dp(8));
        dialogContent.addView(caption);
        dialogContent.addView(createText(numberValue(postDoc, "likesCount") + " lượt thích · "
                + numberValue(postDoc, "commentsCount") + " bình luận", 12, MUTED, false));

        final Button visibilityButton = new Button(this);
        boolean hidden = Boolean.TRUE.equals(postDoc.getBoolean("hidden"));
        final boolean[] hiddenState = { hidden };
        visibilityButton.setText(hidden ? "Hiện lại bài viết" : "Ẩn bài viết");
        visibilityButton.setAllCaps(false);
        visibilityButton.setMinHeight(dp(48));
        visibilityButton.setContentDescription(visibilityButton.getText());
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, dp(48));
        actionParams.topMargin = dp(12);
        dialogContent.addView(visibilityButton, actionParams);

        TextView commentsTitle = createText("Bình luận", 15, NAVY, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(18);
        dialogContent.addView(commentsTitle, titleParams);

        final LinearLayout commentsContainer = new LinearLayout(this);
        commentsContainer.setOrientation(LinearLayout.VERTICAL);
        dialogContent.addView(commentsContainer, new LinearLayout.LayoutParams(-1, -2));
        loadPostComments(postDoc.getId(), commentsContainer);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(dialogContent, new ScrollView.LayoutParams(-1, -2));

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Quản lý bài đăng")
                .setView(scroll)
                .setNegativeButton("Đóng", null)
                .create();

        visibilityButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                boolean nextHidden = !hiddenState[0];
                String action = nextHidden ? "Ẩn" : "Hiện lại";
                new AlertDialog.Builder(AdminActivity.this)
                        .setTitle(action + " bài viết?")
                        .setMessage(action + " " + postCode + " trên DLog?")
                        .setNegativeButton("Hủy", null)
                        .setPositiveButton(action, new android.content.DialogInterface.OnClickListener() {
                            @Override public void onClick(android.content.DialogInterface confirmDialog, int which) {
                                updateVisibility(postDoc, "posts", nextHidden)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override public void onSuccess(Void unused) {
                                            hiddenState[0] = nextHidden;
                                            visibilityButton.setText(nextHidden
                                                    ? "Hiện lại bài viết" : "Ẩn bài viết");
                                            visibilityButton.setContentDescription(visibilityButton.getText());
                                            Toast.makeText(AdminActivity.this,
                                                    nextHidden ? "Đã ẩn bài viết" : "Đã hiện lại bài viết",
                                                    Toast.LENGTH_SHORT).show();
                                            showCollection("posts", "Quản lý bài viết");
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override public void onFailure(@androidx.annotation.NonNull Exception error) {
                                                Toast.makeText(AdminActivity.this,
                                                        "Không thể cập nhật: " + error.getMessage(),
                                                        Toast.LENGTH_LONG).show();
                                            }
                                        });
                            }
                        })
                        .show();
            }
        });
        dialog.show();
    }

    private void loadPostComments(String postId, final LinearLayout container) {
        container.removeAllViews();
        TextView loading = createText("Đang tải bình luận…", 12, MUTED, false);
        loading.setPadding(0, dp(12), 0, dp(12));
        container.addView(loading);
        db.collection("comments").whereEqualTo("postId", postId).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override public void onSuccess(QuerySnapshot snapshot) {
                    container.removeAllViews();
                    if (snapshot.isEmpty()) {
                        TextView empty = createText("Bài viết chưa có bình luận.", 12, MUTED, false);
                        empty.setPadding(0, dp(12), 0, dp(12));
                        container.addView(empty);
                        return;
                    }
                    for (DocumentSnapshot comment : snapshot.getDocuments()) {
                        addDialogCommentRow(postId, comment, container);
                    }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override public void onFailure(@androidx.annotation.NonNull Exception error) {
                    container.removeAllViews();
                    container.addView(createText("Không thể tải bình luận: " + error.getMessage(),
                            12, RED, false));
                    }
                });
    }

    private void addDialogCommentRow(final String postId, final DocumentSnapshot comment,
                                     final LinearLayout container) {
        LinearLayout row = createRow();
        boolean hidden = Boolean.TRUE.equals(comment.getBoolean("hidden"));
        row.addView(createTextColumn(firstValue(comment, "username", "authorId"),
                firstValue(comment, "content"), hidden ? "Đã ẩn" : "Đang hiển thị",
                hidden ? MUTED : GREEN));

        TextView action = createText(hidden ? "Hiện" : "Ẩn", 13, hidden ? BLUE : RED, true);
        action.setGravity(Gravity.CENTER);
        action.setMinWidth(dp(56));
        action.setMinHeight(dp(48));
        action.setBackgroundResource(android.R.drawable.list_selector_background);
        action.setContentDescription((hidden ? "Hiện lại" : "Ẩn") + " bình luận");
        action.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                updateVisibility(comment, "comments", !hidden)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override public void onSuccess(Void unused) { loadPostComments(postId, container); }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override public void onFailure(@androidx.annotation.NonNull Exception error) {
                                Toast.makeText(AdminActivity.this,
                                        "Không thể cập nhật bình luận: " + error.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });
        row.addView(action);
        container.addView(row);
    }

    private TextView actionMenu(final DocumentSnapshot doc, final String collection,
                                final String name, final boolean active) {
        TextView button = createText("⋮", 24, NAVY, true);
        button.setGravity(Gravity.CENTER);
        button.setMinWidth(dp(48));
        button.setMinHeight(dp(48));
        button.setBackgroundResource(android.R.drawable.list_selector_background);
        button.setContentDescription("Tùy chọn quản trị");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu menu = new PopupMenu(AdminActivity.this, view);
                final boolean user = "users".equals(collection);
                menu.getMenu().add(user ? (active ? "Mở khóa tài khoản" : "Khóa tài khoản")
                        : (active ? "Hiện lại" : "Ẩn khỏi DLog"));
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(android.view.MenuItem item) {
                        if ("users".equals(collection)) {
                            confirmLock(doc, name, !active);
                        } else {
                            confirmVisibility(doc, collection, name, !active);
                        }
                        return true;
                    }
                });
                menu.show();
            }
        });
        return button;
    }

    private void confirmLock(final DocumentSnapshot doc, String name, final boolean locked) {
        final String verb = locked ? "Khóa" : "Mở khóa";
        new AlertDialog.Builder(this).setTitle(verb + " tài khoản?")
                .setMessage(verb + " “" + name + "” trên DLog?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton(verb, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        db.collection("users").document(doc.getId()).update("locked", locked)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        showCollection("users", "Quản lý người dùng");
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@androidx.annotation.NonNull Exception error) {
                                        Toast.makeText(AdminActivity.this, "Không thể cập nhật: " + error.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                })
                .show();
    }

    private void confirmVisibility(final DocumentSnapshot doc, final String collection,
                                   String name, final boolean hidden) {
        final String verb = hidden ? "Ẩn" : "Hiện lại";
        new AlertDialog.Builder(this)
                .setTitle(verb + ("comments".equals(collection) ? " bình luận?" : " bài viết?"))
                .setMessage(verb + " “" + name + "” " + (hidden ? "khỏi DLog?" : "trên DLog?"))
                .setNegativeButton("Hủy", null)
                .setPositiveButton(verb, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        updateVisibility(doc, collection, hidden)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Toast.makeText(AdminActivity.this, hidden ? "Đã ẩn" : "Đã hiện lại", Toast.LENGTH_SHORT).show();
                                        showCollection(collection, "comments".equals(collection)
                                                ? "Quản lý bình luận" : "Quản lý bài viết");
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@androidx.annotation.NonNull Exception error) {
                                        Toast.makeText(AdminActivity.this, "Không thể cập nhật: " + error.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                })
                .show();
    }

    private com.google.android.gms.tasks.Task<Void> updateVisibility(
            DocumentSnapshot doc, String collection, final boolean hidden) {
        final DocumentReference target = db.collection(collection).document(doc.getId());
        if (!"comments".equals(collection)) {
            return target.update("hidden", hidden);
        }
        return db.runTransaction(new com.google.firebase.firestore.Transaction.Function<Void>() {
            @Override
            public Void apply(@androidx.annotation.NonNull com.google.firebase.firestore.Transaction transaction)
                    throws com.google.firebase.firestore.FirebaseFirestoreException {
                DocumentSnapshot current = transaction.get(target);
                if (!current.exists()) {
                    return null;
                }
                boolean wasHidden = Boolean.TRUE.equals(current.getBoolean("hidden"));
                if (wasHidden == hidden) {
                    return null;
                }
                String postId = current.getString("postId");
                if (postId != null && !postId.isEmpty()) {
                    DocumentReference post = db.collection("posts").document(postId);
                    DocumentSnapshot postData = transaction.get(post);
                    if (postData.exists()) {
                        Long count = postData.getLong("commentsCount");
                        long next = Math.max(0, (count == null ? 0 : count) + (hidden ? -1 : 1));
                        transaction.update(post, "commentsCount", next);
                    }
                }
                transaction.update(target, "hidden", hidden);
                return null;
            }
        });
    }

    private void addCommentRow(DocumentSnapshot doc) {
        LinearLayout row = createRow();
        ImageView avatar = createAvatar();
        String avatarUrl = doc.getString("avatarUrl");
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            Glide.with(this).load(avatarUrl).placeholder(R.drawable.bg_circle_avatar).error(R.drawable.bg_circle_avatar).circleCrop().into(avatar);
        }
        row.addView(avatar);
        boolean hidden = Boolean.TRUE.equals(doc.getBoolean("hidden"));
        String contentText = firstValue(doc, "content");
        row.addView(createTextColumn(firstValue(doc, "username", "authorId"),
                contentText, hidden ? "Đã ẩn" : "Bình luận đang hiển thị", hidden ? MUTED : BLUE));
        row.addView(actionMenu(doc, "comments", contentText, hidden));
        content.addView(row);
    }

    private void addReportRow(DocumentSnapshot doc) {
        LinearLayout row = createRow();
        final String postId = doc.getString("postId");
        String imageUrl = doc.getString("imageUrl");
        if ("image".equals(doc.getString("type")) && imageUrl != null && !imageUrl.isEmpty()) {
            ImageView image = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(70), dp(70));
            params.setMarginEnd(dp(12));
            image.setLayoutParams(params);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setContentDescription("Ảnh bị báo cáo");
            Glide.with(this).load(imageUrl).centerCrop().into(image);
            row.addView(image);
        }
        Long imageIndex = doc.getLong("imageIndex");
        String title = "image".equals(doc.getString("type"))
                ? "Ảnh " + (imageIndex == null ? 1 : imageIndex + 1) + " bị báo cáo"
                : firstValue(doc, "title", "type", "reason");
        String details = "image".equals(doc.getString("type"))
                ? firstValue(doc, "reason") + " · Người báo cáo: " + firstValue(doc, "reporterId")
                : firstValue(doc, "description", "message");
        row.addView(createTextColumn(title, details,
                postId == null ? "Mã: " + doc.getId() : "Nội dung: " + postId, RED));
        if (postId != null && !postId.isEmpty()) {
            row.setContentDescription("Nội dung có ảnh bị báo cáo: " + postId);
        }
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

        addSetting("Thông tin ứng dụng", "Phiên bản 1.0.0");
        addSetting("Quản lý nội dung", "Quyền truy cập do Firestore Rules kiểm soát");
        addSetting("Sao lưu dữ liệu", "Backup & Restore");
        addSetting("Ngôn ngữ", "Tiếng Việt");
    }

    private void addSetting(String title, String subtitle) {
        LinearLayout row = createRow();
        row.addView(createTextColumn(title, subtitle, "›", BLUE));
        content.addView(row);
    }

    private void addHeading(String text, String subtitle) {
        // Đã vô hiệu hóa để ẩn hoàn toàn khung tiêu đề khỏi các tab
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

    private void addWeeklyChart() {
        addSectionTitle("Hoạt động 7 ngày", "Chạm vào biểu đồ để xem số liệu");
        final LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(14), dp(12), dp(10));
        card.setBackgroundResource(R.drawable.bg_admin_card);
        card.setElevation(dp(2));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.bottomMargin = dp(18);
        content.addView(card, cardParams);

        LinearLayout legend = new LinearLayout(this);
        legend.setOrientation(LinearLayout.HORIZONTAL);
        legend.addView(createText("● Người dùng  ", 12, BLUE, true));
        legend.addView(createText("● Bài viết  ", 12, GREEN, true));
        legend.addView(createText("● Tương tác", 12,
                ContextCompat.getColor(this, R.color.dlog_chart_interaction), true));
        card.addView(legend);

        final WeeklyTrendView chart = new WeeklyTrendView(this);
        card.addView(chart, new LinearLayout.LayoutParams(-1, dp(210)));

        final TextView summary = createText("Đang tải thống kê…", 12, MUTED, false);
        card.addView(summary);

        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        start.add(Calendar.DAY_OF_YEAR, -6);
        final long firstDay = start.getTimeInMillis();

        final int[] weeklyUsers = new int[7];
        final int[] weeklyPosts = new int[7];
        final int[] weeklyInteractions = new int[7];

        db.collection("users").whereGreaterThanOrEqualTo("createdAt", firstDay).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot snapshot) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            int day = dayIndex(doc.getLong("createdAt"), firstDay);
                            if (day >= 0) weeklyUsers[day]++;
                        }
                        if (card.getParent() != null) {
                            chart.setData(weeklyUsers, weeklyPosts, weeklyInteractions);
                            updateWeeklySummary(summary, weeklyUsers, weeklyPosts, weeklyInteractions);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@androidx.annotation.NonNull Exception error) {
                        summary.setText("Không thể tải số liệu người dùng.");
                    }
                });

        db.collection("posts").whereGreaterThanOrEqualTo("createdAt", firstDay).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot snapshot) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            int day = dayIndex(doc.getLong("createdAt"), firstDay);
                            if (day >= 0) weeklyPosts[day]++;
                        }
                        if (card.getParent() != null) {
                            chart.setData(weeklyUsers, weeklyPosts, weeklyInteractions);
                            updateWeeklySummary(summary, weeklyUsers, weeklyPosts, weeklyInteractions);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@androidx.annotation.NonNull Exception error) {
                        summary.setText("Không thể tải số liệu bài viết.");
                    }
                });

        loadWeeklyInteractions("likes", firstDay, weeklyUsers, weeklyPosts,
                weeklyInteractions, chart, card, summary);
        loadWeeklyInteractions("comments", firstDay, weeklyUsers, weeklyPosts,
                weeklyInteractions, chart, card, summary);
    }

    private void loadWeeklyInteractions(String collection, final long firstDay, final int[] users,
                                        final int[] posts, final int[] interactions, final WeeklyTrendView chart,
                                        final LinearLayout card, final TextView summary) {
        db.collection(collection).whereGreaterThanOrEqualTo("createdAt", firstDay).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot snapshot) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            int day = dayIndex(doc.getLong("createdAt"), firstDay);
                            if (day >= 0) interactions[day]++;
                        }
                        if (card.getParent() != null) {
                            chart.setData(users, posts, interactions);
                            updateWeeklySummary(summary, users, posts, interactions);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@androidx.annotation.NonNull Exception error) {
                        summary.setText("Không thể tải đủ số liệu tương tác. Chạm để thử lại sau.");
                    }
                });
    }

    private int dayIndex(Long timestamp, long firstDay) {
        if (timestamp == null || timestamp < firstDay) return -1;
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(timestamp);
        Calendar first = Calendar.getInstance();
        first.setTimeInMillis(firstDay);
        int days = date.get(Calendar.DAY_OF_YEAR) - first.get(Calendar.DAY_OF_YEAR);
        if (date.get(Calendar.YEAR) != first.get(Calendar.YEAR)) {
            days += first.getActualMaximum(Calendar.DAY_OF_YEAR);
        }
        return days >= 0 && days < 7 ? days : -1;
    }

    private void updateWeeklySummary(TextView summary, int[] users, int[] posts,
                                     int[] interactions) {
        int userTotal = 0;
        int postTotal = 0;
        int interactionTotal = 0;
        for (int i = 0; i < 7; i++) {
            userTotal += users[i];
            postTotal += posts[i];
            interactionTotal += interactions[i];
        }
        summary.setText(String.format(Locale.getDefault(),
                "7 ngày qua: %d người dùng · %d bài viết · %d tương tác",
                userTotal, postTotal, interactionTotal));
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
        view.setTypeface(bold ? interBold : interRegular);
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
