package com.example.owntest.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.owntest.R;
import com.example.owntest.models.Test;
import com.example.owntest.models.TestWithCompletion;
import com.example.owntest.models.TestCompletion;

import java.util.ArrayList;
import java.util.List;

public class CompletedTestAdapter extends RecyclerView.Adapter<CompletedTestAdapter.TestViewHolder> {

    private List<TestWithCompletion> tests;
    private OnTestClickListener listener;

    public interface OnTestClickListener {
        void onTestClick(Test test, TestCompletion completion);
    }

    public CompletedTestAdapter(OnTestClickListener listener) {
        this.tests = new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public TestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_completed_test, parent, false);
        return new TestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TestViewHolder holder, int position) {
        TestWithCompletion testWithCompletion = tests.get(position);
        holder.bind(testWithCompletion);
    }

    @Override
    public int getItemCount() {
        return tests.size();
    }

    public void setTests(List<TestWithCompletion> tests) {
        this.tests = tests;
        notifyDataSetChanged();
    }

    public void clearTests() {
        this.tests.clear();
        notifyDataSetChanged();
    }

    class TestViewHolder extends RecyclerView.ViewHolder {

        private ImageView ivTestIcon;
        private TextView tvTestTitle;
        private TextView tvTestDescription;
        private TextView tvDifficulty;
        private TextView tvQuestionCount;
        private TextView tvCreator;
        private TextView tvScore;
        private TextView tvPercentage;
        private TextView tvStatus;

        public TestViewHolder(@NonNull View itemView) {
            super(itemView);

            ivTestIcon = itemView.findViewById(R.id.ivTestIcon);
            tvTestTitle = itemView.findViewById(R.id.tvTestTitle);
            tvTestDescription = itemView.findViewById(R.id.tvTestDescription);
            tvDifficulty = itemView.findViewById(R.id.tvDifficulty);
            tvQuestionCount = itemView.findViewById(R.id.tvQuestionCount);
            tvCreator = itemView.findViewById(R.id.tvCreator);
            tvScore = itemView.findViewById(R.id.tvScore);
            tvPercentage = itemView.findViewById(R.id.tvPercentage);
            tvStatus = itemView.findViewById(R.id.tvStatus);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    TestWithCompletion testWithCompletion = tests.get(position);
                    listener.onTestClick(testWithCompletion.getTest(), testWithCompletion.getCompletion());
                }
            });
        }

        public void bind(TestWithCompletion testWithCompletion) {
            Context context = itemView.getContext();
            Test test = testWithCompletion.getTest();
            TestCompletion completion = testWithCompletion.getCompletion();

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

            // Результаты прохождения
            if (completion.isAuto()) {
                // Автоматическая проверка
                tvScore.setText(completion.getScore() + " / " + completion.getTotalQuestions());
                tvPercentage.setText(String.format("%.0f%%", completion.getPercentage()));
                tvStatus.setVisibility(View.GONE);
            } else if (completion.isPending()) {
                // Ожидает проверки
                tvScore.setText("—");
                tvPercentage.setText("—");
                tvStatus.setVisibility(View.VISIBLE);
                tvStatus.setText("Ожидает проверки");
                tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
            } else if (completion.isChecked()) {
                // Проверено
                tvScore.setText(completion.getEarnedPoints() + " / " + completion.getMaxPoints());
                tvPercentage.setText(String.format("%.0f%%", completion.getPercentage()));
                tvStatus.setVisibility(View.VISIBLE);
                tvStatus.setText("Проверено");
                tvStatus.setBackgroundResource(R.drawable.bg_status_checked);
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