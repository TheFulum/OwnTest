package com.example.owntest.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.owntest.R;
import com.example.owntest.managers.FirebaseManager;
import com.example.owntest.models.Answer;
import com.example.owntest.models.Question;
import com.example.owntest.models.Test;
import com.example.owntest.models.TestCompletion;
import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.Map;

public class TakeTestFragment extends Fragment {

    private TextView tvTestTitle, tvProgress, tvQuestionText, tvQuestionDescription;
    private RadioGroup rgAnswers;
    private MaterialButton btnNext, btnPrevious;
    private ProgressBar progressBar;
    private LinearLayout questionContainer;

    private Test test;
    private int currentQuestionIndex = 0;
    private Map<String, Integer> userAnswers;
    private FirebaseManager firebaseManager;

    private TestCompletion existingCompletion; // Если есть незавершенное прохождение
    private String completionId;

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
        userAnswers = new HashMap<>();

        String testId = getArguments() != null ? getArguments().getString("testId") : null;
        if (testId != null) {
            loadTest(testId);
        }

        setupListeners();

        return view;
    }

    private void initViews(View view) {
        tvTestTitle = view.findViewById(R.id.tvTestTitle);
        tvProgress = view.findViewById(R.id.tvProgress);
        tvQuestionText = view.findViewById(R.id.tvQuestionText);
        tvQuestionDescription = view.findViewById(R.id.tvQuestionDescription);
        rgAnswers = view.findViewById(R.id.rgAnswers);
        btnNext = view.findViewById(R.id.btnNext);
        btnPrevious = view.findViewById(R.id.btnPrevious);
        progressBar = view.findViewById(R.id.progressBar);
        questionContainer = view.findViewById(R.id.questionContainer);
    }

    private void loadTest(String testId) {
        progressBar.setVisibility(View.VISIBLE);
        questionContainer.setVisibility(View.GONE);

        firebaseManager.getTestById(testId, new FirebaseManager.TestCallback() {
            @Override
            public void onSuccess(Test loadedTest) {
                if (getContext() == null) return;

                test = loadedTest;
                tvTestTitle.setText(test.getTitle());

                // Проверяем есть ли незавершенное прохождение
                checkExistingProgress();
            }

            @Override
            public void onFailure(String error) {
                if (getContext() == null) return;

                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Ошибка загрузки теста: " + error, Toast.LENGTH_LONG).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    private void checkExistingProgress() {
        String userId = firebaseManager.getCurrentUser().getUid();

        firebaseManager.getTestCompletion(userId, test.getTestId(), new FirebaseManager.CompletionCallback() {
            @Override
            public void onSuccess(TestCompletion completion) {
                if (getContext() == null) return;

                // Если тест не завершен - восстанавливаем прогресс
                if (!completion.isCompleted()) {
                    existingCompletion = completion;
                    completionId = completion.getCompletionId();
                    currentQuestionIndex = completion.getCurrentQuestionIndex();
                    userAnswers = completion.getUserAnswers() != null ?
                            new HashMap<>(completion.getUserAnswers()) : new HashMap<>();

                    Toast.makeText(getContext(), "Восстановлен прогресс", Toast.LENGTH_SHORT).show();
                }

                progressBar.setVisibility(View.GONE);
                questionContainer.setVisibility(View.VISIBLE);
                updateQuestionUI();
            }

            @Override
            public void onFailure(String error) {
                // Нет прохождения - начинаем с начала
                progressBar.setVisibility(View.GONE);
                questionContainer.setVisibility(View.VISIBLE);
                updateQuestionUI();
            }
        });
    }

    private void setupListeners() {
        btnNext.setOnClickListener(v -> {
            if (saveCurrentAnswer()) {
                if (currentQuestionIndex < test.getQuestions().size() - 1) {
                    currentQuestionIndex++;
                    saveProgress(); // Сохраняем прогресс при переходе
                    updateQuestionUI();
                } else {
                    finishTest();
                }
            }
        });

        btnPrevious.setOnClickListener(v -> {
            saveCurrentAnswer();
            currentQuestionIndex--;
            saveProgress(); // Сохраняем прогресс
            updateQuestionUI();
        });
    }

    private void updateQuestionUI() {
        if (test == null || test.getQuestions() == null || test.getQuestions().isEmpty()) {
            return;
        }

        Question question = test.getQuestions().get(currentQuestionIndex);

        tvProgress.setText("Вопрос " + (currentQuestionIndex + 1) + " из " + test.getQuestions().size());
        tvQuestionText.setText(question.getQuestionText());

        if (question.getQuestionDescription() != null && !question.getQuestionDescription().isEmpty()) {
            tvQuestionDescription.setVisibility(View.VISIBLE);
            tvQuestionDescription.setText(question.getQuestionDescription());
        } else {
            tvQuestionDescription.setVisibility(View.GONE);
        }

        rgAnswers.removeAllViews();
        if (question.getAnswers() != null) {
            for (int i = 0; i < question.getAnswers().size(); i++) {
                Answer answer = question.getAnswers().get(i);
                RadioButton radioButton = new RadioButton(getContext());
                radioButton.setText(answer.getAnswerText());
                radioButton.setId(i);
                radioButton.setTextSize(16);
                radioButton.setPadding(16, 16, 16, 16);

                rgAnswers.addView(radioButton);
            }
        }

        Integer savedAnswer = userAnswers.get(question.getQuestionId());
        if (savedAnswer != null && savedAnswer < rgAnswers.getChildCount()) {
            ((RadioButton) rgAnswers.getChildAt(savedAnswer)).setChecked(true);
        }

        btnPrevious.setVisibility(currentQuestionIndex > 0 ? View.VISIBLE : View.GONE);
        btnNext.setText(currentQuestionIndex == test.getQuestions().size() - 1 ? "Завершить тест" : "Далее");
    }

    private boolean saveCurrentAnswer() {
        int selectedId = rgAnswers.getCheckedRadioButtonId();

        if (selectedId == -1) {
            Toast.makeText(getContext(), "Выберите ответ", Toast.LENGTH_SHORT).show();
            return false;
        }

        Question currentQuestion = test.getQuestions().get(currentQuestionIndex);
        userAnswers.put(currentQuestion.getQuestionId(), selectedId);

        return true;
    }

    // Автосохранение прогресса
    private void saveProgress() {
        String userId = firebaseManager.getCurrentUser().getUid();

        TestCompletion progress = new TestCompletion();
        progress.setCompletionId(completionId != null ? completionId :
                firebaseManager.getDatabase().child("completions").push().getKey());
        progress.setUserId(userId);
        progress.setTestId(test.getTestId());
        progress.setUserAnswers(userAnswers);
        progress.setCurrentQuestionIndex(currentQuestionIndex);
        progress.setCompleted(false); // Не завершен
        progress.setTotalQuestions(test.getQuestions().size());
        progress.setLastUpdated(System.currentTimeMillis());

        completionId = progress.getCompletionId();

        firebaseManager.saveTestProgress(progress, new FirebaseManager.AuthCallback() {
            @Override
            public void onSuccess(String id) {
                // Прогресс сохранен
            }

            @Override
            public void onFailure(String error) {
                // Игнорируем ошибки автосохранения
            }
        });
    }

    private void finishTest() {
        progressBar.setVisibility(View.VISIBLE);
        btnNext.setEnabled(false);
        btnPrevious.setEnabled(false);

        // Подсчитываем результат
        int correctAnswers = 0;
        for (int i = 0; i < test.getQuestions().size(); i++) {
            Question question = test.getQuestions().get(i);
            Integer userAnswer = userAnswers.get(question.getQuestionId());

            if (userAnswer != null && userAnswer == question.getCorrectAnswerIndex()) {
                correctAnswers++;
            }
        }

        int finalCorrectAnswers = correctAnswers;

        // Показываем диалог для выбора рейтинга
        showRatingDialog(finalCorrectAnswers);
    }

    private void showRatingDialog(int correctAnswers) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_test_rating, null);

        TextView tvResult = dialogView.findViewById(R.id.tvResult);
        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);

        double percentage = (double) correctAnswers / test.getQuestions().size() * 100;
        tvResult.setText(String.format(
                "Правильных ответов: %d из %d\nРезультат: %.1f%%\n\nОцените тест:",
                correctAnswers, test.getQuestions().size(), percentage
        ));

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Тест завершен!")
                .setView(dialogView)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    int rating = (int) ratingBar.getRating();
                    if (rating == 0) {
                        Toast.makeText(getContext(), "Поставьте оценку", Toast.LENGTH_SHORT).show();
                        showRatingDialog(correctAnswers); // Показываем снова
                        return;
                    }
                    saveCompletedTest(correctAnswers, rating);
                })
                .setCancelable(false)
                .show();
    }

    private void saveCompletedTest(int correctAnswers, int rating) {
        String userId = firebaseManager.getCurrentUser().getUid();

        TestCompletion completion = new TestCompletion(
                completionId != null ? completionId : null,
                userId,
                test.getTestId(),
                correctAnswers,
                test.getQuestions().size(),
                userAnswers
        );

        completion.setCompleted(true);
        completion.setUserRating(rating);
        completion.setCompletedDate(System.currentTimeMillis());

        firebaseManager.saveTestCompletion(completion, new FirebaseManager.AuthCallback() {
            @Override
            public void onSuccess(String id) {
                if (getContext() == null) return;

                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Результат сохранен!", Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            }

            @Override
            public void onFailure(String error) {
                if (getContext() == null) return;

                progressBar.setVisibility(View.GONE);
                btnNext.setEnabled(true);
                btnPrevious.setEnabled(true);
                Toast.makeText(getContext(), "Ошибка сохранения: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}