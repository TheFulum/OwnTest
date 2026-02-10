package com.example.owntest.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class CreateQuestionsFragment extends Fragment {

    private TextView tvProgress;
    private TextInputEditText etQuestion, etQuestionDescription;
    private Spinner spinnerQuestionType;
    private LinearLayout answersContainer;
    private LinearLayout layoutMaxPoints;
    private TextInputEditText etMaxPoints;
    private MaterialButton btnNext, btnPrevious;
    private ProgressBar progressBar;

    private String testId;
    private String title;
    private String description;
    private String iconUrl;
    private String difficulty;
    private int totalQuestions;
    private int answerOptionsCount;
    private boolean isManualCheck;
    private String creatorId;
    private String creatorNickname;

    private int currentQuestionIndex = 0;
    private List<Question> questions;
    private List<TextInputEditText> answerFields;
    private List<CheckBox> answerCheckboxes;

    private FirebaseManager firebaseManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_questions, container, false);

        getDataFromBundle();
        initViews(view);
        firebaseManager = FirebaseManager.getInstance();

        setupQuestionTypeSpinner();

        // Управление видимостью элементов в зависимости от режима проверки
        if (isManualCheck) {
            spinnerQuestionType.setVisibility(View.VISIBLE);
            layoutMaxPoints.setVisibility(View.VISIBLE);
        } else {
            spinnerQuestionType.setVisibility(View.GONE);
            layoutMaxPoints.setVisibility(View.GONE);
            answersContainer.setVisibility(View.VISIBLE);
        }

        questions = new ArrayList<>();
        for (int i = 0; i < totalQuestions; i++) {
            questions.add(new Question());
        }

        setupAnswerFields();
        updateUI();
        setupListeners();

        return view;
    }

    private void getDataFromBundle() {
        if (getArguments() != null) {
            title = getArguments().getString("title");
            description = getArguments().getString("description");
            iconUrl = getArguments().getString("iconUrl");
            difficulty = getArguments().getString("difficulty");
            totalQuestions = getArguments().getInt("questionCount");
            answerOptionsCount = getArguments().getInt("answerOptionsCount");
            isManualCheck = getArguments().getBoolean("isManualCheck", false);
            creatorId = getArguments().getString("creatorId");
            creatorNickname = getArguments().getString("creatorNickname");
        }
    }

    private void initViews(View view) {
        tvProgress = view.findViewById(R.id.tvProgress);
        etQuestion = view.findViewById(R.id.etQuestion);
        etQuestionDescription = view.findViewById(R.id.etQuestionDescription);
        spinnerQuestionType = view.findViewById(R.id.spinnerQuestionType);
        answersContainer = view.findViewById(R.id.answersContainer);
        layoutMaxPoints = view.findViewById(R.id.layoutMaxPoints);
        etMaxPoints = view.findViewById(R.id.etMaxPoints);
        btnNext = view.findViewById(R.id.btnNext);
        btnPrevious = view.findViewById(R.id.btnPrevious);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void setupQuestionTypeSpinner() {
        if (!isManualCheck) {
            return;
        }

        String[] types = {"Выбор ответа", "Текстовый ответ"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                types
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerQuestionType.setAdapter(adapter);

        spinnerQuestionType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                boolean isChoice = position == 0;
                updateAnswerFieldsVisibility(isChoice);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setupAnswerFields() {
        answerFields = new ArrayList<>();
        answerCheckboxes = new ArrayList<>();
        answersContainer.removeAllViews();

        for (int i = 0; i < answerOptionsCount; i++) {
            View answerView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_answer_input, answersContainer, false);

            TextInputEditText etAnswer = answerView.findViewById(R.id.etAnswer);
            CheckBox cbCorrect = answerView.findViewById(R.id.cbCorrect);
            TextView tvAnswerNumber = answerView.findViewById(R.id.tvAnswerNumber);

            tvAnswerNumber.setText("Ответ " + (i + 1));

            final int index = i;
            cbCorrect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    for (int j = 0; j < answerCheckboxes.size(); j++) {
                        if (j != index) {
                            answerCheckboxes.get(j).setChecked(false);
                        }
                    }
                }
            });

            answerFields.add(etAnswer);
            answerCheckboxes.add(cbCorrect);
            answersContainer.addView(answerView);
        }
    }

    private void updateAnswerFieldsVisibility(boolean isChoice) {
        answersContainer.setVisibility(isChoice ? View.VISIBLE : View.GONE);
    }

    private boolean isTextTypeSelected() {
        if (!isManualCheck) {
            return false;
        }
        return spinnerQuestionType.getSelectedItemPosition() == 1;
    }

    private void updateUI() {
        tvProgress.setText("Вопрос " + (currentQuestionIndex + 1) + " из " + totalQuestions);

        Question question = questions.get(currentQuestionIndex);

        etQuestion.setText(question.getQuestionText() != null ? question.getQuestionText() : "");
        etQuestionDescription.setText(question.getQuestionDescription() != null ? question.getQuestionDescription() : "");

        // Тип вопроса и видимость полей
        boolean isTextQuestion = isTextTypeSelected();

        if (isManualCheck) {
            spinnerQuestionType.setSelection(isTextQuestion ? 1 : 0);
        }

        updateAnswerFieldsVisibility(!isTextQuestion);

        // Баллы
        if (isManualCheck) {
            etMaxPoints.setText(question.getMaxPoints() > 0 ? String.valueOf(question.getMaxPoints()) : "1");
        }

        // Ответы
        if (question.getAnswers() != null && !question.getAnswers().isEmpty()) {
            for (int i = 0; i < Math.min(question.getAnswers().size(), answerFields.size()); i++) {
                answerFields.get(i).setText(question.getAnswers().get(i).getAnswerText());
            }

            if (question.getCorrectAnswerIndex() >= 0 && question.getCorrectAnswerIndex() < answerCheckboxes.size()) {
                answerCheckboxes.get(question.getCorrectAnswerIndex()).setChecked(true);
            }
        } else {
            for (TextInputEditText field : answerFields) field.setText("");
            for (CheckBox cb : answerCheckboxes) cb.setChecked(false);
        }

        btnPrevious.setVisibility(currentQuestionIndex > 0 ? View.VISIBLE : View.GONE);
        btnNext.setText(currentQuestionIndex == totalQuestions - 1 ? "Создать тест" : "Далее");
    }

    private void setupListeners() {
        btnNext.setOnClickListener(v -> {
            if (validateCurrentQuestion()) {
                saveCurrentQuestion();

                if (currentQuestionIndex == totalQuestions - 1) {
                    createTest();
                } else {
                    currentQuestionIndex++;
                    updateUI();
                }
            }
        });

        btnPrevious.setOnClickListener(v -> {
            saveCurrentQuestion();
            currentQuestionIndex--;
            updateUI();
        });
    }

    private boolean validateCurrentQuestion() {
        String questionText = etQuestion.getText().toString().trim();

        if (TextUtils.isEmpty(questionText)) {
            etQuestion.setError("Введите вопрос");
            etQuestion.requestFocus();
            return false;
        }

        boolean isTextQuestion = isTextTypeSelected();

        // Проверка вариантов ответа (только для CHOICE)
        if (!isTextQuestion) {
            for (int i = 0; i < answerFields.size(); i++) {
                String answerText = answerFields.get(i).getText().toString().trim();
                if (TextUtils.isEmpty(answerText)) {
                    answerFields.get(i).setError("Введите ответ");
                    answerFields.get(i).requestFocus();
                    return false;
                }
            }

            boolean hasCorrectAnswer = false;
            for (CheckBox cb : answerCheckboxes) {
                if (cb.isChecked()) {
                    hasCorrectAnswer = true;
                    break;
                }
            }
            if (!hasCorrectAnswer) {
                Toast.makeText(getContext(), "Выберите правильный ответ", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        // Проверка баллов (только при ручной проверке)
        if (isManualCheck) {
            String pointsStr = etMaxPoints.getText().toString().trim();
            if (TextUtils.isEmpty(pointsStr)) {
                etMaxPoints.setError("Введите баллы");
                return false;
            }
            try {
                int points = Integer.parseInt(pointsStr);
                if (points < 1) {
                    etMaxPoints.setError("Минимум 1 балл");
                    return false;
                }
            } catch (NumberFormatException e) {
                etMaxPoints.setError("Некорректное число");
                return false;
            }
        }

        return true;
    }

    private void saveCurrentQuestion() {
        String questionText = etQuestion.getText().toString().trim();
        String questionDesc = etQuestionDescription.getText().toString().trim();

        boolean isTextQuestion = isTextTypeSelected();
        String questionType = isTextQuestion ? "TEXT" : "CHOICE";

        int maxPoints = 1;
        if (isManualCheck) {
            try {
                maxPoints = Integer.parseInt(etMaxPoints.getText().toString().trim());
            } catch (NumberFormatException e) {
                maxPoints = 1;
            }
        }

        List<Answer> answers = new ArrayList<>();
        int correctIndex = -1;

        if (!isTextQuestion) {
            for (int i = 0; i < answerFields.size(); i++) {
                String answerText = answerFields.get(i).getText().toString().trim();
                answers.add(new Answer("answer_" + currentQuestionIndex + "_" + i, answerText));

                if (answerCheckboxes.get(i).isChecked()) {
                    correctIndex = i;
                }
            }
        }

        Question question = new Question(
                "question_" + currentQuestionIndex,
                questionText,
                questionDesc,
                answers,
                correctIndex,
                questionType,
                maxPoints
        );

        questions.set(currentQuestionIndex, question);
    }

    private void createTest() {
        progressBar.setVisibility(View.VISIBLE);
        btnNext.setEnabled(false);

        boolean allowMultipleAttempts = getArguments().getBoolean("allowMultipleAttempts", true);

        Test test = new Test(
                null,
                title,
                description,
                iconUrl,
                difficulty,
                totalQuestions,
                answerOptionsCount,
                creatorId,
                creatorNickname,
                isManualCheck,
                allowMultipleAttempts
        );

        test.setQuestions(questions);

        firebaseManager.createTest(test, new FirebaseManager.AuthCallback() {
            @Override
            public void onSuccess(String generatedTestId) {
                if (getContext() == null) return;
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Тест успешно создан!", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
                getParentFragmentManager().popBackStack();
            }

            @Override
            public void onFailure(String error) {
                if (getContext() == null) return;
                progressBar.setVisibility(View.GONE);
                btnNext.setEnabled(true);
                Toast.makeText(getContext(), "Ошибка создания теста: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}