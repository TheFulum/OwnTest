package com.example.owntest.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.owntest.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.owntest.managers.FirebaseManager;

public class RegistrationActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etNickname, etName, etPassword, etConfirmPassword;
    private MaterialButton btnRegister;
    private ProgressBar progressBar;
    private TextView tvLogin;
    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        initViews();
        firebaseManager = FirebaseManager.getInstance();

        btnRegister.setOnClickListener(v -> registerUser());
        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etNickname = findViewById(R.id.etNickname);
        etName = findViewById(R.id.etName);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);
        tvLogin = findViewById(R.id.tvLogin);
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String nickname = etNickname.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

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

        if (TextUtils.isEmpty(nickname)) {
            etNickname.setError("Введите никнейм");
            etNickname.requestFocus();
            return;
        }

        if (nickname.length() < 3) {
            etNickname.setError("Никнейм должен быть не менее 3 символов");
            etNickname.requestFocus();
            return;
        }

        if (!nickname.matches("[a-zA-Z0-9_]+")) {
            etNickname.setError("Никнейм может содержать только буквы, цифры и _");
            etNickname.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(name)) {
            etName.setError("Введите имя");
            etName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Введите пароль");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Пароль должен быть не менее 6 символов");
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Пароли не совпадают");
            etConfirmPassword.requestFocus();
            return;
        }

        // Показываем прогресс
        showLoading(true);

        // Регистрируем пользователя с проверкой уникальности
        firebaseManager.registerUser(email, password, nickname, name, new FirebaseManager.AuthCallback() {
            @Override
            public void onSuccess(String userId) {
                showLoading(false);
                Toast.makeText(RegistrationActivity.this,
                        "Регистрация успешна! Добро пожаловать!",
                        Toast.LENGTH_SHORT).show();
                navigateToMain();
            }

            @Override
            public void onFailure(String error) {
                showLoading(false);

                // Специальная обработка ошибок
                if (error.contains("Никнейм уже занят")) {
                    etNickname.setError(error);
                    etNickname.requestFocus();
                } else if (error.contains("Email уже зарегистрирован")) {
                    etEmail.setError(error);
                    etEmail.requestFocus();
                } else {
                    Toast.makeText(RegistrationActivity.this,
                            "Ошибка: " + error,
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!show);
        btnRegister.setText(show ? "" : "Зарегистрироваться");
    }
}