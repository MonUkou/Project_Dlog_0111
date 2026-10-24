package com.example.myapplication.service.impl;

import android.content.Context;
import com.example.myapplication.R;
import org.json.JSONObject;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Unsigned image upload; call only from a background thread. */
public final class CloudinaryUploader {
    private CloudinaryUploader() { }

    public static String upload(Context context, File file, String mimeType) throws Exception {
        String cloud = context.getString(R.string.cloudinary_cloud_name).trim();
        String preset = context.getString(R.string.cloudinary_upload_preset).trim();
        if (cloud.isEmpty() || preset.isEmpty())
            throw new IllegalStateException("Chưa cấu hình Cloudinary cloud name và unsigned upload preset.");
        if (!cloud.matches("[A-Za-z0-9_-]+") || !preset.matches("[A-Za-z0-9_-]+"))
            throw new IllegalArgumentException("Cấu hình Cloudinary không hợp lệ.");
        String boundary = "DLog" + java.util.UUID.randomUUID().toString().replace("-", "");
        byte[] presetPart = ("--" + boundary + "\r\nContent-Disposition: form-data; name=\"upload_preset\"\r\n\r\n"
                + preset + "\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] fileHeader = ("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\""
                + file.getName() + "\"\r\nContent-Type: " + mimeType + "\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8);
        byte[] closing = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = (HttpURLConnection) new URL("https://api.cloudinary.com/v1_1/" + cloud + "/image/upload").openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(60000);
            connection.setFixedLengthStreamingMode((long) presetPart.length + fileHeader.length
                    + file.length() + closing.length);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            try (OutputStream out = new BufferedOutputStream(connection.getOutputStream())) {
                out.write(presetPart);
                out.write(fileHeader);
                try (InputStream in = new FileInputStream(file)) {
                    byte[] bytes = new byte[65536]; int count;
                    while ((count = in.read(bytes)) != -1) out.write(bytes, 0, count);
                }
                out.write(closing);
            }
            int status = connection.getResponseCode();
            try (InputStream in = status < 400 ? connection.getInputStream() : connection.getErrorStream()) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                if (in != null) { byte[] block = new byte[8192]; int count; while ((count = in.read(block)) != -1) bytes.write(block, 0, count); }
                JSONObject response = new JSONObject(bytes.toString("UTF-8"));
                if (status < 200 || status >= 300) {
                    JSONObject error = response.optJSONObject("error");
                    throw new IOException("Cloudinary: " + (error == null ? "HTTP " + status
                            : error.optString("message", "HTTP " + status)));
                }
                String secureUrl = response.optString("secure_url", "");
                if (!secureUrl.startsWith("https://res.cloudinary.com/")) throw new IOException("Cloudinary không trả về URL ảnh hợp lệ.");
                return secureUrl;
            }
        } finally { connection.disconnect(); }
    }
}
