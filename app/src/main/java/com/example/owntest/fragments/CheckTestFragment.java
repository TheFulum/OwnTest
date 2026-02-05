package com.example.owntest.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.owntest.R;
import com.example.owntest.managers.FirebaseManager;
import com.example.owntest.models.Question;
import com.example.owntest.models.Test;
import com.example.owntest.models.TestCompletion;
import com.example.owntest.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CheckTestFragment extends Fragment {

    private TextView tvUserInfo, tvTestInfo, tvCompletedDate;
    private LinearLayout answersContainer;
    private MaterialButton btnSaveCheck;
    private ImageButton btnBack;
    private ProgressBar progressBar;

    private Test test;
    private TestCompletion completion;
    private User user;

    private FirebaseManager firebaseManager;

    public static CheckTestFragment newInstance(String completionId) {
        CheckTestFragment fragment = new CheckTestFragment();
        Bundle args = new Bundle();
        args.putString("completionId", completionId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_check_test, container, false);

        initViews(view);
        firebaseManager = FirebaseManager.getInstance();

        String completionId = getArguments() != null ? getArguments().getString("completionId") : null;
        if (completionId != null) {
            loadCompletion(completionId);
        }

        setupListeners();

        return view;
    }

    private void initViews(View view) {
        tvUserInfo = view.findViewById(R.id.tvUserInfo);
        tvTestInfo = view.findViewById(R.id.tvTestInfo);
        tvCompletedDate = view.findViewById(R.id.tvCompletedDate);
        answersContainer = view.findViewById(R.id.answersContainer);
        btnSaveCheck = view.findViewById(R.id.btnSaveCheck);
        progressBar = view.findViewById(R.id.progressBar);
        btnSaveCheck = view.findViewById(R.id.btnSaveCheck);
        btnBack = view.findViewById(R.id.btnBack);
    }

    private void loadCompletion(String completionId) {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.getDatabase().child("completions").child(completionId).get()
                .addOnSuccessListener(snapshot -> {
                    completion = snapshot.getValue(TestCompletion.class);
                    if (completion != null) {
                        loadTestAndUser();
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Прохождение не найдено", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadTestAndUser() {
        // Загружаем тест
        firebaseManager.getTestById(completion.getTestId(), new FirebaseManager.TestCallback() {
            @Override
            public void onSuccess(Test loadedTest) {
                test = loadedTest;
                loadUser();
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Ошибка загрузки теста", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUser() {
        // Загружаем данные пользователя
        firebaseManager.getUserData(completion.getUserId(), new FirebaseManager.UserCallback() {
            @Override
            public void onSuccess(User loadedUser) {
                user = loadedUser;
                progressBar.setVisibility(View.GONE);
                displayData();
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Ошибка загрузки пользователя", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayData() {
        // Инфо о пользователе
        tvUserInfo.setText("Пользователь: " + user.getName() + " (@" + user.getNickname() + ")");

        // Инфо о тесте
        tvTestInfo.setText("Тест: " + test.getTitle() + "\nМаксимум баллов: " + completion.getMaxPoints());

        // Дата прохождения
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        tvCompletedDate.setText("Пройден: " + sdf.format(new Date(completion.getCompletedDate())));

        // Отображаем вопросы и ответы
        displayAnswers();
    }

    private void displayAnswers() {
        answersContainer.removeAllViews();

        for (int i = 0; i < test.getQuestions().size(); i++) {
            Question question = test.getQuestions().get(i);

            View questionView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_check_question, answersContainer, false);

            TextView tvQuestionNumber = questionView.findViewById(R.id.tvQuestionNumber);
            TextView tvQuestionText = questionView.findViewById(R.id.tvQuestionText);
            TextView tvQuestionType = questionView.findViewById(R.id.tvQuestionType);
            TextView tvUserAnswer = questionView.findViewById(R.id.tvUserAnswer);
            TextView tvCorrectAnswer = questionView.findViewById(R.id.tvCorrectAnswer);
            LinearLayout layoutPoints = questionView.findViewById(R.id.layoutPoints);
            TextView tvMaxPoints = questionView.findViewById(R.id.tvMaxPoints);
            TextInputEditText etEarnedPoints = questionView.findViewById(R.id.etEarnedPoints);

            tvQuestionNumber.setText("Вопрос " + (i + 1));
            tvQuestionText.setText(question.getQuestionText());
            tvMaxPoints.setText("Макс: " + question.getMaxPoints());

            if (question.isTextQuestion()) {
                // Текстовый вопрос
                tvQuestionType.setText("Текстовый ответ");
                tvQuestionType.setVisibility(View.VISIBLE);

                String userAnswer = completion.getTextAnswers().get(question.getQuestionId());
                tvUserAnswer.setText("Ответ:\n" + (userAnswer != null ? userAnswer : "Нет ответа"));

                tvCorrectAnswer.setVisibility(View.GONE);
                layoutPoints.setVisibility(View.VISIBLE);

            } else {
                // Вопрос с выбором
                tvQuestionType.setVisibility(View.GONE);

                Integer userAnswerIndex = completion.getUserAnswers().get(question.getQuestionId());
                if (userAnswerIndex != null && userAnswerIndex < question.getAnswers().size()) {
                    String userAnswerText = question.getAnswers().get(userAnswerIndex).getAnswerText();
                    tvUserAnswer.setText("Ответ: " + userAnswerText);
                } else {
                    tvUserAnswer.setText("Ответ: Нет ответа");
                }

                String correctAnswerText = question.getAnswers().get(question.getCorrectAnswerIndex()).getAnswerText();
                tvCorrectAnswer.setText("Правильный ответ: " + correctAnswerText);
                tvCorrectAnswer.setVisibility(View.VISIBLE);

                // Для choice вопросов тоже можем дать баллы
                layoutPoints.setVisibility(View.VISIBLE);
            }

            answersContainer.addView(questionView);
        }
    }

    private void setupListeners() {
        btnSaveCheck.setOnClickListener(v -> saveCheck());
        btnBack.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });
    }

    private void saveCheck() {
        int totalEarnedPoints = 0;

        // Собираем баллы из всех полей
        for (int i = 0; i < answersContainer.getChildCount(); i++) {
            View questionView = answersContainer.getChildAt(i);
            TextInputEditText etEarnedPoints = questionView.findViewById(R.id.etEarnedPoints);

            String pointsStr = etEarnedPoints.getText().toString().trim();
            if (pointsStr.isEmpty()) {
                Toast.makeText(getContext(), "Заполните все баллы", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int points = Integer.parseInt(pointsStr);
                Question question = test.getQuestions().get(i);

                if (points < 0 || points > question.getMaxPoints()) {
                    Toast.makeText(getContext(), "Баллы должны быть от 0 до " + question.getMaxPoints(),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                totalEarnedPoints += points;
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Некорректное число в вопросе " + (i + 1), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Показываем подтверждение
        showConfirmDialog(totalEarnedPoints);
    }

    private void showConfirmDialog(int totalEarnedPoints) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Подтвердите проверку")
                .setMessage("Выставить " + totalEarnedPoints + " баллов из " + completion.getMaxPoints() + "?")
                .setPositiveButton("Да", (dialog, which) -> {
                    saveCheckToFirebase(totalEarnedPoints);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void saveCheckToFirebase(int totalEarnedPoints) {
        progressBar.setVisibility(View.VISIBLE);
        btnSaveCheck.setEnabled(false);

        firebaseManager.updateCompletionWithPoints(completion.getCompletionId(), totalEarnedPoints,
                new FirebaseManager.AuthCallback() {
                    @Override
                    public void onSuccess(String message) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Проверка сохранена!", Toast.LENGTH_SHORT).show();

                        // Отправляем уведомление только если проверяющий НЕ является тем, кто проходил тест
                        String currentUserId = firebaseManager.getCurrentUser().getUid();
                        if (!currentUserId.equals(completion.getUserId())) {
                            firebaseManager.sendTestCheckedNotification(
                                    completion.getUserId(),
                                    test.getTestId(),
                                    test.getTitle()
                            );
                        }

                        // Возвращаемся назад
                        requireActivity().getSupportFragmentManager().popBackStack();
                    }

                    @Override
                    public void onFailure(String error) {
                        progressBar.setVisibility(View.GONE);
                        btnSaveCheck.setEnabled(true);
                        Toast.makeText(getContext(), "Ошибка: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}