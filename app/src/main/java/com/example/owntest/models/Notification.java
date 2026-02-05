package com.example.owntest.models;

public class Notification {
    private String notificationId;
    private String userId;
    private String type;
    private String testId;
    private String testTitle;
    private String completionId;
    private String userName;
    private boolean isRead;
    private long createdDate;

    public Notification() {
        this.isRead = false;
        this.createdDate = System.currentTimeMillis();
    }

    public Notification(String notificationId, String userId, String type,
                        String testId, String testTitle, String completionId, String userName) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.type = type;
        this.testId = testId;
        this.testTitle = testTitle;
        this.completionId = completionId;
        this.userName = userName;
        this.isRead = false;
        this.createdDate = System.currentTimeMillis();
    }

    // Getters
    public String getNotificationId() { return notificationId; }
    public String getUserId() { return userId; }
    public String getType() { return type; }
    public String getTestId() { return testId; }
    public String getTestTitle() { return testTitle; }
    public String getCompletionId() { return completionId; }
    public String getUserName() { return userName; }
    public boolean isRead() { return isRead; }
    public long getCreatedDate() { return createdDate; }

    // Setters
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setType(String type) { this.type = type; }
    public void setTestId(String testId) { this.testId = testId; }
    public void setTestTitle(String testTitle) { this.testTitle = testTitle; }
    public void setCompletionId(String completionId) { this.completionId = completionId; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setRead(boolean read) { isRead = read; }
    public void setCreatedDate(long createdDate) { this.createdDate = createdDate; }
}