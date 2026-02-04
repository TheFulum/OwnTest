package com.example.owntest.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.owntest.R;
import com.example.owntest.managers.CloudinaryManager;
import com.example.owntest.managers.FirebaseManager;
import com.example.owntest.models.Test;
import com.example.owntest.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

public class CreateTestFragment extends Fragment {

    private ImageView ivTestIcon;
    private TextInputEditText etTitle, etDescription, etQuestionCount;
    private Spinner spinnerDifficulty, spinnerAnswerOptions;
    private MaterialButton btnNext;
    private Uri selectedImageUri;
    private String uploadedImageUrl;

    private FirebaseManager firebaseManager;
    private CloudinaryManager cloudinaryManager;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    // Режим редактирования
    private boolean isEditMode = false;
    private String editingTestId = null;
    private Test editingTest = null;

    public static CreateTestFragment newInstance(String testId, boolean editMode) {
        CreateTestFragment fragment = new CreateTestFragment();
        Bundle args = new Bundle();
        args.putString("testId", testId);
        args.putBoolean("editMode", editMode);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_test, container, false);

        initViews(view);
        setupSpinners();
        setupImagePicker();

        firebaseManager = FirebaseManager.getInstance();
        cloudinaryManager = CloudinaryManager.getInstance();

        // Проверяем режим редактирования
        if (getArguments() != null) {
            isEditMode = getArguments().getBoolean("editMode", false);
            editingTestId = getArguments().getString("testId");
        }

        if (isEditMode && editingTestId != null) {
            loadTestForEditing();
        }

        setupListeners();

        return view;
    }

    private void initViews(View view) {
        ivTestIcon = view.findViewById(R.id.ivTestIcon);
        etTitle = view.findViewById(R.id.etTitle);
        etDescription = view.findViewById(R.id.etDescription);
        etQuestionCount = view.findViewById(R.id.etQuestionCount);
        spinnerDifficulty = view.findViewById(R.id.spinnerDifficulty);
        spinnerAnswerOptions = view.findViewById(R.id.spinnerAnswerOptions);
        btnNext = view.findViewById(R.id.btnNext);
    }

    private void setupSpinners() {
        String[] difficulties = {"Легкий", "Средний", "Тяжелый"};
        ArrayAdapter<String> difficultyAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                difficulties
        );
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDifficulty.setAdapter(difficultyAdapter);

        String[] answerOptions = {"2", "3", "4"};
        ArrayAdapter<String> answerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                answerOptions
        );
        answerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAnswerOptions.setAdapter(answerAdapter);
    }

    private void loadTestForEditing() {
        btnNext.setEnabled(false);
        btnNext.setText("Загрузка...");

        firebaseManager.getTestById(editingTestId, new FirebaseManager.TestCallback() {
            @Override
            public void onSuccess(Test test) {
                if (getContext() == null) return;

                editingTest = test;
                fillFormWithTestData(test);

                btnNext.setEnabled(true);
                btnNext.setText(isEditMode ? "Сохранить изменения" : "Далее");
            }

            @Override
            public void onFailure(String error) {
                if (getContext() == null) return;

                Toast.makeText(getContext(), "Ошибка загрузки теста: " + error, Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    private void fillFormWithTestData(Test test) {
        etTitle.setText(test.getTitle());
        etDescription.setText(test.getDescription());
        etQuestionCount.setText(String.valueOf(test.getQuestionCount()));

        // Устанавливаем сложность
        String[] difficulties = {"Легкий", "Средний", "Тяжелый"};
        for (int i = 0; i < difficulties.length; i++) {
            if (difficulties[i].equals(test.getDifficulty())) {
                spinnerDifficulty.setSelection(i);
                break;
            }
        }

        // Устанавливаем количество ответов
        String answerCount = String.valueOf(test.getAnswerOptionsCount());
        String[] answerOptions = {"2", "3", "4"};
        for (int i = 0; i < answerOptions.length; i++) {
            if (answerOptions[i].equals(answerCount)) {
                spinnerAnswerOptions.setSelection(i);
                break;
            }
        }

        // Загружаем иконку
        uploadedImageUrl = test.getIconUrl();
        if (uploadedImageUrl != null && !uploadedImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(uploadedImageUrl)
                    .centerCrop()
                    .into(ivTestIcon);
        }
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            Glide.with(this)
                                    .load(selectedImageUri)
                                    .centerCrop()
                                    .into(ivTestIcon);
                        }
                    }
                }
        );
    }

    private void setupListeners() {
        ivTestIcon.setOnClickListener(v -> openImagePicker());
        btnNext.setOnClickListener(v -> validateAndProceed());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void validateAndProceed() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String questionCountStr = etQuestionCount.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Введите название теста");
            etTitle.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(description)) {
            etDescription.setError("Введите описание");
            etDescription.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(questionCountStr)) {
            etQuestionCount.setError("Введите количество вопросов");
            etQuestionCount.requestFocus();
            return;
        }

        int questionCount;
        try {
            questionCount = Integer.parseInt(questionCountStr);
            if (questionCount < 1 || questionCount > 50) {
                etQuestionCount.setError("От 1 до 50 вопросов");
                etQuestionCount.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            etQuestionCount.setError("Некорректное число");
            etQuestionCount.requestFocus();
            return;
        }

        String difficulty = spinnerDifficulty.getSelectedItem().toString();
        int answerOptionsCount = Integer.parseInt(spinnerAnswerOptions.getSelectedItem().toString());

        // Если в режиме редактирования и изображение не менялось
        if (isEditMode && selectedImageUri == null && uploadedImageUrl != null) {
            proceedWithExistingImage(title, description, uploadedImageUrl, difficulty, questionCount, answerOptionsCount);
            return;
        }

        // Если нет изображения вообще
        if (selectedImageUri == null && uploadedImageUrl == null) {
            Toast.makeText(requireContext(), "Выберите иконку теста", Toast.LENGTH_SHORT).show();
            return;
        }

        // Загружаем новое изображение
        btnNext.setEnabled(false);
        btnNext.setText("Загрузка...");

        cloudinaryManager.uploadTestIcon(selectedImageUri, "temp_" + System.currentTimeMillis(),
                new CloudinaryManager.UploadCallback() {
                    @Override
                    public void onSuccess(String imageUrl) {
                        if (getActivity() == null) return;

                        uploadedImageUrl = imageUrl;
                        proceedWithExistingImage(title, description, imageUrl, difficulty, questionCount, answerOptionsCount);

                        btnNext.setEnabled(true);
                        btnNext.setText(isEditMode ? "Сохранить изменения" : "Далее");
                    }

                    @Override
                    public void onFailure(String error) {
                        if (getActivity() == null) return;

                        Toast.makeText(requireContext(), "Ошибка загрузки: " + error, Toast.LENGTH_SHORT).show();
                        btnNext.setEnabled(true);
                        btnNext.setText(isEditMode ? "Сохранить изменения" : "Далее");
                    }
                });
    }

    private void proceedWithExistingImage(String title, String description, String iconUrl,
                                          String difficulty, int questionCount, int answerOptionsCount) {
        String uid = firebaseManager.getCurrentUser().getUid();

        firebaseManager.getUserData(uid, new FirebaseManager.UserCallback() {
            @Override
            public void onSuccess(User user) {
                Bundle bundle = new Bundle();
                bundle.putString("title", title);
                bundle.putString("description", description);
                bundle.putString("iconUrl", iconUrl);
                bundle.putString("difficulty", difficulty);
                bundle.putInt("questionCount", questionCount);
                bundle.putInt("answerOptionsCount", answerOptionsCount);
                bundle.putString("creatorId", uid);
                bundle.putString("creatorNickname", user.getNickname());

                // Передаем данные о режиме редактирования
                if (isEditMode) {
                    bundle.putBoolean("editMode", true);
                    bundle.putString("testId", editingTestId);
                    bundle.putSerializable("existingQuestions", editingTest.getQuestions() != null ?
                            new ArrayList<>(editingTest.getQuestions()) : null);
                }

                CreateQuestionsFragment fragment = new CreateQuestionsFragment();
                fragment.setArguments(bundle);

                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(requireContext(), "Ошибка: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}