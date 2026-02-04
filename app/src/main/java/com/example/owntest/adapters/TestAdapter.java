package com.example.owntest.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.owntest.R;
import com.example.owntest.managers.FirebaseManager;
import com.example.owntest.models.Test;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class TestAdapter extends RecyclerView.Adapter<TestAdapter.TestViewHolder> {

    private List<Test> tests;
    private OnTestClickListener listener;
    private boolean showCreatorOptions;
    private String currentUserId;

    public interface OnTestClickListener {
        void onTestClick(Test test);
        void onEditClick(Test test);
        void onDeleteClick(Test test);
    }

    public TestAdapter(OnTestClickListener listener) {
        this(listener, false);
    }

    public TestAdapter(OnTestClickListener listener, boolean showCreatorOptions) {
        this.tests = new ArrayList<>();
        this.listener = listener;
        this.showCreatorOptions = showCreatorOptions;

        // Получаем ID текущего пользователя
        FirebaseManager firebaseManager = FirebaseManager.getInstance();
        if (firebaseManager.getCurrentUser() != null) {
            this.currentUserId = firebaseManager.getCurrentUser().getUid();
        }
    }

    @NonNull
    @Override
    public TestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_test, parent, false);
        return new TestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TestViewHolder holder, int position) {
        Test test = tests.get(position);
        holder.bind(test);
    }

    @Override
    public int getItemCount() {
        return tests.size();
    }

    public void setTests(List<Test> tests) {
        this.tests = tests;
        notifyDataSetChanged();
    }

    public void addTests(List<Test> newTests) {
        int startPosition = this.tests.size();
        this.tests.addAll(newTests);
        notifyItemRangeInserted(startPosition, newTests.size());
    }

    public void clearTests() {
        this.tests.clear();
        notifyDataSetChanged();
    }

    public void removeTest(Test test) {
        int position = tests.indexOf(test);
        if (position != -1) {
            tests.remove(position);
            notifyItemRemoved(position);
        }
    }

    class TestViewHolder extends RecyclerView.ViewHolder {

        private ImageView ivTestIcon;
        private TextView tvAverageRating;
        private TextView tvCompletionsCount;
        private TextView tvTestTitle;
        private TextView tvTestDescription;
        private TextView tvDifficulty;
        private TextView tvQuestionCount;
        private TextView tvCreator;
        private ImageButton btnOptions;

        public TestViewHolder(@NonNull View itemView) {
            super(itemView);

            ivTestIcon = itemView.findViewById(R.id.ivTestIcon);
            tvAverageRating = itemView.findViewById(R.id.tvAverageRating);
            tvCompletionsCount = itemView.findViewById(R.id.tvCompletionsCount);
            tvTestTitle = itemView.findViewById(R.id.tvTestTitle);
            tvTestDescription = itemView.findViewById(R.id.tvTestDescription);
            tvDifficulty = itemView.findViewById(R.id.tvDifficulty);
            tvQuestionCount = itemView.findViewById(R.id.tvQuestionCount);
            tvCreator = itemView.findViewById(R.id.tvCreator);
            btnOptions = itemView.findViewById(R.id.btnOptions);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTestClick(tests.get(position));
                }
            });
        }

        public void bind(Test test) {
            Context context = itemView.getContext();

            // Загружаем иконку
            if (test.getIconUrl() != null && !test.getIconUrl().isEmpty()) {
                Glide.with(context)
                        .load(test.getIconUrl())
                        .placeholder(R.drawable.ic_test_placeholder)
                        .centerCrop()
                        .into(ivTestIcon);
            } else {
                ivTestIcon.setImageResource(R.drawable.ic_test_placeholder);
            }

            // Статистика: рейтинг по звёздам
            if (test.getCompletionsCount() > 0) {
                tvAverageRating.setText(String.format("%.1f", test.getAverageRating()));
                tvCompletionsCount.setText(test.getCompletionsCount() + "");
            } else {
                tvAverageRating.setText("0");
                tvCompletionsCount.setText("0");
            }

            // Основная инфа
            tvTestTitle.setText(test.getTitle());
            tvTestDescription.setText(test.getDescription());

            // Сложность
            tvDifficulty.setText(test.getDifficulty());
            setDifficultyColor(test.getDifficulty());

            // Количество вопросов
            tvQuestionCount.setText(getQuestionDeclension(test.getQuestionCount()));

            // Автор
            tvCreator.setText("@" + test.getCreatorNickname());

            // Кнопка опций
            if (showCreatorOptions && currentUserId != null &&
                    currentUserId.equals(test.getCreatorId())) {
                btnOptions.setVisibility(View.VISIBLE);
                btnOptions.setOnClickListener(v -> showOptionsDialog(test));
            } else {
                btnOptions.setVisibility(View.GONE);
            }
        }

        private void setDifficultyColor(String difficulty) {
            int colorRes;
            switch (difficulty) {
                case "Easy":
                case "Легкий":
                    colorRes = R.color.difficulty_easy;
                    break;
                case "Medium":
                case "Средний":
                    colorRes = R.color.difficulty_medium;
                    break;
                case "Hard":
                case "Тяжелый":
                    colorRes = R.color.difficulty_hard;
                    break;
                default:
                    colorRes = R.color.primary;
            }
            tvDifficulty.setBackgroundTintList(
                    itemView.getContext().getColorStateList(colorRes)
            );
        }

        private void showOptionsDialog(Test test) {
            Context context = itemView.getContext();
            BottomSheetDialog dialog = new BottomSheetDialog(context);
            View view = LayoutInflater.from(context).inflate(R.layout.dialog_test_options, null);

            MaterialButton btnEdit = view.findViewById(R.id.btnEdit);
            MaterialButton btnDelete = view.findViewById(R.id.btnDelete);

            btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEditClick(test);
                dialog.dismiss();
            });

            btnDelete.setOnClickListener(v -> {
                dialog.dismiss();
                showDeleteConfirmation(test);
            });

            dialog.setContentView(view);
            dialog.show();
        }

        private void showDeleteConfirmation(Test test) {
            Context context = itemView.getContext();
            new AlertDialog.Builder(context)
                    .setTitle("Удалить тест?")
                    .setMessage("Вы уверены? Это действие нельзя отменить. Все прохождения этого теста также будут удалены.")
                    .setPositiveButton("Удалить", (d, w) -> {
                        if (listener != null) listener.onDeleteClick(test);
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        }
    }

    private String getQuestionDeclension(int count) {
        if (count <= 0) return count + " вопросов";

        int mod10 = count % 10;
        int mod100 = count % 100;

        if (mod10 == 1 && mod100 != 11) {
            return count + " вопрос";
        } else if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) {
            return count + " вопроса";
        } else {
            return count + " вопросов";
        }
    }
}
