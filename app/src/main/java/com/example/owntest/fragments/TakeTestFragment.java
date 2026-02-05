package com.example.owntest.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.owntest.R;
import com.example.owntest.managers.FirebaseManager;
import com.example.owntest.models.Notification;
import com.example.owntest.models.Question;
import com.example.owntest.models.Test;
import com.example.owntest.models.TestCompletion;
import com.example.owntest.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

public class TakeTestFragment extends Fragment {

    private TextView tvProgress, tvQuestionText, tvQuestionDescription;
    private LinearLayout answerContainer;
    private MaterialButton btnNext, btnPrevious;
    private ProgressBar progressBar;

    private Test test;
    private int currentQuestionIndex = 0;
    private Map<String, Integer> userChoiceAnswers = new HashMap<>(); // для choice
    private Map<String, String> userTextAnswers = new HashMap<>(); // для text

    private RadioGroup currentRadioGroup;
    private TextInputEditText currentTextInput;

    private FirebaseManager firebaseManager;

    public static TakeTestFragment newInstance(String testId) {
        TakeTestFragment fragment = new TakeTestFragment();
        Bundle args = new Bundle();
        args.putString("testId", testId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_take_test, container, false);

        initViews(view);
        firebaseManager = FirebaseManager.getInstance();

        String testId = getArguments() != null ? getArguments().getString("testId") : null;
        if (testId != null) {
            loadTest(testId);
        }

        setupListeners();

        return view;
    }

    private void initViews(View view) {
        tvProgress = view.findViewById(R.id.tvProgress);
        tvQuestionText = view.findViewById(R.id.tvQuestionText);
        tvQuestionDescription = view.findViewById(R.id.tvQuestionDescription);
        answerContainer = view.findViewById(R.id.answerContainer);
        btnNext = view.findViewById(R.id.btnNext);
        btnPrevious = view.findViewById(R.id.btnPrevious);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void loadTest(String testId) {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.getTestById(testId, new FirebaseManager.TestCallback() {
            @Override
            public void onSuccess(Test loadedTest) {
                test = loadedTest;
                progressBar.setVisibility(View.GONE);
                displayQuestion();
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Ошибка загрузки: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        btnNext.setOnClickListener(v -> {
            if (saveCurrentAnswer()) {
                if (currentQuestionIndex == test.getQuestions().size() - 1) {
                    finishTest();
                } else {
                    currentQuestionIndex++;
                    displayQuestion();
                }
            }
        });

        btnPrevious.setOnClickListener(v -> {
            saveCurrentAnswer();
            currentQuestionIndex--;
            displayQuestion();
        });
    }

    private void displayQuestion() {
        if (test == null || test.getQuestions().isEmpty()) return;

        Question question = test.getQuestions().get(currentQuestionIndex);

        // Progress
        tvProgress.setText("Вопрос " + (currentQuestionIndex + 1) + " из " + test.getQuestions().size());

        // Текст вопроса
        tvQuestionText.setText(question.getQuestionText());

        // Описание (если есть)
        if (question.getQuestionDescription() != null && !question.getQuestionDescription().isEmpty()) {
            tvQuestionDescription.setVisibility(View.VISIBLE);
            tvQuestionDescription.setText(question.getQuestionDescription());
        } else {
            tvQuestionDescription.setVisibility(View.GONE);
        }

        // Очищаем контейнер ответов
        answerContainer.removeAllViews();

        // Рендерим в зависимости от типа вопроса
        if (question.isTextQuestion()) {
            renderTextInput(question);
        } else {
            renderChoiceInput(question);
        }

        // Кнопки
        btnPrevious.setVisibility(currentQuestionIndex > 0 ? View.VISIBLE : View.GONE);
        btnNext.setText(currentQuestionIndex == test.getQuestions().size() - 1 ? "Завершить" : "Далее");
    }

    private void renderChoiceInput(Question question) {
        currentTextInput = null;
        currentRadioGroup = new RadioGroup(getContext());
        currentRadioGroup.setOrientation(RadioGroup.VERTICAL);

        for (int i = 0; i < question.getAnswers().size(); i++) {
            RadioButton radioButton = new RadioButton(getContext());
            radioButton.setText(question.getAnswers().get(i).getAnswerText());
            radioButton.setId(i);
            radioButton.setPadding(16, 24, 16, 24);
            radioButton.setTextSize(16);
            currentRadioGroup.addView(radioButton);
        }

        // Восстанавливаем предыдущий ответ если есть
        Integer previousAnswer = userChoiceAnswers.get(question.getQuestionId());
        if (previousAnswer != null) {
            currentRadioGroup.check(previousAnswer);
        }

        answerContainer.addView(currentRadioGroup);
    }

    private void renderTextInput(Question question) {
        currentRadioGroup = null;

        View textInputView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_text_answer_input, answerContainer, false);

        currentTextInput = textInputView.findViewById(R.id.etTextAnswer);

        // Показываем подсказку о баллах если ручная проверка
        TextView tvHint = textInputView.findViewById(R.id.tvHint);
        if (test.isManualCheck()) {
            tvHint.setVisibility(View.VISIBLE);
            tvHint.setText("Максимум баллов: " + question.getMaxPoints());
        } else {
            tvHint.setVisibility(View.GONE);
        }

        // Восстанавливаем предыдущий ответ если есть
        String previousAnswer = userTextAnswers.get(question.getQuestionId());
        if (previousAnswer != null) {
            currentTextInput.setText(previousAnswer);
        }

        answerContainer.addView(textInputView);
    }

    private boolean saveCurrentAnswer() {
        Question question = test.getQuestions().get(currentQuestionIndex);

        if (question.isTextQuestion()) {
            // Сохраняем текстовый ответ
            if (currentTextInput != null) {
                String text = currentTextInput.getText().toString().trim();
                if (text.isEmpty()) {
                    Toast.makeText(getContext(), "Введите ответ", Toast.LENGTH_SHORT).show();
                    return false;
                }
                userTextAnswers.put(question.getQuestionId(), text);
            }
        } else {
            // Сохраняем выбор
            if (currentRadioGroup != null) {
                int selectedId = currentRadioGroup.getCheckedRadioButtonId();
                if (selectedId == -1) {
                    Toast.makeText(getContext(), "Выберите ответ", Toast.LENGTH_SHORT).show();
                    return false;
                }
                userChoiceAnswers.put(question.getQuestionId(), selectedId);
            }
        }

        return true;
    }

    private void finishTest() {
        if (!saveCurrentAnswer()) return;

        // Подсчитываем результат
        int score = 0;
        int totalQuestions = test.getQuestions().size();
        int maxPoints = 0;

        for (Question question : test.getQuestions()) {
            if (question.isChoiceQuestion()) {
                Integer userAnswer = userChoiceAnswers.get(question.getQuestionId());
                if (userAnswer != null && question.isCorrectAnswer(userAnswer)) {
                    score++;
                }
            }
            maxPoints += question.getMaxPoints();
        }

        // Определяем статус проверки
        String checkStatus;
        if (test.isManualCheck()) {
            checkStatus = "PENDING"; // Ожидает проверки
        } else {
            checkStatus = "AUTO"; // Автопроверка
        }

        // Создаем TestCompletion
        TestCompletion completion = new TestCompletion(
                null,
                firebaseManager.getCurrentUser().getUid(),
                test.getTestId(),
                score,
                totalQuestions,
                userChoiceAnswers,
                userTextAnswers,
                checkStatus,
                maxPoints
        );

        // Сохраняем
        progressBar.setVisibility(View.VISIBLE);
        btnNext.setEnabled(false);

        int finalScore = score;
        firebaseManager.saveTestCompletion(completion, new FirebaseManager.AuthCallback() {
            @Override
            public void onSuccess(String completionId) {
                progressBar.setVisibility(View.GONE);

                if (test.isManualCheck()) {
                    // Создаем уведомление создателю
                    createNotificationForCreator(completionId);
                    showManualCheckResult();
                } else {
                    // Показываем диалог с рейтингом
                    showRatingDialog(finalScore, totalQuestions);
                }
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                btnNext.setEnabled(true);
                Toast.makeText(getContext(), "Ошибка: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createNotificationForCreator(String completionId) {
        String currentUid = firebaseManager.getCurrentUser().getUid();

        firebaseManager.getUserData(currentUid, new FirebaseManager.UserCallback() {
            @Override
            public void onSuccess(User user) {
                Notification notification = new Notification(
                        null,
                        test.getCreatorId(), // Кому
                        "TEST_COMPLETED",
                        test.getTestId(),
                        test.getTitle(),
                        completionId,
                        user.getName() // Кто прошел
                );

                firebaseManager.createNotification(notification, new FirebaseManager.AuthCallback() {
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
                // Игнорируем
            }
        });
    }

    private void showManualCheckResult() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Тест отправлен на проверку")
                .setMessage("Создатель теста проверит ваши ответы и выставит баллы. Вы получите уведомление когда проверка будет завершена.")
                .setPositiveButton("OK", (dialog, which) -> {
                    // Возвращаемся назад
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .setCancelable(false)
                .show();
    }

    private void showRatingDialog(int score, int totalQuestions) {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_test_rating, null);

        TextView tvResult = dialogView.findViewById(R.id.tvResult);
        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);

        double percentage = (double) score / totalQuestions * 100;
        tvResult.setText(String.format("Вы набрали %d из %d (%.1f%%)\n\nОцените тест:",
                score, totalQuestions, percentage));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Отправить", null)
                .setCancelable(false)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                float rating = ratingBar.getRating();
                if (rating == 0) {
                    Toast.makeText(getContext(), "Поставьте оценку", Toast.LENGTH_SHORT).show();
                    return;
                }
                saveRating(rating);
                dialog.dismiss();
                requireActivity().getSupportFragmentManager().popBackStack();
            });
        });

        dialog.show();
    }

    private void saveRating(double rating) {
        // Обновляем рейтинг теста
        test.addCompletion(rating);

        firebaseManager.getDatabase()
                .child("tests")
                .child(test.getTestId())
                .setValue(test);
    }
}