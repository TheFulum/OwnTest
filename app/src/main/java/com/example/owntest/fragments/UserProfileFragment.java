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
import com.example.owntest.models.User;
import com.google.android.material.card.MaterialCardView;

public class UserProfileFragment extends Fragment {

    private ImageView ivAvatar, btnBack;
    private TextView tvName, tvNickname, tvEmail, tvDaysInApp;
    private ProgressBar progressBar;
    private MaterialCardView cardContent;

    private String userId;
    private FirebaseManager firebaseManager;

    public static UserProfileFragment newInstance(String userId) {
        UserProfileFragment fragment = new UserProfileFragment();
        Bundle args = new Bundle();
        args.putString("userId", userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_profile, container, false);

        initViews(view);
        firebaseManager = FirebaseManager.getInstance();

        userId = getArguments() != null ? getArguments().getString("userId") : null;
        if (userId != null) {
            loadUserProfile();
        }

        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        return view;
    }

    private void initViews(View view) {
        ivAvatar = view.findViewById(R.id.ivAvatar);
        btnBack = view.findViewById(R.id.btnBack);
        tvName = view.findViewById(R.id.tvName);
        tvNickname = view.findViewById(R.id.tvNickname);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvDaysInApp = view.findViewById(R.id.tvDaysInApp);
        progressBar = view.findViewById(R.id.progressBar);
        cardContent = view.findViewById(R.id.cardContent);
    }

    private void loadUserProfile() {
        progressBar.setVisibility(View.VISIBLE);
        cardContent.setVisibility(View.GONE);

        firebaseManager.getUserData(userId, new FirebaseManager.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (getContext() == null) return;

                displayUserInfo(user);

                progressBar.setVisibility(View.GONE);
                cardContent.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(String error) {
                if (getContext() == null) return;

                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Ошибка загрузки профиля: " + error, Toast.LENGTH_LONG).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    private void displayUserInfo(User user) {
        tvName.setText(user.getName());
        tvNickname.setText("@" + user.getNickname());
        tvEmail.setText(user.getEmail());
        tvDaysInApp.setText(user.getDaysInApp() + " дней");

        // Загружаем аватар
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getAvatarUrl())
                    .placeholder(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(ivAvatar);
        }
    }
}