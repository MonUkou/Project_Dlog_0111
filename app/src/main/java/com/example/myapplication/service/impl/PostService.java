package com.example.myapplication.service.impl;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;

import com.example.myapplication.model.Post;
import com.example.myapplication.model.SavedPost;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PostService {
    private static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public static class FeedResult {
        private final List<Post> posts;
        private final DocumentSnapshot lastDocument;
        private final boolean hasMore;

        public FeedResult(List<Post> posts, DocumentSnapshot lastDocument, boolean hasMore) {
            this.posts = posts;
            this.lastDocument = lastDocument;
            this.hasMore = hasMore;
        }

        public List<Post> getPosts() { return posts; }
        public DocumentSnapshot getLastDocument() { return lastDocument; }
        public boolean hasMore() { return hasMore; }
    }

    private static PostService instance;
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;

    public PostService() {
        this.db = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
    }

    public static synchronized PostService getInstance() {
        if (instance == null) {
            instance = new PostService();
        }
        return instance;
    }

    // F15: Đăng bài viết
    public void createPost(String authorId, String caption, Uri imageUri, Context context, Callback<Post> callback) {
        if (authorId == null || authorId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Tác giả không hợp lệ."));
            return;
        }
        if (imageUri == null) {
            callback.onError(new IllegalArgumentException("Vui lòng chọn hình ảnh."));
            return;
        }

        Context appContext = context.getApplicationContext();
        String postId = db.collection("posts").document().getId();

        // Drive and other cloud providers expose virtual content:// URIs. Copy
        // them through ContentResolver first so Storage uploads a stable local file.
        IO_EXECUTOR.execute(() -> {
            File stagedImage;
            String mimeType;
            try {
                mimeType = resolveImageMimeType(appContext, imageUri);
                stagedImage = copyImageToCache(appContext, imageUri, postId, mimeType);
            } catch (Exception error) {
                String detail = error.getMessage() == null ? "không đọc được dữ liệu ảnh." : error.getMessage();
                MAIN_HANDLER.post(() -> callback.onError(new Exception(
                        "Không đọc được ảnh đã chọn (Google Drive/thư viện): " + detail, error)));
                return;
            }

            String bucket = appContext.getString(com.example.myapplication.R.string.google_storage_bucket);
            String extension = extensionForMimeType(mimeType);
            StorageReference imageRef = FirebaseStorage.getInstance("gs://" + bucket)
                    .getReference().child("posts/" + authorId + "/" + postId + extension);
            StorageMetadata metadata = new StorageMetadata.Builder()
                    .setContentType(mimeType == null || !mimeType.startsWith("image/") ? "image/jpeg" : mimeType)
                    .setCustomMetadata("postId", postId)
                    .setCustomMetadata("authorId", authorId)
                    .build();

            imageRef.putFile(Uri.fromFile(stagedImage), metadata)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            Exception cause = task.getException();
                            if (cause != null) throw cause;
                            throw new IllegalStateException("Firebase Storage chưa hoàn tất tải ảnh.");
                        }
                        return imageRef.getDownloadUrl();
                    })
                    .addOnSuccessListener(downloadUri -> {
                        stagedImage.delete();
                        savePostToFirestore(postId, authorId, caption, downloadUri.toString(), callback);
                    })
                    .addOnFailureListener(error -> {
                        stagedImage.delete();
                        callback.onError(new Exception(storageErrorMessage(error), error));
                    });
        });
    }

    private File copyImageToCache(Context context, Uri imageUri, String postId, String mimeType) throws Exception {
        File cacheDirectory = new File(context.getCacheDir(), "post_uploads");
        if (!cacheDirectory.exists() && !cacheDirectory.mkdirs()) {
            throw new java.io.IOException("Không thể tạo vùng đệm tạm để tải ảnh.");
        }
        File stagedImage = new File(cacheDirectory, postId + "_" + UUID.randomUUID() + extensionForMimeType(mimeType));
        try (InputStream input = context.getContentResolver().openInputStream(imageUri);
             OutputStream output = new FileOutputStream(stagedImage)) {
            if (input == null) throw new java.io.IOException("Nguồn ảnh không mở được. Hãy tải ảnh từ Drive xuống máy rồi chọn lại.");
            byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            output.flush();
        } catch (Exception error) {
            // Remove a partial file before returning the provider/read error.
            //noinspection ResultOfMethodCallIgnored
            stagedImage.delete();
            throw error;
        }
        if (!stagedImage.isFile() || stagedImage.length() == 0) {
            //noinspection ResultOfMethodCallIgnored
            stagedImage.delete();
            throw new java.io.IOException("Ảnh tải từ Drive bị rỗng. Hãy tải ảnh xuống máy trước rồi chọn lại.");
        }
        return stagedImage;
    }

    private String extensionForMimeType(String mimeType) {
        String subtype = mimeType == null || mimeType.lastIndexOf('/') < 0
                ? "" : mimeType.substring(mimeType.lastIndexOf('/') + 1).toLowerCase(java.util.Locale.ROOT);
        if ("png".equals(subtype)) return ".png";
        if ("webp".equals(subtype)) return ".webp";
        if ("gif".equals(subtype)) return ".gif";
        if ("heic".equals(subtype) || "heif".equals(subtype)) return "." + subtype;
        return ".jpg";
    }

    private String resolveImageMimeType(Context context, Uri imageUri) {
        String mimeType = context.getContentResolver().getType(imageUri);
        if (mimeType != null && mimeType.startsWith("image/")) return mimeType;
        String displayName = null;
        try (Cursor cursor = context.getContentResolver().query(
                imageUri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (column >= 0) displayName = cursor.getString(column);
            }
        } catch (Exception ignored) { }
        if (displayName != null) {
            String name = displayName.toLowerCase(java.util.Locale.ROOT);
            if (name.endsWith(".png")) return "image/png";
            if (name.endsWith(".webp")) return "image/webp";
            if (name.endsWith(".gif")) return "image/gif";
            if (name.endsWith(".heic")) return "image/heic";
            if (name.endsWith(".heif")) return "image/heif";
        }
        return "image/jpeg";
    }

    private String storageErrorMessage(Exception error) {
        if (error instanceof StorageException) {
            int code = ((StorageException) error).getErrorCode();
            if (code == StorageException.ERROR_OBJECT_NOT_FOUND) {
                return "Firebase báo không tìm thấy ảnh vừa tải lên. Hãy kiểm tra Storage của project và bucket project-dlog-0111.firebasestorage.app.";
            }
            if (code == StorageException.ERROR_BUCKET_NOT_FOUND) {
                return "Firebase Storage chưa có bucket project-dlog-0111.firebasestorage.app. Hãy tạo/bật Storage trong Firebase Console.";
            }
            if (code == StorageException.ERROR_NOT_AUTHORIZED || code == StorageException.ERROR_NOT_AUTHENTICATED) {
                return "Firebase Storage từ chối tải ảnh. Kiểm tra người dùng đã đăng nhập và Storage Rules cho phép tải ảnh.";
            }
        }
        String message = error == null ? null : error.getMessage();
        return "Không thể tải ảnh lên Firebase Storage" + (message == null ? "." : ": " + message);
    }

    private void savePostToFirestore(String postId, String authorId, String caption, String imageUrl, Callback<Post> callback) {
        Post post = new Post();
        post.setId(postId);
        post.setAuthorId(authorId);
        post.setCaption(caption != null ? caption.trim() : "");
        post.setImageUrl(imageUrl);
        post.setLikesCount(0);
        post.setCommentsCount(0);

        // Fetch author's username and avatar to cache in post if available
        db.collection("users").document(authorId).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String username = userDoc.getString("username");
                        String avatarUrl = userDoc.getString("avatarUrl");
                        if (username != null) post.setAuthorUsername(username);
                        if (avatarUrl != null) post.setAuthorAvatarUrl(avatarUrl);
                    } else {
                        post.setAuthorUsername(authorId);
                    }
                    savePostToFirestoreInternal(post, callback);
                })
                .addOnFailureListener(e -> {
                    post.setAuthorUsername(authorId);
                    savePostToFirestoreInternal(post, callback);
                });
    }

    private void savePostToFirestoreInternal(Post post, Callback<Post> callback) {
        if (!post.validate()) {
            callback.onError(new IllegalArgumentException("Dữ liệu bài viết không hợp lệ."));
            return;
        }

        db.collection("posts").document(post.getId())
                .set(post.toMap())
                .addOnSuccessListener(aVoid -> {
                    db.collection("users").document(post.getAuthorId())
                            .update("postsCount", FieldValue.increment(1));
                    callback.onSuccess(post);
                })
                .addOnFailureListener(callback::onError);
    }

    // F16: Sửa bài viết (Chỉ tác giả)
    public void updatePost(String currentUserId, String postId, String newCaption, Callback<Post> callback) {
        if (currentUserId == null || postId == null) {
            callback.onError(new IllegalArgumentException("Tham số không hợp lệ."));
            return;
        }

        getPostDetail(postId, new Callback<Post>() {
            @Override
            public void onSuccess(Post post) {
                if (!currentUserId.equals(post.getAuthorId())) {
                    callback.onError(new SecurityException("Bạn không có quyền chỉnh sửa bài viết này."));
                    return;
                }

                post.setCaption(newCaption != null ? newCaption.trim() : "");
                post.touch();

                db.collection("posts").document(postId)
                        .update("caption", post.getCaption(), "updatedAt", post.getUpdatedAt())
                        .addOnSuccessListener(aVoid -> callback.onSuccess(post))
                        .addOnFailureListener(callback::onError);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    // F17: Xóa bài viết (Chỉ tác giả)
    public void deletePost(String currentUserId, String postId, Callback<Void> callback) {
        if (currentUserId == null || postId == null) {
            callback.onError(new IllegalArgumentException("Tham số không hợp lệ."));
            return;
        }

        getPostDetail(postId, new Callback<Post>() {
            @Override
            public void onSuccess(Post post) {
                if (!currentUserId.equals(post.getAuthorId())) {
                    callback.onError(new SecurityException("Bạn không có quyền xóa bài viết này."));
                    return;
                }

                db.collection("posts").document(postId)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            db.collection("users").document(post.getAuthorId())
                                    .update("postsCount", FieldValue.increment(-1));

                            if (post.getImageUrl() != null && post.getImageUrl().contains("firebasestorage")) {
                                try {
                                    storage.getReferenceFromUrl(post.getImageUrl()).delete();
                                } catch (Exception ignored) {}
                            }

                            callback.onSuccess(null);
                        })
                        .addOnFailureListener(callback::onError);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    // F18: Xem chi tiết bài viết
    public void getPostDetail(String postId, Callback<Post> callback) {
        if (postId == null || postId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("ID bài viết không hợp lệ."));
            return;
        }

        db.collection("posts").document(postId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.getData() != null) {
                        Post post = Post.fromMap(documentSnapshot.getData());
                        callback.onSuccess(post);
                    } else {
                        callback.onError(new Exception("Bài viết không tồn tại."));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    // F19: Xem bảng tin (Thứ tự thời gian, giới hạn kết quả & tải thêm)
    public void getFeedPosts(DocumentSnapshot lastVisible, int limit, Callback<FeedResult> callback) {
        Query query = db.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit);

        if (lastVisible != null) {
            query = query.startAfter(lastVisible);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Post> posts = new ArrayList<>();
                    DocumentSnapshot newLastDoc = null;

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        if (doc.getData() != null) {
                            Post post = Post.fromMap(doc.getData());
                            if (post != null) {
                                posts.add(post);
                            }
                        }
                    }

                    if (!queryDocumentSnapshots.isEmpty()) {
                        newLastDoc = queryDocumentSnapshots.getDocuments()
                                .get(queryDocumentSnapshots.size() - 1);
                    }

                    boolean hasMore = queryDocumentSnapshots.size() >= limit;
                    callback.onSuccess(new FeedResult(posts, newLastDoc, hasMore));
                })
                .addOnFailureListener(callback::onError);
    }

    // F20: Xem bài viết của một người dùng
    public void getPostsByUserId(String userId, Callback<List<Post>> callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("ID người dùng không hợp lệ."));
            return;
        }

        db.collection("posts")
                .whereEqualTo("authorId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Post> posts = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        if (doc.getData() != null) {
                            Post post = Post.fromMap(doc.getData());
                            if (post != null) {
                                posts.add(post);
                            }
                        }
                    }
                    callback.onSuccess(posts);
                })
                .addOnFailureListener(e -> {
                    db.collection("posts")
                            .whereEqualTo("authorId", userId)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots2 -> {
                                List<Post> posts = new ArrayList<>();
                                for (DocumentSnapshot doc : queryDocumentSnapshots2.getDocuments()) {
                                    if (doc.getData() != null) {
                                        Post post = Post.fromMap(doc.getData());
                                        if (post != null) posts.add(post);
                                    }
                                }
                                posts.sort((p1, p2) -> Long.compare(p2.getCreatedAt(), p1.getCreatedAt()));
                                callback.onSuccess(posts);
                            })
                            .addOnFailureListener(callback::onError);
                });
    }

    // F21: Lưu bài viết (Ngăn lưu trùng)
    public void savePost(String userId, String postId, Callback<SavedPost> callback) {
        if (userId == null || postId == null) {
            callback.onError(new IllegalArgumentException("Tham số không hợp lệ."));
            return;
        }

        String docId = userId + "_" + postId;

        db.collection("saved_posts").document(docId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.getData() != null) {
                        SavedPost existing = SavedPost.fromMap(doc.getData());
                        callback.onSuccess(existing);
                        return;
                    }

                    SavedPost savedPost = new SavedPost(userId, postId);
                    savedPost.setId(docId);

                    if (!savedPost.validate()) {
                        callback.onError(new IllegalArgumentException("Dữ liệu lưu bài viết không hợp lệ."));
                        return;
                    }

                    db.collection("saved_posts").document(docId)
                            .set(savedPost.toMap())
                            .addOnSuccessListener(aVoid -> callback.onSuccess(savedPost))
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    // F22: Bỏ lưu bài viết
    public void unsavePost(String userId, String postId, Callback<Void> callback) {
        if (userId == null || postId == null) {
            callback.onError(new IllegalArgumentException("Tham số không hợp lệ."));
            return;
        }

        String docId = userId + "_" + postId;
        db.collection("saved_posts").document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    // Kiểm tra bài viết đã lưu chưa
    public void isPostSaved(String userId, String postId, Callback<Boolean> callback) {
        if (userId == null || postId == null) {
            callback.onSuccess(false);
            return;
        }

        String docId = userId + "_" + postId;
        db.collection("saved_posts").document(docId).get()
                .addOnSuccessListener(doc -> callback.onSuccess(doc.exists()))
                .addOnFailureListener(e -> callback.onSuccess(false));
    }

    // F23: Danh sách bài viết đã lưu
    public void getSavedPosts(String userId, Callback<List<Post>> callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("ID người dùng không hợp lệ."));
            return;
        }

        db.collection("saved_posts")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> postIds = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String pId = doc.getString("postId");
                        if (pId != null) postIds.add(pId);
                    }

                    if (postIds.isEmpty()) {
                        callback.onSuccess(new ArrayList<>());
                        return;
                    }

                    fetchPostsByIds(postIds, callback);
                })
                .addOnFailureListener(callback::onError);
    }

    private void fetchPostsByIds(List<String> postIds, Callback<List<Post>> callback) {
        List<Post> posts = new ArrayList<>();
        final int total = postIds.size();

        for (String pid : postIds) {
            getPostDetail(pid, new Callback<Post>() {
                @Override
                public void onSuccess(Post post) {
                    synchronized (posts) {
                        posts.add(post);
                        if (posts.size() == total) {
                            posts.sort((p1, p2) -> Long.compare(p2.getCreatedAt(), p1.getCreatedAt()));
                            callback.onSuccess(posts);
                        }
                    }
                }

                @Override
                public void onError(Exception e) {
                    synchronized (posts) {
                        if (posts.size() == total) {
                            callback.onSuccess(posts);
                        }
                    }
                }
            });
        }
    }

    // F24: Lưu ảnh về thiết bị
    public void saveImageToDevice(Context context, String imageUrl, Callback<String> callback) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Đường dẫn ảnh không hợp lệ."));
            return;
        }

        IO_EXECUTOR.execute(() -> {
            try {
                Bitmap bitmap;
                if (imageUrl.startsWith("content://") || imageUrl.startsWith("file://")) {
                    InputStream is = context.getContentResolver().openInputStream(Uri.parse(imageUrl));
                    bitmap = BitmapFactory.decodeStream(is);
                    if (is != null) is.close();
                } else {
                    URL url = new URL(imageUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    bitmap = BitmapFactory.decodeStream(input);
                    input.close();
                }

                if (bitmap == null) {
                    callback.onError(new Exception("Không thể tải hình ảnh."));
                    return;
                }

                String filename = "DLog_" + System.currentTimeMillis() + ".jpg";

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/DLog");

                    Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    if (uri != null) {
                        OutputStream out = context.getContentResolver().openOutputStream(uri);
                        if (out != null) {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                            out.close();
                        }
                        callback.onSuccess("Đã lưu ảnh vào Thư viện (" + filename + ")");
                    } else {
                        callback.onError(new Exception("Không thể lưu ảnh vào thiết bị."));
                    }
                } else {
                    File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                    File dlogDir = new File(picturesDir, "DLog");
                    if (!dlogDir.exists()) dlogDir.mkdirs();
                    File file = new File(dlogDir, filename);
                    FileOutputStream fos = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.close();
                    callback.onSuccess("Đã lưu ảnh vào " + file.getAbsolutePath());
                }
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
}
