package com.example.myapplication.service.impl;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import androidx.annotation.NonNull;

import com.example.myapplication.model.Post;
import com.example.myapplication.model.SavedPost;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
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

    /** Stage provider/resource URIs before uploading profile images. */
    public void uploadProfileImage(String uid, String kind, Uri uri, Context context, Callback<String> callback) {
        if (uid == null || uid.isEmpty() || !("avatars".equals(kind) || "covers".equals(kind))) {
            callback.onError(new IllegalArgumentException("Vui lòng đăng nhập lại.")); return;
        }
        final Context app = context.getApplicationContext();
        IO_EXECUTOR.execute(new Runnable() {
            @Override public void run() {
            File file = null;
            try {
                String mime = resolveImageMimeType(app, uri);
                file = copyImageToCache(app, uri, UUID.randomUUID().toString(), mime);
                final String url = CloudinaryUploader.upload(app, file, mime);
                MAIN_HANDLER.post(new Runnable() {
                    @Override public void run() { callback.onSuccess(url); }
                });
            } catch (final Exception error) {
                MAIN_HANDLER.post(new Runnable() {
                    @Override public void run() { callback.onError(error); }
                });
            } finally {
                if (file != null) file.delete();
            }
            }
        });
    }

    // F15: Đăng bài viết với một hoặc nhiều ảnh.
    public void createPost(final String authorId, final String caption, final List<Uri> imageUris,
                           final List<String> hashtags, final Context context, final Callback<Post> callback) {
        if (authorId == null || authorId.trim().isEmpty() || imageUris == null || imageUris.isEmpty()
                || imageUris.size() > 10) {
            callback.onError(new IllegalArgumentException("Chọn từ 1 đến 10 ảnh để đăng."));
            return;
        }
        final String postId = db.collection("posts").document().getId();
        final Context app = context.getApplicationContext();
        final String colorTone = detectColorTone(app, imageUris.get(0));
        IO_EXECUTOR.execute(new Runnable() {
            @Override public void run() {
                final List<String> urls = new ArrayList<>();
                final List<Map<String, String>> metadata = new ArrayList<>();
                try {
                    for (int index = 0; index < imageUris.size(); index++) {
                        Uri uri = imageUris.get(index);
                        metadata.add(readExifWithoutLocation(app, uri));
                        String mime = resolveImageMimeType(app, uri);
                        try {
                            File file = copyImageToCache(app, uri, postId, mime);
                            try { urls.add(CloudinaryUploader.upload(app, file, mime)); }
                            finally { file.delete(); }
                        } catch (Exception imageError) {
                            throw new java.io.IOException("Ảnh " + (index + 1) + "/" + imageUris.size()
                                    + ": " + imageError.getMessage(), imageError);
                        }
                    }
                    MAIN_HANDLER.post(new Runnable() {
                        @Override public void run() {
                            savePostToFirestore(postId, authorId, caption, urls.get(0), urls,
                                    hashtags, metadata, colorTone, callback);
                        }
                    });
                } catch (final Exception error) {
                    MAIN_HANDLER.post(new Runnable() {
                        @Override public void run() { callback.onError(error); }
                    });
                }
            }
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
            long total = 0;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > 20L * 1024 * 1024) throw new java.io.IOException("Ảnh vượt quá 20 MB. Vui lòng chọn ảnh nhỏ hơn.");
                output.write(buffer, 0, count);
            }
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
        optimizeImage(stagedImage, mimeType);
        return stagedImage;
    }

    /** Resizes large photos before upload and compresses locally to reduce bandwidth. */
    private void optimizeImage(File source, String mimeType) throws Exception {
        if (source == null || !source.isFile() || "image/gif".equals(mimeType)) return;
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(source.getAbsolutePath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return;

        int sample = 1;
        while (bounds.outWidth / sample > 2560 || bounds.outHeight / sample > 2560) sample *= 2;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap decoded = BitmapFactory.decodeFile(source.getAbsolutePath(), options);
        if (decoded == null) return;

        Bitmap working = decoded;
        int longest = Math.max(decoded.getWidth(), decoded.getHeight());
        if (longest > 2048) {
            float ratio = 2048f / longest;
            working = Bitmap.createScaledBitmap(decoded,
                    Math.max(1, Math.round(decoded.getWidth() * ratio)),
                    Math.max(1, Math.round(decoded.getHeight() * ratio)), true);
            if (working != decoded) decoded.recycle();
        }

        if ("image/jpeg".equals(mimeType)) {
            try {
                android.media.ExifInterface exif = new android.media.ExifInterface(source.getAbsolutePath());
                int orientation = exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION,
                        android.media.ExifInterface.ORIENTATION_NORMAL);
                float degrees = orientation == android.media.ExifInterface.ORIENTATION_ROTATE_90 ? 90f
                        : orientation == android.media.ExifInterface.ORIENTATION_ROTATE_180 ? 180f
                        : orientation == android.media.ExifInterface.ORIENTATION_ROTATE_270 ? 270f : 0f;
                if (degrees != 0f) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(degrees);
                    Bitmap rotated = Bitmap.createBitmap(working, 0, 0,
                            working.getWidth(), working.getHeight(), matrix, true);
                    if (rotated != working) working.recycle();
                    working = rotated;
                }
            } catch (Exception ignored) { }
        }

        File optimized = new File(source.getParentFile(), source.getName() + ".optimized");
        Bitmap.CompressFormat format = "image/png".equals(mimeType) ? Bitmap.CompressFormat.PNG
                : (Build.VERSION.SDK_INT >= 30 && "image/webp".equals(mimeType)
                ? Bitmap.CompressFormat.WEBP_LOSSY : Bitmap.CompressFormat.JPEG);
        int quality = format == Bitmap.CompressFormat.PNG ? 100 : 84;
        try (FileOutputStream output = new FileOutputStream(optimized)) {
            if (!working.compress(format, quality, output)) {
                throw new java.io.IOException("Không thể tối ưu ảnh trước khi tải lên.");
            }
        } finally {
            working.recycle();
        }
        if (optimized.length() > 0 && (!source.delete() || !optimized.renameTo(source))) {
            try (InputStream input = new java.io.FileInputStream(optimized);
                 OutputStream output = new FileOutputStream(source)) {
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            }
            optimized.delete();
        }
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

    /** Extracts camera settings only; location is deliberately never uploaded. */
    private Map<String, String> readExifWithoutLocation(Context context, Uri uri) {
        Map<String, String> values = new HashMap<>();
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input == null) return values;
            android.media.ExifInterface exif = new android.media.ExifInterface(input);
            putExif(values, "Thiết bị", exif.getAttribute(android.media.ExifInterface.TAG_MODEL));
            putExif(values, "Hãng", exif.getAttribute(android.media.ExifInterface.TAG_MAKE));
            putExif(values, "ISO", exif.getAttribute(android.media.ExifInterface.TAG_ISO_SPEED_RATINGS));
            putExif(values, "Khẩu độ", exif.getAttribute(android.media.ExifInterface.TAG_F_NUMBER));
            putExif(values, "Tiêu cự", exif.getAttribute(android.media.ExifInterface.TAG_FOCAL_LENGTH));
            putExif(values, "Thời gian chụp", exif.getAttribute(android.media.ExifInterface.TAG_DATETIME_ORIGINAL));
        } catch (Exception ignored) { }
        return values;
    }

    private void putExif(Map<String, String> values, String label, String value) {
        if (value != null && !value.trim().isEmpty()) values.put(label, value.trim());
    }

    private String detectColorTone(Context context, Uri uri) {
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 16;
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
            if (bitmap == null) return "trung_tinh";
            long red = 0, green = 0, blue = 0;
            int stepX = Math.max(1, bitmap.getWidth() / 24);
            int stepY = Math.max(1, bitmap.getHeight() / 24);
            int count = 0;
            for (int y = 0; y < bitmap.getHeight(); y += stepY) {
                for (int x = 0; x < bitmap.getWidth(); x += stepX) {
                    int color = bitmap.getPixel(x, y);
                    red += android.graphics.Color.red(color);
                    green += android.graphics.Color.green(color);
                    blue += android.graphics.Color.blue(color);
                    count++;
                }
            }
            bitmap.recycle();
            float[] hsv = new float[3];
            android.graphics.Color.RGBToHSV((int) (red / count), (int) (green / count), (int) (blue / count), hsv);
            if (hsv[1] < 0.16f) return "don_sac";
            if (hsv[1] > 0.58f) return "ruc_ro";
            if (hsv[0] < 75f || hsv[0] > 330f) return "am";
            if (hsv[0] >= 150f && hsv[0] <= 270f) return "lanh";
        } catch (Exception ignored) { }
        return "trung_tinh";
    }

    private void savePostToFirestore(String postId, String authorId, String caption, String imageUrl,
                                     List<String> imageUrls, List<String> hashtags,
                                     List<Map<String, String>> photoMetadata, String colorTone, Callback<Post> callback) {
        Post post = new Post();
        post.setId(postId);
        post.setAuthorId(authorId);
        post.setCaption(caption != null ? caption.trim() : "");
        post.setImageUrl(imageUrl);
        post.setImageUrls(imageUrls);
        post.setHashtags(hashtags);
        post.setPhotoMetadata(photoMetadata);
        post.setColorTone(colorTone);
        post.setLikesCount(0);
        post.setCommentsCount(0);

        ReadableIdService.nextPostCode(new ReadableIdService.Callback() {
            @Override public void onSuccess(String code) {
                post.setPostCode(code);
                post.setId(code);
                enrichAndSavePost(post, callback);
            }

            @Override public void onError(Exception error) {
                callback.onError(error);
            }
        });
    }

    private void enrichAndSavePost(Post post, Callback<Post> callback) {
        // Fetch author's username and avatar to cache in post if available
        db.collection("users").document(post.getAuthorId()).get()
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot>() {
                    @Override public void onSuccess(DocumentSnapshot userDoc) {
                        if (userDoc.exists()) {
                            String username = userDoc.getString("username");
                            String avatarUrl = userDoc.getString("avatarUrl");
                            if (username != null) post.setAuthorUsername(username);
                            if (avatarUrl != null) post.setAuthorAvatarUrl(avatarUrl);
                        } else {
                            post.setAuthorUsername(post.getAuthorId());
                        }
                        savePostToFirestoreInternal(post, callback);
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    @Override public void onFailure(@NonNull Exception error) {
                        post.setAuthorUsername(post.getAuthorId());
                        savePostToFirestoreInternal(post, callback);
                    }
                });
    }

    private void savePostToFirestoreInternal(Post post, Callback<Post> callback) {
        if (!post.validate()) {
            callback.onError(new IllegalArgumentException("Dữ liệu bài viết không hợp lệ."));
            return;
        }

        db.collection("posts").document(post.getId()).set(post.toMap())
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Void>() {
                    @Override public void onSuccess(Void unused) {
                        db.collection("users").document(post.getAuthorId())
                                .update("postsCount", FieldValue.increment(1));
                        callback.onSuccess(post);
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    @Override public void onFailure(@NonNull Exception error) { callback.onError(error); }
                });
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

    /** Lưu lại bộ ảnh hiện có cùng ảnh mới, giữ nguyên ID và các lượt tương tác. */
    public void updatePostWithImages(final String currentUserId, final String postId,
                                     final String caption, final List<Uri> imageUris,
                                     final List<String> hashtags, final Context context,
                                     final Callback<Post> callback) {
        if (currentUserId == null || currentUserId.isEmpty() || postId == null || postId.isEmpty()
                || imageUris == null || imageUris.isEmpty() || imageUris.size() > 10) {
            callback.onError(new IllegalArgumentException("Chọn từ 1 đến 10 ảnh để lưu."));
            return;
        }
        final List<Uri> images = new ArrayList<>(imageUris);
        final List<String> tags = hashtags == null ? new ArrayList<String>() : new ArrayList<>(hashtags);
        final Context app = context.getApplicationContext();
        getPostDetail(postId, new Callback<Post>() {
            @Override public void onSuccess(final Post post) {
                if (!currentUserId.equals(post.getAuthorId())) {
                    callback.onError(new SecurityException("Chỉ tác giả được sửa bộ ảnh này."));
                    return;
                }
                IO_EXECUTOR.execute(new Runnable() {
                    @Override public void run() {
                        final List<String> urls = new ArrayList<>();
                        final List<Map<String, String>> metadata = new ArrayList<>();
                        try {
                            for (int index = 0; index < images.size(); index++) {
                                Uri uri = images.get(index);
                                String value = uri.toString();
                                int oldIndex = post.getImageUrls().indexOf(value);
                                if (oldIndex >= 0) {
                                    urls.add(value);
                                    List<Map<String, String>> oldMetadata = post.getPhotoMetadata();
                                    metadata.add(oldIndex < oldMetadata.size()
                                            ? oldMetadata.get(oldIndex) : new HashMap<String, String>());
                                } else {
                                    String scheme = uri.getScheme();
                                    if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                                        throw new SecurityException("Ảnh mạng không thuộc bộ ảnh này.");
                                    }
                                    metadata.add(readExifWithoutLocation(app, uri));
                                    String mime = resolveImageMimeType(app, uri);
                                    File file = copyImageToCache(app, uri, postId, mime);
                                    try {
                                        urls.add(CloudinaryUploader.upload(app, file, mime));
                                    } finally {
                                        file.delete();
                                    }
                                }
                            }
                            MAIN_HANDLER.post(new Runnable() {
                                @Override public void run() {
                                    post.setId(postId);
                                    post.setCaption(caption == null ? "" : caption.trim());
                                    post.setHashtags(tags);
                                    post.setImageUrls(urls);
                                    post.setImageUrl(urls.get(0));
                                    post.setPhotoMetadata(metadata);
                                    post.touch();
                                    db.collection("posts").document(postId).update(
                                            "caption", post.getCaption(),
                                            "hashtags", post.getHashtags(),
                                            "imageUrls", post.getImageUrls(),
                                            "imageUrl", post.getImageUrl(),
                                            "photoMetadata", post.getPhotoMetadata(),
                                            "updatedAt", post.getUpdatedAt())
                                            .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Void>() {
                                                @Override public void onSuccess(Void unused) { callback.onSuccess(post); }
                                            })
                                            .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                                                @Override public void onFailure(@NonNull Exception error) { callback.onError(error); }
                                            });
                                }
                            });
                        } catch (final Exception error) {
                            MAIN_HANDLER.post(new Runnable() {
                                @Override public void run() { callback.onError(error); }
                            });
                        }
                    }
                });
            }

            @Override public void onError(Exception error) { callback.onError(error); }
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
                    if (documentSnapshot.exists() && documentSnapshot.getData() != null
                            && !Boolean.TRUE.equals(documentSnapshot.getBoolean("hidden"))) {
                        Post post = Post.fromMap(documentSnapshot.getData());
                        callback.onSuccess(post);
                    } else {
                        callback.onError(new Exception("Bài viết không tồn tại."));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    // F19: Chronological feed excludes the viewer, including across page boundaries.
    public void getFeedPosts(DocumentSnapshot lastVisible, int limit, Callback<FeedResult> callback) {
        getFeedPosts(AuthService.getInstance().getCurrentUserId(), lastVisible, limit, callback);
    }

    public void getFeedPosts(String viewerId, DocumentSnapshot lastVisible, int limit, Callback<FeedResult> callback) {
        if (limit <= 0) { callback.onError(new IllegalArgumentException("Giới hạn bài viết không hợp lệ.")); return; }
        collectFeedPage(viewerId, lastVisible, limit, new ArrayList<>(), callback);
    }

    private void collectFeedPage(String viewerId, DocumentSnapshot cursor, int limit,
                                 List<Post> visible, Callback<FeedResult> callback) {
        int batchSize = Math.max(20, Math.min(50, limit));
        Query query = db.collection("posts").orderBy("createdAt", Query.Direction.DESCENDING).limit(batchSize);
        if (cursor != null) query = query.startAfter(cursor);
        query.get().addOnSuccessListener(snapshot -> {
            List<DocumentSnapshot> documents = snapshot.getDocuments();
            List<Post> raw = new ArrayList<>();
            for (DocumentSnapshot doc : documents) {
                Post post = doc.getData() == null ? null : Post.fromMap(doc.getData());
                if (post != null) post.setId(doc.getId());
                raw.add(post);
            }
            com.example.myapplication.util.FeedPageSelector.Selection selection =
                    com.example.myapplication.util.FeedPageSelector.select(raw, viewerId, limit - visible.size());
            visible.addAll(selection.posts);
            DocumentSnapshot next = selection.lastConsumedIndex < 0 ? cursor : documents.get(selection.lastConsumedIndex);
            boolean hasMore = selection.hasUnreadItems || documents.size() == batchSize;
            if (visible.size() < limit && hasMore) {
                // An entire raw page can belong to the viewer. Continue instead of showing a false empty feed.
                collectFeedPage(viewerId, next, limit, visible, callback);
            } else {
                callback.onSuccess(new FeedResult(visible, next, hasMore));
            }
        }).addOnFailureListener(callback::onError);
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
                            if (post != null && !post.isHidden()) {
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
                                        if (post != null && !post.isHidden()) posts.add(post);
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
        savePost(userId, postId, "Chung", callback);
    }

    public void savePost(String userId, String postId, String collectionName, Callback<SavedPost> callback) {
        if (userId == null || postId == null) {
            callback.onError(new IllegalArgumentException("Tham số không hợp lệ."));
            return;
        }

        String docId = userId + "_" + postId;

        db.collection("saved_posts").document(docId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.getData() != null) {
                        SavedPost existing = SavedPost.fromMap(doc.getData());
                        existing.setCollectionName(collectionName);
                        db.collection("saved_posts").document(docId).set(existing.toMap())
                                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Void>() {
                                    @Override public void onSuccess(Void unused) { callback.onSuccess(existing); }
                                })
                                .addOnFailureListener(callback::onError);
                        return;
                    }

                    SavedPost savedPost = new SavedPost(userId, postId);
                    savedPost.setId(docId);
                    savedPost.setCollectionName(collectionName);

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
        final int[] remaining = {total};

        for (String pid : postIds) {
            getPostDetail(pid, new Callback<Post>() {
                @Override
                public void onSuccess(Post post) {
                    synchronized (posts) {
                        posts.add(post);
                        if (--remaining[0] == 0) {
                            posts.sort((p1, p2) -> Long.compare(p2.getCreatedAt(), p1.getCreatedAt()));
                            callback.onSuccess(posts);
                        }
                    }
                }

                @Override
                public void onError(Exception e) {
                    synchronized (posts) {
                        if (--remaining[0] == 0) {
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
