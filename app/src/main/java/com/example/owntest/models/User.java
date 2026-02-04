package com.example.owntest.models;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String uid;
    private String email;
    private String nickname;
    private String name;
    private int daysInApp;
    private long lastLoginDate;
    private String avatarUrl; // Добавьте это поле

    public User() {
    }

    public User(String uid, String email, String nickname, String name) {
        this.uid = uid;
        this.email = email;
        this.nickname = nickname;
        this.name = name;
        this.daysInApp = 1;
        this.lastLoginDate = System.currentTimeMillis();
        this.avatarUrl = ""; // Инициализируйте пустой строкой
    }

    // Геттеры и сеттеры
    public String getUid() { return uid; }
    public String getEmail() { return email; }
    public String getNickname() { return nickname; }
    public String getName() { return name; }
    public int getDaysInApp() { return daysInApp; }
    public long getLastLoginDate() { return lastLoginDate; }
    public String getAvatarUrl() { return avatarUrl; } // Добавьте геттер

    public void setUid(String uid) { this.uid = uid; }
    public void setEmail(String email) { this.email = email; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setName(String name) { this.name = name; }
    public void setDaysInApp(int daysInApp) { this.daysInApp = daysInApp; }
    public void setLastLoginDate(long lastLoginDate) { this.lastLoginDate = lastLoginDate; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; } // Добавьте сеттер

    // Метод для обновления дней в приложении
    public boolean updateDaysInApp() {
        long currentTime = System.currentTimeMillis();
        long lastLogin = lastLoginDate;

        // Проверяем, прошло ли больше 24 часов с последнего входа
        if (currentTime - lastLogin >= 24 * 60 * 60 * 1000) {
            daysInApp++;
            lastLoginDate = currentTime;
            return true;
        }
        lastLoginDate = currentTime;
        return false;
    }
}