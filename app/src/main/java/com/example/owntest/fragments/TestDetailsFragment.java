package com.example.owntest.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.owntest.R;
import com.example.owntest.managers.FirebaseManager;
import com.example.owntest.models.Test;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class TestDetailsFragment extends Fragment {

    private ImageView ivTestIcon, btnBack;
    private TextView tvTitle, tvDescription, tvDifficulty, tvQuestionCount;
    private TextView tvCreator, tvRating, tvCompletions;
    private MaterialButton btnStartTest;
    private ProgressBar progressBar;
    private MaterialCardView cardContent;

    private Test test;
    private String testId;
    private FirebaseManager firebaseManager;

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
                if (getContext() == null) return;

                test = loadedTest;
                displayTestInfo();

                progressBar.setVisibility(View.GONE);
                cardContent.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(String error) {
                if (getContext() == null) return;

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

        // Загружаем иконку
        if (test.getIconUrl() != null && !test.getIconUrl().isEmpty()) {
            Glide.with(this)
                    .load(test.getIconUrl())
                    .centerCrop()
                    .into(ivTestIcon);
        }

        // Проверяем прошел ли пользователь этот тест
        String userId = firebaseManager.getCurrentUser().getUid();
        firebaseManager.getTestCompletion(userId, testId, new FirebaseManager.CompletionCallback() {
            @Override
            public void onSuccess(com.example.owntest.models.TestCompletion completion) {
                // Тест уже пройден
                btnStartTest.setText("Пройти повторно");
            }

            @Override
            public void onFailure(String error) {
                // Тест не пройден
                btnStartTest.setText("Начать тест");
            }
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        btnStartTest.setOnClickListener(v -> {
            if (test != null) {
                // Открываем фрагмент прохождения теста
                TakeTestFragment fragment = TakeTestFragment.newInstance(testId);
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        tvCreator.setOnClickListener(v -> {
            if (test != null) {
                // Открываем профиль создателя
                UserProfileFragment fragment = UserProfileFragment.newInstance(test.getCreatorId());
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
    }
}