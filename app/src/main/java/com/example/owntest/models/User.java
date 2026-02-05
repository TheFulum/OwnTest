package com.example.owntest.models;

public class User {
    private String uid;
    private String email;
    private String nickname;
    private String name;
    private long registrationDate;
    private String avatarUrl;

    public User() {
        // Пустой конструктор нужен для Firebase
    }

    public User(String uid, String email, String nickname, String name) {
        this.uid = uid;
        this.email = email;
        this.nickname = nickname;
        this.name = name;
        this.registrationDate = System.currentTimeMillis();
        this.avatarUrl = "";
    }

    // Основной метод — дни в приложении
    public int getDaysInApp() {
        if (registrationDate == 0) {
            return 1;
        }
        long diffMillis = System.currentTimeMillis() - registrationDate;
        int days = (int) (diffMillis / (24 * 60 * 60 * 1000L)) + 1;
        return Math.max(1, days); // Минимум 1 день
    }

    // Геттеры
    public String getUid() { return uid; }
    public String getEmail() { return email; }
    public String getNickname() { return nickname; }
    public String getName() { return name; }
    public long getRegistrationDate() { return registrationDate; }
    public String getAvatarUrl() { return avatarUrl != null ? avatarUrl : ""; }

    // Сеттеры
    public void setUid(String uid) { this.uid = uid; }
    public void setEmail(String email) { this.email = email; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setName(String name) { this.name = name; }
    public void setRegistrationDate(long registrationDate) { this.registrationDate = registrationDate; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    // Опционально: метод для обновления даты регистрации (на случай миграции)
    public void updateRegistrationDateIfNeeded(long newDate) {
        if (registrationDate == 0 || registrationDate > newDate) {
            registrationDate = newDate;
        }
    }
}