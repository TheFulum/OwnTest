package com.example.owntest;

import android.app.Application;
import com.example.owntest.managers.CloudinaryManager;

public class QuestifyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Инициализируем Cloudinary при старте приложения
        CloudinaryManager.getInstance().init(this);
    }
}