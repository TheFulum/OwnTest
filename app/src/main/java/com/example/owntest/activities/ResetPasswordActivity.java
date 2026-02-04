package com.example.owntest.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import android.widget.ProgressBar;

import com.example.owntest.R;
import com.example.owntest.managers.FirebaseManager;

public class ResetPasswordActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextInputEditText etEmail;
    private MaterialButton btnResetPassword;
    private ProgressBar progressBar;
    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        initViews();
        firebaseManager = FirebaseManager.getInstance();

        btnBack.setOnClickListener(v -> finish());
        btnResetPassword.setOnClickListener(v -> resetPassword());
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        etEmail = findViewById(R.id.etEmail);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        progressBar = findViewById(R.id.progressBar);
    }

    private void resetPassword() {
        String email = etEmail.getText().toString().trim();

        // Валидация
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Введите email");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Введите корректный email");
            etEmail.requestFocus();
            return;
        }

        // Показываем прогресс
        showLoading(true);

        firebaseManager.resetPassword(email, new FirebaseManager.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                showLoading(false);
                Toast.makeText(ResetPasswordActivity.this,
                        "Ссылка для восстановления отправлена на " + email,
                        Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onFailure(String error) {
                showLoading(false);
                Toast.makeText(ResetPasswordActivity.this, "Ошибка: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnResetPassword.setEnabled(!show);
        btnResetPassword.setText(show ? "" : "Отправить ссылку");
    }
}