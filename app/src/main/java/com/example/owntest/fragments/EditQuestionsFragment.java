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

public class EditQuestionsFragment extends Fragment {

    private TextView tvProgress;
    private TextInputEditText etQuestion, etQuestionDescription;
    private Spinner spinnerQuestionType;
    private LinearLayout answersContainer;
    private LinearLayout layoutMaxPoints;
    private TextInputEditText etMaxPoints;
    private MaterialButton btnNext, btnPrevious, btnSave;
    private ProgressBar progressBar;

    private Test test;
    private String testId;

    private int currentQuestionIndex = 0;
    private List<Question> questions;
    private List<TextInputEditText> answerFields;
    private List<CheckBox> answerCheckboxes;

    private FirebaseManager firebaseManager;

    public static EditQuestionsFragment newInstance(String testId) {
        EditQuestionsFragment fragment = new EditQuestionsFragment();
        Bundle args = new Bundle();
        args.putString("testId", testId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_questions, container, false);

        initViews(view);
        firebaseManager = FirebaseManager.getInstance();

        testId = getArguments() != null ? getArguments().getString("testId") : null;
        if (testId != null) {
            loadTest();
        }

        return view;
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
        btnSave = view.findViewById(R.id.btnSave);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void loadTest() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.getTestById(testId, new FirebaseManager.TestCallback() {
            @Override
            public void onSuccess(Test loadedTest) {
                if (getContext() == null) return;

                test = loadedTest;
                questions = new ArrayList<>(test.getQuestions());

                setupQuestionTypeSpinner();
                setupAnswerFields();
                updateUI();
                setupListeners();

                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(String error) {
                if (getContext() == null) return;

                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Ошибка: " + error, Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    private void setupQuestionTypeSpinner() {
        if (!test.isManualCheck()) {
            spinnerQuestionType.setVisibility(View.GONE);
            layoutMaxPoints.setVisibility(View.GONE);
            answersContainer.setVisibility(View.VISIBLE);
            return;
        }

        spinnerQuestionType.setVisibility(View.VISIBLE);
        layoutMaxPoints.setVisibility(View.VISIBLE);

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

        for (int i = 0; i < test.getAnswerOptionsCount(); i++) {
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
        if (!test.isManualCheck()) {
            return false;
        }
        return spinnerQuestionType.getSelectedItemPosition() == 1;
    }

    private void updateUI() {
        tvProgress.setText("Вопрос " + (currentQuestionIndex + 1) + " из " + questions.size());

        Question question = questions.get(currentQuestionIndex);

        etQuestion.setText(question.getQuestionText() != null ? question.getQuestionText() : "");
        etQuestionDescription.setText(question.getQuestionDescription() != null ? question.getQuestionDescription() : "");

        // Тип вопроса и видимость полей
        boolean isTextQuestion = question.isTextQuestion();

        if (test.isManualCheck()) {
            spinnerQuestionType.setSelection(isTextQuestion ? 1 : 0);
            etMaxPoints.setText(question.getMaxPoints() > 0 ? String.valueOf(question.getMaxPoints()) : "1");
        }

        updateAnswerFieldsVisibility(!isTextQuestion);

        // Заполняем ответы
        if (question.getAnswers() != null && !question.getAnswers().isEmpty() && !isTextQuestion) {
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
        btnNext.setVisibility(currentQuestionIndex < questions.size() - 1 ? View.VISIBLE : View.GONE);
    }

    private void setupListeners() {
        btnNext.setOnClickListener(v -> {
            if (validateCurrentQuestion()) {
                saveCurrentQuestion();
                currentQuestionIndex++;
                updateUI();
            }
        });

        btnPrevious.setOnClickListener(v -> {
            saveCurrentQuestion();
            currentQuestionIndex--;
            updateUI();
        });

        btnSave.setOnClickListener(v -> {
            if (validateCurrentQuestion()) {
                saveCurrentQuestion();
                saveAllQuestions();
            }
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
        if (test.isManualCheck()) {
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
        if (test.isManualCheck()) {
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

                // Сохраняем существующий ID ответа или создаем новый
                String answerId;
                Question currentQuestion = questions.get(currentQuestionIndex);
                if (currentQuestion.getAnswers() != null &&
                        i < currentQuestion.getAnswers().size() &&
                        currentQuestion.getAnswers().get(i) != null) {
                    answerId = currentQuestion.getAnswers().get(i).getAnswerId();
                } else {
                    answerId = "answer_" + currentQuestionIndex + "_" + i;
                }

                answers.add(new Answer(answerId, answerText));

                if (answerCheckboxes.get(i).isChecked()) {
                    correctIndex = i;
                }
            }
        }

        // Сохраняем существующий ID вопроса
        Question currentQuestion = questions.get(currentQuestionIndex);
        String questionId = currentQuestion.getQuestionId();

        Question question = new Question(
                questionId,
                questionText,
                questionDesc,
                answers,
                correctIndex,
                questionType,
                maxPoints
        );

        questions.set(currentQuestionIndex, question);
    }

    private void saveAllQuestions() {
        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        // Обновляем вопросы в тесте
        test.setQuestions(questions);

        firebaseManager.updateTest(test, new FirebaseManager.AuthCallback() {
            @Override
            public void onSuccess(String result) {
                if (getContext() == null) return;

                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Вопросы обновлены!", Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            }

            @Override
            public void onFailure(String error) {
                if (getContext() == null) return;

                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(getContext(), "Ошибка: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}