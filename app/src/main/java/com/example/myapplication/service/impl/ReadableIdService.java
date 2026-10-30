package com.example.myapplication.service.impl;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Allocates short, human-readable management codes while technical Firestore IDs stay stable. */
public final class ReadableIdService {

    public interface Callback {
        void onSuccess(String code);
        void onError(Exception error);
    }

    private ReadableIdService() { }

    public static void nextUserCode(Callback callback) {
        next("users", "USR", callback);
    }

    public static void nextPostCode(Callback callback) {
        next("posts", "POST", callback);
    }

    private static void next(String counterName, String prefix, Callback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference counter = db.collection("counters").document(counterName);
        db.runTransaction(transaction -> {
            Long lastValue = transaction.get(counter).getLong("lastValue");
            long nextValue = (lastValue == null ? 0L : lastValue) + 1L;
            Map<String, Object> values = new HashMap<>();
            values.put("lastValue", nextValue);
            values.put("description", "Bộ đếm mã quản lý " + prefix);
            transaction.set(counter, values, SetOptions.merge());
            return String.format(Locale.US, "%s-%03d", prefix, nextValue);
        }).addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(callback::onError);
    }
}
