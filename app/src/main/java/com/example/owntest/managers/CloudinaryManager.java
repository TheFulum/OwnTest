package com.example.owntest.managers;

import android.content.Context;
import android.net.Uri;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import java.util.HashMap;
import java.util.Map;

public class CloudinaryManager {
    private static CloudinaryManager instance;
    private static final String CLOUD_NAME = "dcdcdbhsr";
    private static final String API_KEY = "938191131632432";
    private static final String API_SECRET = "uL1Ivd1VTC4pbyBFuBTn99jhgH0";

    private CloudinaryManager() {
        // Private constructor
    }

    public static synchronized CloudinaryManager getInstance() {
        if (instance == null) {
            instance = new CloudinaryManager();
        }
        return instance;
    }

    public void init(Context context) {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", CLOUD_NAME);
        config.put("api_key", API_KEY);
        config.put("api_secret", API_SECRET);

        MediaManager.init(context, config);
    }

    public interface UploadCallback {
        void onSuccess(String imageUrl);
        void onFailure(String error);
    }

    public void uploadImage(Uri imageUri, String folder, UploadCallback callback) {
        MediaManager.get().upload(imageUri)
                .option("folder", folder)
                .option("resource_type", "image")
                .callback(new com.cloudinary.android.callback.UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        // Загрузка началась
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        // Прогресс загрузки
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        callback.onSuccess(imageUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        callback.onFailure(error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        // Повтор загрузки
                    }
                })
                .dispatch();
    }

    // Загрузка аватара пользователя
    public void uploadAvatar(Uri imageUri, String userId, UploadCallback callback) {
        uploadImage(imageUri, "avatars/" + userId, callback);
    }

    // Загрузка иконки теста
    public void uploadTestIcon(Uri imageUri, String testId, UploadCallback callback) {
        uploadImage(imageUri, "test_icons/" + testId, callback);
    }
}