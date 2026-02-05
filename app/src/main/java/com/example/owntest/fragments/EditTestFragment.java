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
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class EditTestFragment extends Fragment {

    private ImageView ivTestIcon, btnBack;
    private TextInputEditText etTitle, etDescription;
    private Spinner spinnerDifficulty;
    private CheckBox cbMultipleAttempts;
    private MaterialButton btnSave;
    private ProgressBar progressBar;

    private Uri selectedImageUri;
    private String currentIconUrl;
    private Test test;
    private String testId;

    private FirebaseManager firebaseManager;
    private CloudinaryManager cloudinaryManager;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    public static EditTestFragment newInstance(String testId) {
        EditTestFragment fragment = new EditTestFragment();
        Bundle args = new Bundle();
        args.putString("testId", testId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_test, container, false);

        initViews(view);
        setupSpinners();
        setupImagePicker();
        setupListeners();

        firebaseManager = FirebaseManager.getInstance();
        cloudinaryManager = CloudinaryManager.getInstance();

        testId = getArguments() != null ? getArguments().getString("testId") : null;
        if (testId != null) {
            loadTest();
        }

        return view;
    }

    private void initViews(View view) {
        ivTestIcon = view.findViewById(R.id.ivTestIcon);
        btnBack = view.findViewById(R.id.btnBack);
        etTitle = view.findViewById(R.id.etTitle);
        etDescription = view.findViewById(R.id.etDescription);
        spinnerDifficulty = view.findViewById(R.id.spinnerDifficulty);
        cbMultipleAttempts = view.findViewById(R.id.cbMultipleAttempts);
        btnSave = view.findViewById(R.id.btnSave);
        progressBar = view.findViewById(R.id.progressBar);
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
        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        btnSave.setOnClickListener(v -> validateAndSave());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void loadTest() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.getTestById(testId, new FirebaseManager.TestCallback() {
            @Override
            public void onSuccess(Test loadedTest) {
                if (getContext() == null) return;

                test = loadedTest;
                displayTestData();
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

    private void displayTestData() {
        etTitle.setText(test.getTitle());
        etDescription.setText(test.getDescription());
        cbMultipleAttempts.setChecked(test.isAllowMultipleAttempts());

        // Устанавливаем сложность
        String[] difficulties = {"Легкий", "Средний", "Тяжелый"};
        for (int i = 0; i < difficulties.length; i++) {
            if (difficulties[i].equals(test.getDifficulty())) {
                spinnerDifficulty.setSelection(i);
                break;
            }
        }

        // Загружаем текущую иконку
        currentIconUrl = test.getIconUrl();
        if (currentIconUrl != null && !currentIconUrl.isEmpty()) {
            Glide.with(this)
                    .load(currentIconUrl)
                    .centerCrop()
                    .into(ivTestIcon);
        }
    }

    private void validateAndSave() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

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

        String difficulty = spinnerDifficulty.getSelectedItem().toString();
        boolean allowMultipleAttempts = cbMultipleAttempts.isChecked();

        // Если выбрана новая иконка, загружаем её
        if (selectedImageUri != null) {
            uploadNewIcon(title, description, difficulty, allowMultipleAttempts);
        } else {
            // Используем старую иконку
            saveTest(title, description, currentIconUrl, difficulty, allowMultipleAttempts);
        }
    }

    private void uploadNewIcon(String title, String description, String difficulty, boolean allowMultipleAttempts) {
        btnSave.setEnabled(false);
        btnSave.setText("Загрузка...");

        cloudinaryManager.uploadTestIcon(selectedImageUri, "test_" + testId,
                new CloudinaryManager.UploadCallback() {
                    @Override
                    public void onSuccess(String imageUrl) {
                        if (getActivity() == null) return;

                        saveTest(title, description, imageUrl, difficulty, allowMultipleAttempts);
                    }

                    @Override
                    public void onFailure(String error) {
                        if (getActivity() == null) return;

                        Toast.makeText(requireContext(), "Ошибка загрузки: " + error, Toast.LENGTH_SHORT).show();
                        btnSave.setEnabled(true);
                        btnSave.setText("Сохранить");
                    }
                });
    }

    private void saveTest(String title, String description, String iconUrl, String difficulty, boolean allowMultipleAttempts) {
        btnSave.setEnabled(false);
        btnSave.setText("Сохранение...");

        // Обновляем данные теста
        test.setTitle(title);
        test.setDescription(description);
        test.setIconUrl(iconUrl);
        test.setDifficulty(difficulty);
        test.setAllowMultipleAttempts(allowMultipleAttempts);

        firebaseManager.updateTest(test, new FirebaseManager.AuthCallback() {
            @Override
            public void onSuccess(String result) {
                if (getContext() == null) return;

                Toast.makeText(getContext(), "Тест обновлен!", Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            }

            @Override
            public void onFailure(String error) {
                if (getContext() == null) return;

                Toast.makeText(getContext(), "Ошибка: " + error, Toast.LENGTH_SHORT).show();
                btnSave.setEnabled(true);
                btnSave.setText("Сохранить");
            }
        });
    }
}