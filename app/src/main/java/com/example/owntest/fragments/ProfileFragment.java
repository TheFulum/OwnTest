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
import com.example.owntest.adapters.CompletedTestAdapter;
import com.example.owntest.adapters.TestAdapter;
import com.example.owntest.managers.CloudinaryManager;
import com.example.owntest.managers.FirebaseManager;
import com.example.owntest.models.Test;
import com.example.owntest.models.TestCompletion;
import com.example.owntest.models.TestWithCompletion;
import com.example.owntest.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;

import java.util.Collections;
import java.util.List;

public class ProfileFragment extends Fragment {

    private ImageView ivAvatar;
    private TextInputEditText etNickname, etName, etEmail;
    private TextView tvDaysInApp;
    private MaterialButton btnSave, btnLogout;
    private View btnNotifications;
    private TextView tvNotificationBadge;
    private TabLayout tabLayout;
    private RecyclerView rvTests;
    private View layoutEmpty;

    private FirebaseManager firebaseManager;
    private CloudinaryManager cloudinaryManager;
    private User currentUser;

    private int currentTab = 0;
    private TestAdapter createdTestsAdapter;
    private CompletedTestAdapter completedTestsAdapter;

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
        loadUnreadCount();

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
        btnNotifications = view.findViewById(R.id.btnNotifications);
        tvNotificationBadge = view.findViewById(R.id.tvNotificationBadge);
    }

    private void setupAvatarPicker() {
        avatarPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedAvatarUri = result.getData().getData();
                        if (selectedAvatarUri != null) {
                            Glide.with(this)
                                    .load(selectedAvatarUri)
                                    .placeholder(R.drawable.ic_default_avatar)
                                    .circleCrop()
                                    .into(ivAvatar);

                            uploadAvatar();
                        }
                    }
                }
        );
    }

    private void setupRecyclerView() {
        rvTests.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Адаптер для созданных тестов
        createdTestsAdapter = new TestAdapter(new TestAdapter.OnTestClickListener() {
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
        }, true);

        // Адаптер для пройденных тестов
        completedTestsAdapter = new CompletedTestAdapter((test, completion) -> {
            openCompletedTestDetails(test, completion);
        });
    }

    private void loadUserData() {
        FirebaseUser user = firebaseManager.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

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
        btnNotifications.setOnClickListener(v -> openNotifications());

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

                        currentUser.setAvatarUrl(imageUrl);

                        firebaseManager.updateUserProfile(currentUser, new FirebaseManager.AuthCallback() {
                            @Override
                            public void onSuccess(String message) {
                                if (getContext() == null) return;

                                Toast.makeText(getContext(), "Аватар обновлён!", Toast.LENGTH_SHORT).show();
                                btnSave.setEnabled(true);
                                btnSave.setText("Сохранить");

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
            // Созданные тесты
            rvTests.setAdapter(createdTestsAdapter);
            loadMyTests(uid);
        } else {
            // Пройденные тесты
            rvTests.setAdapter(completedTestsAdapter);
            loadCompletedTestsWithStats(uid);
        }
    }

    private void loadMyTests(String uid) {
        firebaseManager.getUserCreatedTests(uid, new FirebaseManager.TestListCallback() {
            @Override
            public void onSuccess(List<Test> tests) {
                if (getContext() == null) return;

                // Сортируем по дате (новые сверху)
                Collections.sort(tests, (t1, t2) -> Long.compare(t2.getCreatedDate(), t1.getCreatedDate()));

                createdTestsAdapter.setTests(tests);
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

                // Сортируем по дате (новые сверху)
                Collections.sort(tests, (t1, t2) -> Long.compare(t2.getCreatedDate(), t1.getCreatedDate()));

                createdTestsAdapter.setTests(tests);
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

    private void editTest(Test test) {
        // Открываем фрагмент редактирования теста
        EditTestFragment fragment = EditTestFragment.newInstance(test.getTestId());
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void deleteTest(Test test) {
        firebaseManager.deleteTest(test.getTestId(), new FirebaseManager.AuthCallback() {
            @Override
            public void onSuccess(String result) {
                if (getContext() == null) return;

                Toast.makeText(getContext(), "Тест успешно удален", Toast.LENGTH_SHORT).show();
                createdTestsAdapter.removeTest(test);
                loadTests();
            }

            @Override
            public void onFailure(String error) {
                if (getContext() == null) return;

                Toast.makeText(getContext(), "Ошибка удаления: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openNotifications() {
        NotificationsFragment fragment = new NotificationsFragment();
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void loadUnreadCount() {
        if (firebaseManager.getCurrentUser() == null) return;
        String uid = firebaseManager.getCurrentUser().getUid();

        firebaseManager.getUnreadNotificationsCount(uid, new FirebaseManager.UnreadCountCallback() {
            @Override
            public void onSuccess(int count) {
                if (getContext() == null) return;
                updateNotificationBadge(count);
            }

            @Override
            public void onFailure(String error) {
                // Игнорируем
            }
        });
    }

    private void loadCompletedTestsWithStats(String uid) {
        firebaseManager.getUserCompletedTestsWithCompletions(uid,
                new FirebaseManager.TestWithCompletionListCallback() {
                    @Override
                    public void onSuccess(List<TestWithCompletion> tests) {
                        if (getContext() == null) return;

                        // Сортируем по дате (новые сверху)
                        Collections.sort(tests, (t1, t2) ->
                                Long.compare(t2.getTest().getCreatedDate(),
                                        t1.getTest().getCreatedDate()));

                        completedTestsAdapter.setTests(tests);
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

    private void openCompletedTestDetails(Test test, TestCompletion completion) {
        // Открываем детали с возможностью оценить (если проверено и еще не оценено)
        TestDetailsFragment fragment = TestDetailsFragment.newInstance(test.getTestId());
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void updateNotificationBadge(int count) {
        if (count > 0) {
            tvNotificationBadge.setText(String.valueOf(count));
            tvNotificationBadge.setVisibility(View.VISIBLE);
        } else {
            tvNotificationBadge.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentUser != null) {
            loadTests();
            loadUnreadCount(); // Обновляем счетчик при возврате
        }
    }
}