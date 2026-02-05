package com.example.owntest.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.owntest.R;
import com.example.owntest.adapters.NotificationAdapter;
import com.example.owntest.managers.FirebaseManager;
import com.example.owntest.models.Notification;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private RecyclerView rvNotifications;
    private ProgressBar progressBar;
    private View layoutEmpty;
    private MaterialButton btnMarkAllRead;
    private ImageButton btnBack;

    private NotificationAdapter adapter;
    private FirebaseManager firebaseManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        initViews(view);
        setupRecyclerView();
        setupListeners();

        firebaseManager = FirebaseManager.getInstance();
        loadNotifications();

        return view;
    }

    private void initViews(View view) {
        rvNotifications = view.findViewById(R.id.rvNotifications);
        progressBar = view.findViewById(R.id.progressBar);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        btnMarkAllRead = view.findViewById(R.id.btnMarkAllRead);
        btnBack = view.findViewById(R.id.btnBack);
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(notification -> {
            // Клик по уведомлению
            handleNotificationClick(notification);
        });

        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        rvNotifications.setAdapter(adapter);
    }

    private void setupListeners() {
        btnMarkAllRead.setOnClickListener(v -> markAllAsRead());
        btnBack.setOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
        });
    }

    private void loadNotifications() {
        showLoading(true);

        String userId = firebaseManager.getCurrentUser().getUid();

        firebaseManager.getUserNotifications(userId, new FirebaseManager.NotificationListCallback() {
            @Override
            public void onSuccess(List<Notification> notifications) {
                if (getContext() == null) return;

                adapter.setNotifications(notifications);
                updateEmptyState(notifications.isEmpty());
                showLoading(false);
            }

            @Override
            public void onFailure(String error) {
                if (getContext() == null) return;

                Toast.makeText(getContext(), "Ошибка: " + error, Toast.LENGTH_SHORT).show();
                showLoading(false);
            }
        });
    }

    private void handleNotificationClick(Notification notification) {
        // Помечаем как прочитанное
        if (!notification.isRead()) {
            markAsRead(notification);
        }

        // Открываем соответствующий экран
        if ("TEST_COMPLETED".equals(notification.getType())) {
            // Открыть CheckTestFragment
            CheckTestFragment fragment = CheckTestFragment.newInstance(notification.getCompletionId());
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();

        } else if ("TEST_CHECKED".equals(notification.getType())) {
            // Открыть детали теста или результат
            TestDetailsFragment fragment = TestDetailsFragment.newInstance(notification.getTestId());
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void markAsRead(Notification notification) {
        String userId = firebaseManager.getCurrentUser().getUid();

        firebaseManager.markNotificationAsRead(userId, notification.getNotificationId(),
                new FirebaseManager.AuthCallback() {
                    @Override
                    public void onSuccess(String message) {
                        // Перезагружаем список уведомлений из базы данных
                        loadNotifications();
                    }

                    @Override
                    public void onFailure(String error) {
                        // Игнорируем ошибку
                    }
                });
    }

    private void markAllAsRead() {
        String userId = firebaseManager.getCurrentUser().getUid();

        firebaseManager.markAllNotificationsAsRead(userId, new FirebaseManager.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(getContext(), "Все уведомления прочитаны", Toast.LENGTH_SHORT).show();
                loadNotifications(); // Перезагружаем
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "Ошибка: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvNotifications.setVisibility(View.GONE);
            btnMarkAllRead.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvNotifications.setVisibility(View.VISIBLE);
            btnMarkAllRead.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}