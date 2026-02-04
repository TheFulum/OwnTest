package com.example.owntest.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.owntest.R;
import com.example.owntest.activities.LoginActivity;
import com.example.owntest.adapters.TestAdapter;
import com.example.owntest.managers.CloudinaryManager;
import com.example.owntest.managers.FirebaseManager;
import com.example.owntest.models.Test;
import com.example.owntest.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class ProfileFragment extends Fragment {

    private ImageView ivAvatar;
    private TextInputEditText etNickname, etName, etEmail;
    private TextView tvDaysInApp;
    private MaterialButton btnSave, btnLogout;
    private TabLayout tabLayout;
    private RecyclerView rvTests;
    private View layoutEmpty;

    private TestAdapter adapter;
    private FirebaseManager firebaseManager;
    private CloudinaryManager cloudinaryManager;
    private User currentUser;

    private int currentTab = 0; // 0 = мои тесты, 1 = пройденные

    private Uri selectedAvatarUri;
    private ActivityResultLauncher<Intent> avatarPickerLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        initViews(view);
        firebaseManager = FirebaseManager.getInstance();
        cloudinaryManager = CloudinaryManager.getInstance();

        setupAvatarPicker();
        loadUserData();
        setupRecyclerView();
        setupListeners();

        return view;
    }

    private void initViews(View view) {
        ivAvatar = view.findViewById(R.id.ivAvatar);
        etNickname = view.findViewById(R.id.etNickname);
        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        tvDaysInApp = view.findViewById(R.id.tvDaysInApp);
        btnSave = view.findViewById(R.id.btnSave);
        btnLogout = view.findViewById(R.id.btnLogout);
        tabLayout = view.findViewById(R.id.tabLayout);
        rvTests = view.findViewById(R.id.rvTests);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
    }

    private void setupAvatarPicker() {
        avatarPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedAvatarUri = result.getData().getData();
                        if (selectedAvatarUri != null) {
                            // Сразу показываем выбранное изображение
                            Glide.with(this)
                                    .load(selectedAvatarUri)
                                    .placeholder(R.drawable.ic_default_avatar)
                                    .circleCrop()
                                    .into(ivAvatar);

                            // Загружаем в Cloudinary
                            uploadAvatar();
                        }
                    }
                }
        );
    }

    private void setupRecyclerView() {
        // Создаем адаптер с поддержкой редактирования/удаления для вкладки "Мои тесты"
        adapter = new TestAdapter(new TestAdapter.OnTestClickListener() {
            @Override
            public void onTestClick(Test test) {
                openTestDetails(test);
            }

            @Override
            public void onEditClick(Test test) {
                editTest(test);
            }

            @Override
            public void onDeleteClick(Test test) {
                deleteTest(test);
            }
        }, true); // true = показывать опции создателя

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        rvTests.setLayoutManager(layoutManager);
        rvTests.setAdapter(adapter);
    }

    private void loadUserData() {
        String uid = firebaseManager.getCurrentUser().getUid();

        firebaseManager.getUserData(uid, new FirebaseManager.UserCallback() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                displayUserData(user);
                loadTests();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "Ошибка загрузки: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayUserData(User user) {
        if (getContext() == null) return;

        etNickname.setText(user.getNickname());
        etName.setText(user.getName());
        etEmail.setText(user.getEmail());
        tvDaysInApp.setText(String.valueOf(user.getDaysInApp()));

        etEmail.setEnabled(false);
        etEmail.setFocusable(false);

        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getAvatarUrl())
                    .placeholder(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(ivAvatar);
        }
    }

    private void setupListeners() {
        ivAvatar.setOnClickListener(v -> openAvatarPicker());

        btnSave.setOnClickListener(v -> saveProfile());
        btnLogout.setOnClickListener(v -> logout());

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                loadTests();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void openAvatarPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        avatarPickerLauncher.launch(intent);
    }

    private void uploadAvatar() {
        if (selectedAvatarUri == null || currentUser == null) return;

        btnSave.setEnabled(false);
        btnSave.setText("Загрузка аватара...");

        String userId = firebaseManager.getCurrentUser().getUid();

        cloudinaryManager.uploadAvatar(selectedAvatarUri, userId,
                new CloudinaryManager.UploadCallback() {
                    @Override
                    public void onSuccess(String imageUrl) {
                        if (getContext() == null) return;

                        // Обновляем URL аватара в объекте пользователя
                        currentUser.setAvatarUrl(imageUrl);

                        // Сохраняем в Firebase
                        firebaseManager.updateUserProfile(currentUser, new FirebaseManager.AuthCallback() {
                            @Override
                            public void onSuccess(String message) {
                                if (getContext() == null) return;

                                Toast.makeText(getContext(), "Аватар обновлён!", Toast.LENGTH_SHORT).show();
                                btnSave.setEnabled(true);
                                btnSave.setText("Сохранить");

                                // Обновляем header в MainActivity
                                if (getActivity() instanceof com.example.owntest.activities.MainActivity) {
                                    ((com.example.owntest.activities.MainActivity) getActivity()).refreshUserHeader();
                                }
                            }

                            @Override
                            public void onFailure(String error) {
                                if (getContext() == null) return;

                                Toast.makeText(getContext(), "Ошибка сохранения: " + error, Toast.LENGTH_SHORT).show();
                                btnSave.setEnabled(true);
                                btnSave.setText("Сохранить");
                            }
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        if (getContext() == null) return;

                        Toast.makeText(getContext(), "Ошибка загрузки: " + error, Toast.LENGTH_SHORT).show();
                        btnSave.setEnabled(true);
                        btnSave.setText("Сохранить");
                    }
                });
    }

    private void loadTests() {
        if (currentUser == null) return;

        String uid = firebaseManager.getCurrentUser().getUid();

        if (currentTab == 0) {
            loadMyTests(uid);
        } else {
            loadCompletedTests(uid);
        }
    }

    private void loadMyTests(String uid) {
        firebaseManager.getUserCreatedTests(uid, new FirebaseManager.TestListCallback() {
            @Override
            public void onSuccess(List<Test> tests) {
                if (getContext() == null) return;

                adapter.setTests(tests);
                updateEmptyState(tests.isEmpty());
            }

            @Override
            public void onFailure(String error) {
                if (getContext() == null) return;

                Toast.makeText(getContext(), "Ошибка: " + error, Toast.LENGTH_SHORT).show();
                updateEmptyState(true);
            }
        });
    }

    private void loadCompletedTests(String uid) {
        firebaseManager.getUserCompletedTests(uid, new FirebaseManager.TestListCallback() {
            @Override
            public void onSuccess(List<Test> tests) {
                if (getContext() == null) return;

                adapter.setTests(tests);
                updateEmptyState(tests.isEmpty());
            }

            @Override
            public void onFailure(String error) {
                if (getContext() == null) return;

                Toast.makeText(getContext(), "Ошибка: " + error, Toast.LENGTH_SHORT).show();
                updateEmptyState(true);
            }
        });
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvTests.setVisibility(View.GONE);

            TextView tvEmpty = layoutEmpty.findViewById(R.id.tvEmpty);
            if (currentTab == 0) {
                tvEmpty.setText("Вы еще не создали ни одного теста");
            } else {
                tvEmpty.setText("Вы еще не прошли ни одного теста");
            }
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvTests.setVisibility(View.VISIBLE);
        }
    }

    private void saveProfile() {
        String newNickname = etNickname.getText().toString().trim();
        String newName = etName.getText().toString().trim();

        if (newNickname.isEmpty()) {
            etNickname.setError("Введите никнейм");
            return;
        }

        if (newNickname.length() < 3) {
            etNickname.setError("Никнейм должен быть не менее 3 символов");
            return;
        }

        if (newName.isEmpty()) {
            etName.setError("Введите имя");
            return;
        }

        currentUser.setNickname(newNickname);
        currentUser.setName(newName);

        firebaseManager.updateUserProfile(currentUser, new FirebaseManager.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(getContext(), "Профиль обновлен!", Toast.LENGTH_SHORT).show();

                if (getActivity() instanceof com.example.owntest.activities.MainActivity) {
                    ((com.example.owntest.activities.MainActivity) getActivity()).refreshUserHeader();
                }
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "Ошибка: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logout() {
        firebaseManager.logout();
        Toast.makeText(getContext(), "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void openTestDetails(Test test) {
        TestDetailsFragment fragment = TestDetailsFragment.newInstance(test.getTestId());

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    // ========================= НОВЫЕ МЕТОДЫ =========================

    private void editTest(Test test) {
        CreateTestFragment fragment = CreateTestFragment.newInstance(test.getTestId(), true);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void deleteTest(Test test) {
        // Показываем диалог подтверждения уже в адаптере, просто выполняем удаление
        firebaseManager.deleteTest(test.getTestId(), new FirebaseManager.AuthCallback() {
            @Override
            public void onSuccess(String result) {
                if (getContext() == null) return;

                Toast.makeText(getContext(), "Тест успешно удален", Toast.LENGTH_SHORT).show();

                // Удаляем тест из адаптера
                adapter.removeTest(test);

                // Перезагружаем список тестов
                loadTests();
            }

            @Override
            public void onFailure(String error) {
                if (getContext() == null) return;

                Toast.makeText(getContext(), "Ошибка удаления: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentUser != null) {
            loadTests();
        }
    }
}