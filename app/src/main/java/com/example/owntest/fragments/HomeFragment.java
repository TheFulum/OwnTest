package com.example.owntest.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.owntest.R;
import com.example.owntest.adapters.TestAdapter;
import com.example.owntest.managers.FirebaseManager;
import com.example.owntest.models.Test;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextInputEditText etSearch;
    private MaterialButton btnFilter;
    private RecyclerView rvTests;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private View layoutEmpty;

    private TestAdapter adapter;
    private FirebaseManager firebaseManager;

    private List<Test> allTests = new ArrayList<>();
    private List<Test> filteredTests = new ArrayList<>();

    private String currentSortBy = "date";
    private boolean isDescending = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initViews(view);
        setupRecyclerView();
        setupListeners();

        firebaseManager = FirebaseManager.getInstance();
        loadTests();

        return view;
    }

    private void initViews(View view) {
        etSearch = view.findViewById(R.id.etSearch);
        btnFilter = view.findViewById(R.id.btnFilter);
        rvTests = view.findViewById(R.id.rvTests);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        progressBar = view.findViewById(R.id.progressBar);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
    }

    private void setupRecyclerView() {
        adapter = new TestAdapter(new TestAdapter.OnTestClickListener() {
            @Override
            public void onTestClick(Test test) {
                openTestDetails(test);
            }

            @Override
            public void onEditClick(Test test) {
                // Этот метод не будет вызываться в HomeFragment
                // так как showCreatorOptions = false
            }

            @Override
            public void onDeleteClick(Test test) {
                // Этот метод не будет вызываться в HomeFragment
            }
        }, false); // false = не показывать опции создателя

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        rvTests.setLayoutManager(layoutManager);
        rvTests.setAdapter(adapter);

        rvTests.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!recyclerView.canScrollVertically(1)) {
                    // TODO: Пагинация
                }
            }
        });
    }

    private void setupListeners() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTests(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnFilter.setOnClickListener(v -> showFilterDialog());
        swipeRefresh.setOnRefreshListener(this::loadTests);
    }

    private void loadTests() {
        showLoading(true);

        firebaseManager.getAllTests(new FirebaseManager.TestListCallback() {
            @Override
            public void onSuccess(List<Test> tests) {
                if (getContext() == null) return;

                allTests = tests;
                sortTests();
                filterTests(etSearch.getText().toString());

                showLoading(false);
                swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onFailure(String error) {
                if (getContext() == null) return;

                Toast.makeText(getContext(), "Ошибка: " + error, Toast.LENGTH_SHORT).show();
                showLoading(false);
                swipeRefresh.setRefreshing(false);
            }
        });
    }

    private void filterTests(String query) {
        filteredTests.clear();

        if (query.isEmpty()) {
            filteredTests.addAll(allTests);
        } else {
            String lowerQuery = query.toLowerCase(Locale.getDefault());
            for (Test test : allTests) {
                if (test.getTitle().toLowerCase(Locale.getDefault()).contains(lowerQuery) ||
                        test.getDescription().toLowerCase(Locale.getDefault()).contains(lowerQuery) ||
                        test.getCreatorNickname().toLowerCase(Locale.getDefault()).contains(lowerQuery)) {
                    filteredTests.add(test);
                }
            }
        }

        adapter.setTests(filteredTests);
        updateEmptyState();
    }

    private void sortTests() {
        Comparator<Test> comparator;

        switch (currentSortBy) {
            case "rating":
                comparator = Comparator.comparingDouble(Test::getAverageRating);
                break;
            case "completions":
                comparator = Comparator.comparingInt(Test::getCompletionsCount);
                break;
            case "date":
            default:
                comparator = Comparator.comparingLong(Test::getCreatedDate);
                break;
        }

        if (isDescending) {
            comparator = comparator.reversed();
        }

        Collections.sort(allTests, comparator);
    }

    private void showFilterDialog() {
        String[] options = {
                "По дате (сначала новые)",
                "По дате (сначала старые)",
                "По рейтингу (от высокого)",
                "По рейтингу (от низкого)",
                "По популярности (от высокой)",
                "По популярности (от низкой)"
        };

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Сортировка")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            currentSortBy = "date";
                            isDescending = true;
                            break;
                        case 1:
                            currentSortBy = "date";
                            isDescending = false;
                            break;
                        case 2:
                            currentSortBy = "rating";
                            isDescending = true;
                            break;
                        case 3:
                            currentSortBy = "rating";
                            isDescending = false;
                            break;
                        case 4:
                            currentSortBy = "completions";
                            isDescending = true;
                            break;
                        case 5:
                            currentSortBy = "completions";
                            isDescending = false;
                            break;
                    }
                    sortTests();
                    filterTests(etSearch.getText().toString());
                })
                .show();
    }

    private void updateEmptyState() {
        if (filteredTests.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvTests.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvTests.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvTests.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void openTestDetails(Test test) {
        TestDetailsFragment fragment = TestDetailsFragment.newInstance(test.getTestId());

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}