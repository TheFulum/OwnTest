package com.example.owntest.managers;

import androidx.annotation.NonNull;

import com.example.owntest.models.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.Query;

import com.example.owntest.models.User;
import com.example.owntest.models.Test;
import com.example.owntest.models.TestCompletion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseManager {
    private static FirebaseManager instance;
    private FirebaseAuth auth;
    private DatabaseReference database;

    private FirebaseManager() {
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public FirebaseAuth getAuth() {
        return auth;
    }

    public DatabaseReference getDatabase() {
        return database;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public boolean isUserLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    // ========================= CALLBACK INTERFACES =========================

    public interface AuthCallback {
        void onSuccess(String userId);

        void onFailure(String error);
    }

    public interface UserCallback {
        void onSuccess(User user);

        void onFailure(String error);
    }

    public interface TestCallback {
        void onSuccess(Test test);

        void onFailure(String error);
    }

    public interface TestListCallback {
        void onSuccess(List<Test> tests);

        void onFailure(String error);
    }

    public interface CompletionCallback {
        void onSuccess(TestCompletion completion);

        void onFailure(String error);
    }

    public interface NotificationListCallback {
        void onSuccess(List<Notification> notifications);

        void onFailure(String error);
    }

    public interface UnreadCountCallback {
        void onSuccess(int count);

        void onFailure(String error);
    }

    public interface TestWithCompletionListCallback {
        void onSuccess(List<com.example.owntest.models.TestWithCompletion> tests);

        void onFailure(String error);
    }

    // ========================= РЕГИСТРАЦИЯ С ПРОВЕРКОЙ УНИКАЛЬНОСТИ =========================

    public void registerUser(String email, String password, String nickname, String name, AuthCallback callback) {
        DatabaseReference nicknameRef = database.child("nicknames").child(nickname);

        nicknameRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                if (currentData.getValue() != null) {
                    return Transaction.abort();
                }
                currentData.setValue(true);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                if (!committed) {
                    callback.onFailure("Никнейм уже занят");
                    return;
                }

                auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener(result -> {
                            String uid = result.getUser().getUid();
                            User user = new User(uid, email, nickname, name);  // registrationDate устанавливается автоматически

                            Map<String, Object> updates = new HashMap<>();
                            updates.put("users/" + uid, user);
                            updates.put("nicknames/" + nickname, uid);

                            database.updateChildren(updates)
                                    .addOnSuccessListener(v -> callback.onSuccess(uid))
                                    .addOnFailureListener(e -> {
                                        nicknameRef.removeValue();
                                        callback.onFailure(e.getMessage());
                                    });
                        })
                        .addOnFailureListener(e -> {
                            nicknameRef.removeValue();
                            String errorMsg = e.getMessage();
                            if (errorMsg != null && errorMsg.contains("email address is already in use")) {
                                callback.onFailure("Email уже зарегистрирован");
                            } else {
                                callback.onFailure(errorMsg);
                            }
                        });
            }
        });
    }

    // ========================= АВТОРИЗАЦИЯ =========================

    public void loginUser(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && auth.getCurrentUser() != null) {
                        String uid = auth.getCurrentUser().getUid();
                        // updateUserDaysInApp(uid);  ← УДАЛЕНО
                        callback.onSuccess(uid);
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Ошибка входа";
                        callback.onFailure(error);
                    }
                });
    }

    // ========================= ВОССТАНОВЛЕНИЕ ПАРОЛЯ =========================

    public void resetPassword(String email, AuthCallback callback) {
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess("Письмо отправлено");
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Ошибка отправки";
                        callback.onFailure(error);
                    }
                });
    }

    // ========================= ВЫХОД =========================

    public void logout() {
        auth.signOut();
    }

    public void getUserData(String uid, UserCallback callback) {
        database.child("users").child(uid).get()
                .addOnSuccessListener(snapshot -> {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        callback.onSuccess(user);
                    } else {
                        callback.onFailure("Пользователь не найден");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void updateUserProfile(User user, AuthCallback callback) {
        if (getCurrentUser() == null) {
            callback.onFailure("Пользователь не авторизован");
            return;
        }

        String uid = getCurrentUser().getUid();

        database.child("users").child(uid).get().addOnSuccessListener(snapshot -> {
            User oldUser = snapshot.getValue(User.class);
            if (oldUser == null) {
                callback.onFailure("Пользователь не найден");
                return;
            }

            user.setRegistrationDate(oldUser.getRegistrationDate());

            String currentNickname = oldUser.getNickname();

            if (currentNickname.equals(user.getNickname())) {
                database.child("users").child(uid).setValue(user)
                        .addOnSuccessListener(aVoid -> callback.onSuccess("Профиль обновлен"))
                        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                return;
            }

            // Если никнейм не изменился - просто обновляем
            if (currentNickname.equals(user.getNickname())) {
                database.child("users").child(uid).setValue(user)
                        .addOnSuccessListener(aVoid -> callback.onSuccess("Профиль обновлен"))
                        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                return;
            }

            // Если никнейм изменился - проверяем уникальность
            DatabaseReference newNicknameRef = database.child("nicknames").child(user.getNickname());
            newNicknameRef.runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                    if (currentData.getValue() != null) {
                        return Transaction.abort();
                    }
                    currentData.setValue(uid);
                    return Transaction.success(currentData);
                }

                @Override
                public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                    if (!committed) {
                        callback.onFailure("Никнейм уже занят");
                        return;
                    }

                    // Обновляем данные пользователя и удаляем старый никнейм
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("users/" + uid, user);
                    updates.put("nicknames/" + user.getNickname(), uid);
                    updates.put("nicknames/" + currentNickname, null); // Удаляем старый

                    database.updateChildren(updates)
                            .addOnSuccessListener(v -> callback.onSuccess("Профиль обновлен"))
                            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                }
            });
        });
    }

    // ========================= СОЗДАНИЕ ТЕСТА =========================

    public void createTest(Test test, AuthCallback callback) {
        if (getCurrentUser() == null) {
            callback.onFailure("Пользователь не авторизован");
            return;
        }

        String testId = database.child("tests").push().getKey();
        if (testId == null) {
            callback.onFailure("Ошибка создания ID теста");
            return;
        }

        test.setTestId(testId);
        String uid = getCurrentUser().getUid();

        // Сохраняем тест и добавляем его в список созданных тестов пользователя
        Map<String, Object> updates = new HashMap<>();
        updates.put("tests/" + testId, test);
        updates.put("userCreatedTests/" + uid + "/" + testId, true);

        database.updateChildren(updates)
                .addOnSuccessListener(v -> callback.onSuccess(testId))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ========================= ПОЛУЧЕНИЕ ТЕСТОВ =========================

    public void getAllTests(TestListCallback callback) {
        database.child("tests").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Test> tests = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Test test = child.getValue(Test.class);
                    if (test != null) {
                        tests.add(test);
                    }
                }
                callback.onSuccess(tests);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailure(error.getMessage());
            }
        });
    }

    public void getUserCreatedTests(String uid, TestListCallback callback) {
        database.child("userCreatedTests").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Test> tests = new ArrayList<>();
                        int totalTests = (int) snapshot.getChildrenCount();

                        if (totalTests == 0) {
                            callback.onSuccess(tests);
                            return;
                        }

                        final int[] loadedCount = {0};

                        for (DataSnapshot child : snapshot.getChildren()) {
                            String testId = child.getKey();
                            database.child("tests").child(testId).get()
                                    .addOnSuccessListener(testSnapshot -> {
                                        Test test = testSnapshot.getValue(Test.class);
                                        if (test != null) {
                                            tests.add(test);
                                        }
                                        loadedCount[0]++;
                                        if (loadedCount[0] == totalTests) {
                                            // Сортируем по дате создания (новые сверху)
                                            Collections.sort(tests, (t1, t2) -> Long.compare(t2.getCreatedDate(), t1.getCreatedDate()));
                                            callback.onSuccess(tests);
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onFailure(error.getMessage());
                    }
                });
    }

    public void getUserCompletedTests(String uid, TestListCallback callback) {
        database.child("userCompletions").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Test> tests = new ArrayList<>();
                        int totalTests = (int) snapshot.getChildrenCount();

                        if (totalTests == 0) {
                            callback.onSuccess(tests);
                            return;
                        }

                        final int[] loadedCount = {0};

                        for (DataSnapshot child : snapshot.getChildren()) {
                            String testId = child.getKey();
                            database.child("tests").child(testId).get()
                                    .addOnSuccessListener(testSnapshot -> {
                                        Test test = testSnapshot.getValue(Test.class);
                                        if (test != null) {
                                            tests.add(test);
                                        }
                                        loadedCount[0]++;
                                        if (loadedCount[0] == totalTests) {
                                            callback.onSuccess(tests);
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onFailure(error.getMessage());
                    }
                });
    }

    public void getUserCompletedTestsWithCompletions(String uid, TestWithCompletionListCallback callback) {
        database.child("userCompletions").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<com.example.owntest.models.TestWithCompletion> testsWithCompletions = new ArrayList<>();
                        int totalTests = (int) snapshot.getChildrenCount();

                        if (totalTests == 0) {
                            callback.onSuccess(testsWithCompletions);
                            return;
                        }

                        final int[] loadedCount = {0};

                        for (DataSnapshot child : snapshot.getChildren()) {
                            String testId = child.getKey();
                            String completionId = child.getValue(String.class);

                            // Загружаем тест
                            database.child("tests").child(testId).get()
                                    .addOnSuccessListener(testSnapshot -> {
                                        Test test = testSnapshot.getValue(Test.class);

                                        // Загружаем прохождение
                                        database.child("completions").child(completionId).get()
                                                .addOnSuccessListener(completionSnapshot -> {
                                                    TestCompletion completion = completionSnapshot.getValue(TestCompletion.class);

                                                    if (test != null && completion != null) {
                                                        com.example.owntest.models.TestWithCompletion twc =
                                                                new com.example.owntest.models.TestWithCompletion(test, completion);
                                                        testsWithCompletions.add(twc);
                                                    }

                                                    loadedCount[0]++;
                                                    if (loadedCount[0] == totalTests) {
                                                        callback.onSuccess(testsWithCompletions);
                                                    }
                                                });
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onFailure(error.getMessage());
                    }
                });
    }

    public void getTestById(String testId, TestCallback callback) {
        database.child("tests").child(testId).get()
                .addOnSuccessListener(snapshot -> {
                    Test test = snapshot.getValue(Test.class);
                    if (test != null) {
                        callback.onSuccess(test);
                    } else {
                        callback.onFailure("Тест не найден");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ========================= СОХРАНЕНИЕ ПРОХОЖДЕНИЯ ТЕСТА =========================

    public void saveTestCompletion(TestCompletion completion, AuthCallback callback) {
        if (getCurrentUser() == null) {
            callback.onFailure("Пользователь не авторизован");
            return;
        }

        String completionId = database.child("completions").push().getKey();
        if (completionId == null) {
            callback.onFailure("Ошибка создания ID прохождения");
            return;
        }

        completion.setCompletionId(completionId);
        String uid = getCurrentUser().getUid();
        String testId = completion.getTestId();

        // Обновляем статистику теста
        database.child("tests").child(testId).get().addOnSuccessListener(snapshot -> {
            Test test = snapshot.getValue(Test.class);
            if (test != null) {
                // ← ВОТ ТУТ БЫЛ БАГ: было completion.getScore() (кол-во правильных ответов)
                // Теперь берём getUserRating() — звёзды которые юзер поставил через ratingBar
                test.addCompletion(completion.getUserRating());

                Map<String, Object> updates = new HashMap<>();
                updates.put("completions/" + completionId, completion);
                updates.put("userCompletions/" + uid + "/" + testId, completionId);
                updates.put("tests/" + testId, test);

                database.updateChildren(updates)
                        .addOnSuccessListener(v -> {
                            // Создаём уведомление для создателя теста
                            createTestCompletionNotification(completion, test);
                            callback.onSuccess(completionId);
                        })
                        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
            }
        });
    }

    public void saveTestProgress(TestCompletion progress, AuthCallback callback) {
        if (getCurrentUser() == null) {
            callback.onFailure("Пользователь не авторизован");
            return;
        }

        String completionId = progress.getCompletionId();
        if (completionId == null) {
            completionId = database.child("completions").push().getKey();
            if (completionId == null) {
                callback.onFailure("Ошибка создания ID");
                return;
            }
            progress.setCompletionId(completionId);
        }

        // Создаём финальную переменную для использования в лямбде
        final String finalCompletionId = completionId;

        String uid = getCurrentUser().getUid();
        String testId = progress.getTestId();

        Map<String, Object> updates = new HashMap<>();
        updates.put("completions/" + finalCompletionId, progress);
        updates.put("userCompletions/" + uid + "/" + testId, finalCompletionId);

        database.updateChildren(updates)
                .addOnSuccessListener(v -> callback.onSuccess(finalCompletionId))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void getTestCompletion(String userId, String testId, CompletionCallback callback) {
        database.child("userCompletions").child(userId).child(testId).get()
                .addOnSuccessListener(snapshot -> {
                    String completionId = snapshot.getValue(String.class);
                    if (completionId != null) {
                        database.child("completions").child(completionId).get()
                                .addOnSuccessListener(compSnapshot -> {
                                    TestCompletion completion = compSnapshot.getValue(TestCompletion.class);
                                    if (completion != null) {
                                        callback.onSuccess(completion);
                                    } else {
                                        callback.onFailure("Прохождение не найдено");
                                    }
                                });
                    } else {
                        callback.onFailure("Тест не пройден");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ========================= УДАЛЕНИЕ ТЕСТА =========================

    public void deleteTest(String testId, AuthCallback callback) {
        if (getCurrentUser() == null) {
            callback.onFailure("Пользователь не авторизован");
            return;
        }

        String uid = getCurrentUser().getUid();

        // Сначала проверяем, является ли пользователь создателем теста
        database.child("tests").child(testId).get().addOnSuccessListener(snapshot -> {
            Test test = snapshot.getValue(Test.class);
            if (test == null) {
                callback.onFailure("Тест не найден");
                return;
            }

            if (!test.getCreatorId().equals(uid)) {
                callback.onFailure("У вас нет прав на удаление этого теста");
                return;
            }

            // Удаляем тест и все связанные данные
            Map<String, Object> updates = new HashMap<>();
            updates.put("tests/" + testId, null);
            updates.put("userCreatedTests/" + uid + "/" + testId, null);

            database.updateChildren(updates)
                    .addOnSuccessListener(v -> callback.onSuccess("Тест удален"))
                    .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
        }).addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ========================= ОБНОВЛЕНИЕ ТЕСТА =========================

    public void updateTest(Test test, AuthCallback callback) {
        if (getCurrentUser() == null) {
            callback.onFailure("Пользователь не авторизован");
            return;
        }

        String uid = getCurrentUser().getUid();
        String testId = test.getTestId();

        if (testId == null) {
            callback.onFailure("ID теста не указан");
            return;
        }

        // Проверяем, является ли пользователь создателем
        database.child("tests").child(testId).get().addOnSuccessListener(snapshot -> {
            Test existingTest = snapshot.getValue(Test.class);
            if (existingTest == null) {
                callback.onFailure("Тест не найден");
                return;
            }

            if (!existingTest.getCreatorId().equals(uid)) {
                callback.onFailure("У вас нет прав на редактирование этого теста");
                return;
            }

            // Сохраняем статистику при редактировании (не сбрасываем)
            test.setCompletionsCount(existingTest.getCompletionsCount());
            test.setTotalRatings(existingTest.getTotalRatings());
            test.setAverageRating(existingTest.getAverageRating());

            // Обновляем тест
            database.child("tests").child(testId).setValue(test)
                    .addOnSuccessListener(v -> callback.onSuccess("Тест обновлен"))
                    .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
        }).addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ========================= ПРОВЕРКА ПРАВ ДОСТУПА =========================

    public interface TestCreatorCallback {
        void onResult(boolean isCreator);
    }

    public void isTestCreator(String testId, TestCreatorCallback callback) {
        if (getCurrentUser() == null) {
            callback.onResult(false);
            return;
        }

        String uid = getCurrentUser().getUid();

        database.child("tests").child(testId).get().addOnSuccessListener(snapshot -> {
            Test test = snapshot.getValue(Test.class);
            if (test != null && test.getCreatorId().equals(uid)) {
                callback.onResult(true);
            } else {
                callback.onResult(false);
            }
        }).addOnFailureListener(e -> callback.onResult(false));
    }
// ========================= УВЕДОМЛЕНИЯ =========================

    public interface NotificationCallback {
        void onSuccess(Notification notification);
        void onFailure(String error);
    }


    // Создать уведомление
    public void createNotification(Notification notification, AuthCallback callback) {
        String notificationId = database.child("notifications").child(notification.getUserId()).push().getKey();
        if (notificationId == null) {
            callback.onFailure("Ошибка создания ID уведомления");
            return;
        }

        notification.setNotificationId(notificationId);

        database.child("notifications").child(notification.getUserId()).child(notificationId)
                .setValue(notification)
                .addOnSuccessListener(v -> callback.onSuccess(notificationId))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // Получить уведомления пользователя
    public void getUserNotifications(String userId, NotificationListCallback callback) {
        database.child("notifications").child(userId)
                .orderByChild("createdDate")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Notification> notifications = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Notification notification = child.getValue(Notification.class);
                            if (notification != null) {
                                notifications.add(notification);
                            }
                        }
                        // Сортируем по дате (новые сначала)
                        Collections.reverse(notifications);
                        callback.onSuccess(notifications);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onFailure(error.getMessage());
                    }
                });
    }

    // Пометить уведомление как прочитанное
    public void markNotificationAsRead(String userId, String notificationId, AuthCallback callback) {
        database.child("notifications").child(userId).child(notificationId).child("read")
                .setValue(true)
                .addOnSuccessListener(v -> callback.onSuccess("Прочитано"))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // Пометить все уведомления как прочитанные
    public void markAllNotificationsAsRead(String userId, AuthCallback callback) {
        database.child("notifications").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Map<String, Object> updates = new HashMap<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            updates.put(child.getKey() + "/read", true);
                        }

                        if (updates.isEmpty()) {
                            callback.onSuccess("Нет уведомлений");
                            return;
                        }

                        database.child("notifications").child(userId).updateChildren(updates)
                                .addOnSuccessListener(v -> callback.onSuccess("Все прочитаны"))
                                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onFailure(error.getMessage());
                    }
                });
    }

// ========================= ПРОВЕРКА ТЕСТА (РУЧНАЯ) =========================

    // Обновить прохождение теста с баллами
    public void updateCompletionWithPoints(String completionId, int earnedPoints, AuthCallback callback) {
        database.child("completions").child(completionId).get()
                .addOnSuccessListener(snapshot -> {
                    TestCompletion completion = snapshot.getValue(TestCompletion.class);
                    if (completion == null) {
                        callback.onFailure("Прохождение не найдено");
                        return;
                    }

                    completion.setEarnedPoints(earnedPoints);
                    completion.setCheckStatus("CHECKED");

                    // Пересчитываем процент на основе баллов
                    if (completion.getMaxPoints() > 0) {
                        double percentage = (double) earnedPoints / completion.getMaxPoints() * 100;
                        completion.setPercentage(percentage);
                    }

                    database.child("completions").child(completionId).setValue(completion)
                            .addOnSuccessListener(v -> {
                                // Создаем уведомление юзеру что тест проверен
                                createTestCheckedNotification(completion);
                                callback.onSuccess("Тест проверен");
                            })
                            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                });
    }

    // Создать уведомление о проверке теста
    private void createTestCheckedNotification(TestCompletion completion) {
        // Получаем данные о тесте
        getTestById(completion.getTestId(), new TestCallback() {
            @Override
            public void onSuccess(Test test) {
                Notification notification = new Notification(
                        null,
                        completion.getUserId(),
                        "TEST_CHECKED",
                        test.getTestId(),
                        test.getTitle(),
                        completion.getCompletionId(),
                        test.getCreatorNickname()
                );

                createNotification(notification, new AuthCallback() {
                    @Override
                    public void onSuccess(String userId) {
                        // Уведомление создано
                    }

                    @Override
                    public void onFailure(String error) {
                        // Игнорируем ошибку
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                // Игнорируем ошибку
            }
        });
    }

    // Создать уведомление о прохождении теста
    private void createTestCompletionNotification(TestCompletion completion, Test test) {
        // Получаем данные о юзере который прошёл тест
        getUserData(completion.getUserId(), new UserCallback() {
            @Override
            public void onSuccess(User user) {
                // Не создаём уведомление если юзер сам прошёл свой тест
                if (completion.getUserId().equals(test.getCreatorId())) {
                    return;
                }

                Notification notification = new Notification(
                        null,
                        test.getCreatorId(), // Уведомление для создателя
                        "TEST_COMPLETED",
                        test.getTestId(),
                        test.getTitle(),
                        completion.getCompletionId(),
                        user.getNickname() // Кто прошёл тест
                );

                createNotification(notification, new AuthCallback() {
                    @Override
                    public void onSuccess(String userId) {
                        // Уведомление создано
                    }

                    @Override
                    public void onFailure(String error) {
                        // Игнорируем ошибку
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                // Игнорируем ошибку
            }
        });
    }

    // Получить тесты ожидающие проверки для создателя
    public void getPendingTestsForCreator(String creatorId, TestListCallback callback) {
        // Сначала получаем все тесты созданные юзером
        getUserCreatedTests(creatorId, new TestListCallback() {
            @Override
            public void onSuccess(List<Test> tests) {
                // Теперь для каждого теста проверяем есть ли ожидающие проверки
                List<Test> pendingTests = new ArrayList<>();
                final int[] checkedCount = {0};

                if (tests.isEmpty()) {
                    callback.onSuccess(pendingTests);
                    return;
                }

                for (Test test : tests) {
                    if (!test.isManualCheck()) {
                        checkedCount[0]++;
                        if (checkedCount[0] == tests.size()) {
                            callback.onSuccess(pendingTests);
                        }
                        continue;
                    }

                    // Проверяем есть ли PENDING прохождения
                    database.child("completions")
                            .orderByChild("testId")
                            .equalTo(test.getTestId())
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    boolean hasPending = false;
                                    for (DataSnapshot child : snapshot.getChildren()) {
                                        TestCompletion comp = child.getValue(TestCompletion.class);
                                        if (comp != null && comp.isPending()) {
                                            hasPending = true;
                                            break;
                                        }
                                    }

                                    if (hasPending) {
                                        pendingTests.add(test);
                                    }

                                    checkedCount[0]++;
                                    if (checkedCount[0] == tests.size()) {
                                        callback.onSuccess(pendingTests);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    checkedCount[0]++;
                                    if (checkedCount[0] == tests.size()) {
                                        callback.onSuccess(pendingTests);
                                    }
                                }
                            });
                }
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }
        });
    }

    // ========================= УВЕДОМЛЕНИЯ =========================

    public void sendTestCheckedNotification(String userId, String testId, String testTitle) {
        String notificationId = database.child("notifications").child(userId).push().getKey();
        if (notificationId == null) return;

        Notification notification = new Notification(
                notificationId,
                userId,
                "TEST_CHECKED",
                testId,
                testTitle,
                null,
                null
        );

        database.child("notifications").child(userId).child(notificationId)
                .setValue(notification);
    }

    public void getUnreadNotificationsCount(String userId, UnreadCountCallback callback) {
        database.child("notifications").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int count = 0;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Notification notification = child.getValue(Notification.class);
                            if (notification != null && !notification.isRead()) {
                                count++;
                            }
                        }
                        callback.onSuccess(count);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onFailure(error.getMessage());
                    }
                });
    }
}