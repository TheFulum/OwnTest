package com.example.owntest.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.owntest.R;
import com.example.owntest.managers.FirebaseManager;
import com.example.owntest.models.Test;
import com.example.owntest.models.TestCompletion;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.HashMap;
import java.util.Map;

public class TestDetailsFragment extends Fragment {

    private ImageView ivTestIcon, btnBack;
    private TextView tvTitle, tvDescription, tvDifficulty, tvQuestionCount;
    private TextView tvCreator, tvRating, tvCompletions;
    private MaterialButton btnStartTest;
    private ProgressBar progressBar;
    private MaterialCardView cardContent;

    private Test test;
    private TestCompletion userCompletion;
    private String testId;
    private FirebaseManager firebaseManager;

    // Флаги состояния
    private boolean alreadyCompleted = false;
    private boolean needsRating = false;

    public static TestDetailsFragment newInstance(String testId) {
        TestDetailsFragment fragment = new TestDetailsFragment();
        Bundle args = new Bundle();
        args.putString("testId", testId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_test_details, container, false);

        initViews(view);
        firebaseManager = FirebaseManager.getInstance();

        testId = getArguments() != null ? getArguments().getString("testId") : null;
        if (testId != null) {
            loadTestDetails();
        }

        setupListeners();

        return view;
    }

    private void initViews(View view) {
        ivTestIcon = view.findViewById(R.id.ivTestIcon);
        btnBack = view.findViewById(R.id.btnBack);
        tvTitle = view.findViewById(R.id.tvTitle);
        tvDescription = view.findViewById(R.id.tvDescription);
        tvDifficulty = view.findViewById(R.id.tvDifficulty);
        tvQuestionCount = view.findViewById(R.id.tvQuestionCount);
        tvCreator = view.findViewById(R.id.tvCreator);
        tvRating = view.findViewById(R.id.tvRating);
        tvCompletions = view.findViewById(R.id.tvCompletions);
        btnStartTest = view.findViewById(R.id.btnStartTest);
        progressBar = view.findViewById(R.id.progressBar);
        cardContent = view.findViewById(R.id.cardContent);
    }

    private void loadTestDetails() {
        progressBar.setVisibility(View.VISIBLE);
        cardContent.setVisibility(View.GONE);

        firebaseManager.getTestById(testId, new FirebaseManager.TestCallback() {
            @Override
            public void onSuccess(Test loadedTest) {
                test = loadedTest;
                displayTestInfo();

                progressBar.setVisibility(View.GONE);
                cardContent.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Ошибка загрузки: " + error, Toast.LENGTH_LONG).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    private void displayTestInfo() {
        if (test == null) return;

        tvTitle.setText(test.getTitle());
        tvDescription.setText(test.getDescription());
        tvDifficulty.setText("Сложность: " + test.getDifficulty());
        tvQuestionCount.setText("Вопросов: " + test.getQuestionCount());
        tvCreator.setText("Автор: @" + test.getCreatorNickname());
        tvRating.setText(String.format("%.1f", test.getAverageRating()));
        tvCompletions.setText(String.valueOf(test.getCompletionsCount()));

        if (test.getIconUrl() != null && !test.getIconUrl().isEmpty()) {
            Glide.with(this)
                    .load(test.getIconUrl())
                    .centerCrop()
                    .into(ivTestIcon);
        }

        // Проверяем наличие прохождения
        String userId = firebaseManager.getCurrentUser().getUid();
        firebaseManager.getTestCompletion(userId, testId, new FirebaseManager.CompletionCallback() {
            @Override
            public void onSuccess(TestCompletion completion) {
                userCompletion = completion;
                alreadyCompleted = true;

                // Проверяем, нужно ли оценить тест (только если проверен вручную)
                needsRating = completion.isChecked() && completion.getUserRating() == 0;

                updateStartButton();
            }

            @Override
            public void onFailure(String error) {
                // Тест ещё не пройден
                alreadyCompleted = false;
                needsRating = false;
                updateStartButton();
            }
        });
    }

    private void updateStartButton() {
        if (alreadyCompleted) {
            if (needsRating) {
                // Приоритет: нужно поставить оценку
                btnStartTest.setText("Оценить тест");
                btnStartTest.setEnabled(true);
                btnStartTest.setOnClickListener(v -> showRatingDialog());
            }
            else if (!test.isAllowMultipleAttempts()) {
                // Запрещено повторное прохождение
                btnStartTest.setText("Уже пройден");
                btnStartTest.setEnabled(false);
            }
            else {
                // Разрешено повторное прохождение
                btnStartTest.setText("Пройти повторно");
                btnStartTest.setEnabled(true);
                btnStartTest.setOnClickListener(v -> startTest());
            }
        } else {
            // Первый раз
            btnStartTest.setText("Начать тест");
            btnStartTest.setEnabled(true);
            btnStartTest.setOnClickListener(v -> startTest());
        }
    }

    private void showRatingDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_test_rating, null);

        TextView tvResult = dialogView.findViewById(R.id.tvResult);
        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);

        tvResult.setText(String.format("Вы набрали %d из %d (%.1f%%)\nОцените тест:",
                userCompletion.getEarnedPoints(),
                userCompletion.getMaxPoints(),
                userCompletion.getPercentage()));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Отправить", null)
                .setNegativeButton("Отмена", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                float rating = ratingBar.getRating();
                if (rating == 0) {
                    Toast.makeText(getContext(), "Поставьте оценку от 1 до 5", Toast.LENGTH_SHORT).show();
                    return;
                }
                saveRating(rating);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void saveRating(float rating) {
        test.addCompletion(rating);
        userCompletion.setUserRating((int) rating);

        Map<String, Object> updates = new HashMap<>();
        updates.put("tests/" + test.getTestId(), test);
        updates.put("completions/" + userCompletion.getCompletionId() + "/userRating", (int) rating);

        firebaseManager.getDatabase().updateChildren(updates)
                .addOnSuccessListener(v -> {
                    Toast.makeText(getContext(), "Спасибо за оценку!", Toast.LENGTH_SHORT).show();
                    btnStartTest.setText("Пройти повторно");
                    btnStartTest.setOnClickListener(v2 -> startTest());
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Ошибка сохранения оценки", Toast.LENGTH_SHORT).show());
    }

    private void startTest() {
        // Дополнительная защита
        if (alreadyCompleted && !test.isAllowMultipleAttempts()) {
            Toast.makeText(getContext(), "Повторное прохождение этого теста запрещено", Toast.LENGTH_LONG).show();
            return;
        }

        TakeTestFragment fragment = TakeTestFragment.newInstance(testId);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        tvCreator.setOnClickListener(v -> {
            if (test != null) {
                UserProfileFragment fragment = UserProfileFragment.newInstance(test.getCreatorId());
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
    }
}