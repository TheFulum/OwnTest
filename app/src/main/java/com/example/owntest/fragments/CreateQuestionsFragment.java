package com.example.owntest.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.owntest.R;
import com.example.owntest.activities.MainActivity;
import com.example.owntest.managers.FirebaseManager;
import com.example.owntest.models.Answer;
import com.example.owntest.models.Question;
import com.example.owntest.models.Test;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class CreateQuestionsFragment extends Fragment {

    // Добавьте эти переменные после существующих
    private boolean isEditMode = false;
    private String editingTestId = null;
    private ArrayList<Question> existingQuestions = null;

    private TextView tvProgress;
    private TextInputEditText etQuestion, etQuestionDescription;
    private LinearLayout answersContainer;
    private MaterialButton btnNext, btnPrevious;
    private ProgressBar progressBar;

    private String title;
    private String description;
    private String iconUrl;
    private String difficulty;
    private int totalQuestions;
    private int answerOptionsCount;
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

        questions = new ArrayList<>();
        if (isEditMode && existingQuestions != null && !existingQuestions.isEmpty()) {
            questions.addAll(existingQuestions);
            totalQuestions = questions.size();
        } else {
            for (int i = 0; i < totalQuestions; i++) {
                questions.add(new Question());
            }
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
            creatorId = getArguments().getString("creatorId");
            creatorNickname = getArguments().getString("creatorNickname");

            // Данные для режима редактирования
            isEditMode = getArguments().getBoolean("editMode", false);
            editingTestId = getArguments().getString("testId");
            existingQuestions = (ArrayList<Question>) getArguments().getSerializable("existingQuestions");
        }
    }

    private void initViews(View view) {
        tvProgress = view.findViewById(R.id.tvProgress);
        etQuestion = view.findViewById(R.id.etQuestion);
        etQuestionDescription = view.findViewById(R.id.etQuestionDescription);
        answersContainer = view.findViewById(R.id.answersContainer);
        btnNext = view.findViewById(R.id.btnNext);
        btnPrevious = view.findViewById(R.id.btnPrevious);
        progressBar = view.findViewById(R.id.progressBar);
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
                    // Снимаем все остальные чекбоксы
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

    private void updateUI() {
        tvProgress.setText("Вопрос " + (currentQuestionIndex + 1) + " из " + totalQuestions);

        // Загружаем сохраненные данные вопроса если есть
        Question question = questions.get(currentQuestionIndex);

        if (question.getQuestionText() != null) {
            etQuestion.setText(question.getQuestionText());
        } else {
            etQuestion.setText("");
        }

        if (question.getQuestionDescription() != null) {
            etQuestionDescription.setText(question.getQuestionDescription());
        } else {
            etQuestionDescription.setText("");
        }

        // Загружаем ответы
        if (question.getAnswers() != null && !question.getAnswers().isEmpty()) {
            for (int i = 0; i < question.getAnswers().size() && i < answerFields.size(); i++) {
                answerFields.get(i).setText(question.getAnswers().get(i).getAnswerText());
            }

            if (question.getCorrectAnswerIndex() >= 0 && question.getCorrectAnswerIndex() < answerCheckboxes.size()) {
                answerCheckboxes.get(question.getCorrectAnswerIndex()).setChecked(true);
            }
        } else {
            for (TextInputEditText field : answerFields) {
                field.setText("");
            }
            for (CheckBox cb : answerCheckboxes) {
                cb.setChecked(false);
            }
        }

        // Кнопки навигации
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

        // Проверяем что все ответы заполнены
        for (int i = 0; i < answerFields.size(); i++) {
            String answerText = answerFields.get(i).getText().toString().trim();
            if (TextUtils.isEmpty(answerText)) {
                answerFields.get(i).setError("Введите ответ");
                answerFields.get(i).requestFocus();
                return false;
            }
        }

        // Проверяем что выбран правильный ответ
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

        return true;
    }

    private void saveCurrentQuestion() {
        String questionText = etQuestion.getText().toString().trim();
        String questionDesc = etQuestionDescription.getText().toString().trim();

        List<Answer> answers = new ArrayList<>();
        int correctIndex = -1;

        for (int i = 0; i < answerFields.size(); i++) {
            String answerText = answerFields.get(i).getText().toString().trim();
            answers.add(new Answer("answer_" + currentQuestionIndex + "_" + i, answerText));

            if (answerCheckboxes.get(i).isChecked()) {
                correctIndex = i;
            }
        }

        Question question = new Question(
                "question_" + currentQuestionIndex,
                questionText,
                questionDesc,
                answers,
                correctIndex
        );

        questions.set(currentQuestionIndex, question);
    }

    private void createTest() {
        progressBar.setVisibility(View.VISIBLE);
        btnNext.setEnabled(false);

        Test test = new Test(
                isEditMode ? editingTestId : null,
                title,
                description,
                iconUrl,
                difficulty,
                totalQuestions,
                answerOptionsCount,
                creatorId,
                creatorNickname
        );

        test.setQuestions(questions);

        if (isEditMode) {
            // Обновляем существующий тест
            firebaseManager.updateTest(test, new FirebaseManager.AuthCallback() {
                @Override
                public void onSuccess(String message) {
                    if (getContext() == null) return;

                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Тест успешно обновлен!", Toast.LENGTH_SHORT).show();

                    // Переходим на главный экран
                    openMainActivity();
                }

                @Override
                public void onFailure(String error) {
                    if (getContext() == null) return;

                    progressBar.setVisibility(View.GONE);
                    btnNext.setEnabled(true);
                    Toast.makeText(getContext(), "Ошибка обновления теста: " + error, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // Создаем новый тест
            firebaseManager.createTest(test, new FirebaseManager.AuthCallback() {
                @Override
                public void onSuccess(String testId) {
                    if (getContext() == null) return;

                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Тест успешно создан!", Toast.LENGTH_SHORT).show();

                    // Переходим на главный экран
                    openMainActivity();
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

    /**
     * Метод для перехода на главный экран
     */
    private void openMainActivity() {
        if (getActivity() == null) return;

        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        getActivity().finish(); // закрываем текущую Activity с фрагментом
    }


}