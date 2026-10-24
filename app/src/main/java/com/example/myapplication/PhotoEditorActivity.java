package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhotoEditorActivity extends AppCompatActivity {
    public static final String EXTRA_IMAGE_URI = "image_uri";
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private ImageView preview;
    private ProgressBar progress;
    private Button cropButton;
    private Button filterButton;
    private Uri source;
    private int rotation;
    private int cropMode;
    private int filterMode;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_photo_editor);
        source = Uri.parse(getIntent().getStringExtra(EXTRA_IMAGE_URI));
        preview = findViewById(R.id.ivEditorPreview);
        progress = findViewById(R.id.progressEditor);
        cropButton = findViewById(R.id.btnCropEditor);
        filterButton = findViewById(R.id.btnFilterEditor);
        preview.setImageURI(source);
        findViewById(R.id.btnCancelEditor).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { finish(); }
        });
        findViewById(R.id.btnRotateEditor).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { rotation = (rotation + 90) % 360; updatePreview(); }
        });
        cropButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                cropMode = (cropMode + 1) % 4;
                String[] labels = {"Cắt: Gốc", "Cắt: 1:1", "Cắt: 4:5", "Cắt: 16:9"};
                cropButton.setText(labels[cropMode]);
            }
        });
        filterButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                filterMode = (filterMode + 1) % 4;
                String[] labels = {"Màu: Gốc", "Màu: Ấm", "Màu: Lạnh", "Màu: Đen trắng"};
                filterButton.setText(labels[filterMode]);
                updatePreview();
            }
        });
        findViewById(R.id.btnSaveEditor).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { saveEditedImage(); }
        });
    }

    private void updatePreview() {
        preview.setRotation(rotation);
        preview.setColorFilter(new ColorMatrixColorFilter(filterMatrix(filterMode)));
    }

    private void saveEditedImage() {
        progress.setVisibility(View.VISIBLE);
        worker.execute(new Runnable() {
            @Override public void run() {
                try {
                    Bitmap bitmap = decodeBitmap(source);
                    bitmap = rotate(bitmap, rotation);
                    bitmap = crop(bitmap, cropMode);
                    bitmap = applyFilter(bitmap, filterMode);
                    File folder = new File(getCacheDir(), "edited_photos");
                    if (!folder.exists()) folder.mkdirs();
                    File output = new File(folder, "edit_" + System.currentTimeMillis() + ".jpg");
                    try (FileOutputStream stream = new FileOutputStream(output)) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, stream);
                    }
                    bitmap.recycle();
                    final Uri result = Uri.fromFile(output);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            Intent data = new Intent();
                            data.putExtra(EXTRA_IMAGE_URI, result.toString());
                            setResult(RESULT_OK, data);
                            finish();
                        }
                    });
                } catch (final Exception error) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            progress.setVisibility(View.GONE);
                            Toast.makeText(PhotoEditorActivity.this, "Không thể chỉnh ảnh: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

    private Bitmap decodeBitmap(Uri uri) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = getContentResolver().openInputStream(uri)) { BitmapFactory.decodeStream(input, null, bounds); }
        int sample = 1;
        while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) sample *= 2;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            Bitmap result = BitmapFactory.decodeStream(input, null, options);
            if (result == null) throw new IllegalStateException("Ảnh không đọc được");
            return result;
        }
    }

    private Bitmap rotate(Bitmap sourceBitmap, int degrees) {
        if (degrees == 0) return sourceBitmap;
        Matrix matrix = new Matrix(); matrix.postRotate(degrees);
        Bitmap result = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight(), matrix, true);
        if (result != sourceBitmap) sourceBitmap.recycle();
        return result;
    }

    private Bitmap crop(Bitmap sourceBitmap, int mode) {
        if (mode == 0) return sourceBitmap;
        float ratio = mode == 1 ? 1f : mode == 2 ? 4f / 5f : 16f / 9f;
        int width = sourceBitmap.getWidth(); int height = sourceBitmap.getHeight();
        int cropWidth = width; int cropHeight = Math.round(width / ratio);
        if (cropHeight > height) { cropHeight = height; cropWidth = Math.round(height * ratio); }
        Bitmap result = Bitmap.createBitmap(sourceBitmap, (width - cropWidth) / 2, (height - cropHeight) / 2, cropWidth, cropHeight);
        if (result != sourceBitmap) sourceBitmap.recycle();
        return result;
    }

    private Bitmap applyFilter(Bitmap sourceBitmap, int mode) {
        if (mode == 0) return sourceBitmap;
        Bitmap result = Bitmap.createBitmap(sourceBitmap.getWidth(), sourceBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColorFilter(new ColorMatrixColorFilter(filterMatrix(mode)));
        new Canvas(result).drawBitmap(sourceBitmap, 0, 0, paint);
        sourceBitmap.recycle();
        return result;
    }

    private ColorMatrix filterMatrix(int mode) {
        if (mode == 3) { ColorMatrix matrix = new ColorMatrix(); matrix.setSaturation(0f); return matrix; }
        if (mode == 1) return new ColorMatrix(new float[]{1.08f,0,0,0,10, 0,1.02f,0,0,4, 0,0,0.9f,0,-4, 0,0,0,1,0});
        if (mode == 2) return new ColorMatrix(new float[]{0.92f,0,0,0,-3, 0,1f,0,0,1, 0,0,1.1f,0,10, 0,0,0,1,0});
        return new ColorMatrix();
    }

    @Override protected void onDestroy() { worker.shutdownNow(); super.onDestroy(); }
}
