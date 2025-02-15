package com.denniscode.coderquiz;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import androidx.annotation.Nullable;

public class QuizCategoriesFragment extends Fragment {
    public QuizCategoriesFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_category, container, false);

        try (QuizDatabaseHelper dbHelper = new QuizDatabaseHelper(getContext())) {
            List<String> categories = dbHelper.getCategories();
            RecyclerView categoryRecyclerView = view.findViewById(R.id.categoryRecyclerView);
            categoryRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            CategoryAdapter categoryAdapter = new CategoryAdapter(categories);
            categoryRecyclerView.setAdapter(categoryAdapter);
        }

        return view;
    }

    private class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
        private final List<String> categories;

        public CategoryAdapter(List<String> categories) {
            this.categories = categories;
        }

        @NonNull
        @Override
        public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
            return new CategoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
            String category = categories.get(position);
            holder.categoryText.setText(category);

            holder.itemView.setOnClickListener(v -> startQuiz(category));
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        public static class CategoryViewHolder extends RecyclerView.ViewHolder {
            TextView categoryText;

            public CategoryViewHolder(View itemView) {
                super(itemView);
                categoryText = itemView.findViewById(R.id.categoryText);
            }
        }
    }

    private void startQuiz(String selectedCategory) {
        Intent intent = new Intent(getActivity(), QuizActivity.class);
        intent.putExtra("CATEGORY", selectedCategory);
        startActivity(intent);
    }
}