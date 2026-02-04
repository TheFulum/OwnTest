package com.example.owntest.managers;

import androidx.annotation.NonNull;

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

    // ========================= РЕГИСТРАЦИЯ С ПРОВЕРКОЙ УНИКАЛЬНОСТИ =========================

    public void registerUser(String email, String password, String nickname, String name, AuthCallback callback) {
        // Проверяем уникальность nickname
        DatabaseReference nicknameRef = database.child("nicknames").child(nickname);

        nicknameRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                if (currentData.getValue() != null) {
                    // Никнейм уже занят
                    return Transaction.abort();
                }
                // Временно резервируем никнейм
                currentData.setValue(true);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                if (!committed) {
                    callback.onFailure("Никнейм уже занят");
                    return;
                }

                // Создаем пользователя в Firebase Auth
                auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener(result -> {
                            String uid = result.getUser().getUid();
                            User user = new User(uid, email, nickname, name);

                            // Multi-location update для атомарности
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("users/" + uid, user);
                            updates.put("nicknames/" + nickname, uid);

                            database.updateChildren(updates)
                                    .addOnSuccessListener(v -> callback.onSuccess(uid))
                                    .addOnFailureListener(e -> {
                                        // Откатываем никнейм если не удалось создать юзера
                                        nicknameRef.removeValue();
                                        callback.onFailure(e.getMessage());
                                    });
                        })
                        .addOnFailureListener(e -> {
                            // Освобождаем никнейм если Auth не создался
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
                        updateUserDaysInApp(uid);
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

    // ========================= РАБОТА С ПОЛЬЗОВАТЕЛЯМИ =========================

    private void updateUserDaysInApp(String uid) {
        database.child("users").child(uid).get().addOnSuccessListener(snapshot -> {
            User user = snapshot.getValue(User.class);
            if (user != null) {
                boolean isNewDay = user.updateDaysInApp();

                // Обновляем только если это новый день (оптимизация)
                if (isNewDay) {
                    database.child("users").child(uid).setValue(user);
                } else {
                    // Просто обновляем lastLoginDate
                    database.child("users").child(uid).child("lastLoginDate")
                            .setValue(System.currentTimeMillis());
                }
            }
        });
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
        String oldNickname = user.getNickname();

        // Получаем старый никнейм чтобы обновить его в базе
        database.child("users").child(uid).get().addOnSuccessListener(snapshot -> {
            User oldUser = snapshot.getValue(User.class);
            if (oldUser == null) {
                callback.onFailure("Пользователь не найден");
                return;
            }

            String currentNickname = oldUser.getNickname();

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
                        .addOnSuccessListener(v -> callback.onSuccess(completionId))
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

}