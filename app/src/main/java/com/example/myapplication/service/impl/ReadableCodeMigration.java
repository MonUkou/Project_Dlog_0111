package com.example.myapplication.service.impl;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** One-time, idempotent migration that gives existing users and posts readable codes. */
public final class ReadableCodeMigration {

    public interface Callback {
        void onSuccess(int users, int posts);
        void onError(Exception error);
    }

    private ReadableCodeMigration() { }

    public static void run(FirebaseFirestore db, Callback callback) {
        db.collection("users").get()
                .addOnSuccessListener(users -> db.collection("posts").get()
                        .addOnSuccessListener(posts -> migrate(db, users, posts, callback))
                        .addOnFailureListener(callback::onError))
                .addOnFailureListener(callback::onError);
    }

    private static void migrate(FirebaseFirestore db, QuerySnapshot usersSnapshot,
                                QuerySnapshot postsSnapshot, Callback callback) {
        List<DocumentSnapshot> users = sorted(usersSnapshot.getDocuments());
        List<DocumentSnapshot> posts = sorted(postsSnapshot.getDocuments());
        if (users.size() + posts.size() + 2 > 500) {
            callback.onError(new IllegalStateException("Dữ liệu vượt giới hạn migration 498 bản ghi."));
            return;
        }

        WriteBatch batch = db.batch();
        for (int index = 0; index < users.size(); index++) {
            String code = String.format(Locale.US, "USR-%03d", index + 1);
            if (!code.equals(users.get(index).getString("userCode"))) {
                batch.update(users.get(index).getReference(), "userCode", code);
            }
        }
        for (int index = 0; index < posts.size(); index++) {
            String code = String.format(Locale.US, "POST-%03d", index + 1);
            if (!code.equals(posts.get(index).getString("postCode"))) {
                batch.update(posts.get(index).getReference(), "postCode", code);
            }
        }

        Map<String, Object> userCounter = new HashMap<>();
        userCounter.put("lastValue", (long) users.size());
        userCounter.put("description", "Bộ đếm mã quản lý USR");
        batch.set(db.collection("counters").document("users"), userCounter, SetOptions.merge());

        Map<String, Object> postCounter = new HashMap<>();
        postCounter.put("lastValue", (long) posts.size());
        postCounter.put("description", "Bộ đếm mã quản lý POST");
        batch.set(db.collection("counters").document("posts"), postCounter, SetOptions.merge());

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(users.size(), posts.size()))
                .addOnFailureListener(callback::onError);
    }

    private static List<DocumentSnapshot> sorted(List<DocumentSnapshot> source) {
        List<DocumentSnapshot> result = new ArrayList<>(source);
        Collections.sort(result, new Comparator<DocumentSnapshot>() {
            @Override public int compare(DocumentSnapshot left, DocumentSnapshot right) {
                Long leftTime = left.getLong("createdAt");
                Long rightTime = right.getLong("createdAt");
                int timeComparison = Long.compare(leftTime == null ? 0L : leftTime,
                        rightTime == null ? 0L : rightTime);
                return timeComparison != 0 ? timeComparison : left.getId().compareTo(right.getId());
            }
        });
        return result;
    }
}
