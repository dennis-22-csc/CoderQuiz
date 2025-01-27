package com.denniscode.coderquiz;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class MainActivity extends AppCompatActivity {

    private QuizDatabaseHelper dbHelper;
    private ActivityResultLauncher<Intent> pickFileLauncher;

    private CategoryAdapter categoryAdapter;

    private RecyclerView categoryRecyclerView;
    private List<String> categories;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new QuizDatabaseHelper(this);

        // Button to open file picker
        Button syncButton = findViewById(R.id.syncButton);
        syncButton.setOnClickListener(v -> openFilePicker());

        // Button to start the quiz
        Button startQuizButton = findViewById(R.id.startQuizButton);
        startQuizButton.setOnClickListener(v -> {
            if (dbHelper.getCategories().isEmpty()) {
                Toast.makeText(MainActivity.this, "No categories available. Please load a quiz file first.", Toast.LENGTH_SHORT).show();
            } else  {
                showQuizCategories();
            }
        });

        // Initialize the ActivityResultLauncher for picking files
        pickFileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Intent data = result.getData();
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    handleZipFile(uri);
                } else {
                    Toast.makeText(MainActivity.this, "No file selected", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/zip");
        pickFileLauncher.launch(intent);
    }

    private void handleZipFile(Uri uri) {
        try {
            // Open the zip file from the provided Uri
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return;
            }

            File tempFile = File.createTempFile("tempZip", ".zip", getCacheDir());
            FileOutputStream fos = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            fos.close();
            inputStream.close();

            ZipFile zipFile = new ZipFile(tempFile);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                if (entry.isDirectory()) {
                    String folderName = entry.getName();

                    // Check if it's the main folder (e.g., "fund/")
                    if (folderName.matches("^[^/]+/$")) { // Matches folders with no subfolders
                        boolean hasImagesFolder = false;
                        boolean hasTxtFile = false;

                        // Check for sub-entries within the main folder
                        Enumeration<? extends ZipEntry> subEntries = zipFile.entries();
                        while (subEntries.hasMoreElements()) {
                            ZipEntry subEntry = subEntries.nextElement();
                            if (subEntry.getName().startsWith(folderName)) {
                                if (subEntry.getName().equals(folderName + "images/")) {
                                    hasImagesFolder = true;
                                } else if (subEntry.getName().endsWith(".txt")) {
                                    hasTxtFile = true;
                                }
                            }
                        }

                        // Process only if the folder has txt file
                        if (hasTxtFile) {
                            processTxtFile(zipFile, folderName);
                        } else {
                            Toast.makeText(this, "Zip file not accepted", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }

            zipFile.close();
            tempFile.delete();
        } catch (Exception e) {
            Toast.makeText(this, "Error processing Zip file", Toast.LENGTH_LONG).show();
        }
    }

    private void processTxtFile(ZipFile zipFile, String folderName) {
        try {
            ZipEntry txtEntry = zipFile.getEntry(folderName + "questions.txt"); // Adjust filename if needed
            if (txtEntry == null) {
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(zipFile.getInputStream(txtEntry)));
            String line;

            // The first line contains the quiz category (title of the paper)
            String quizCategory = reader.readLine();
            if (quizCategory != null && !quizCategory.trim().isEmpty()) {
                dbHelper.addCategory(quizCategory.trim());
            }

            // Process each question line
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");

                if (columns.length == 6 || columns.length == 7 || columns.length == 8 || columns.length == 9) {
                    String sourceID = columns[0].trim();
                    String questionText = columns[1].trim();
                    String optionA = columns[2].trim();
                    String optionB = columns[3].trim();
                    String optionC = (columns.length >= 8) ? columns[4].trim() : null;
                    String optionD = (columns.length >= 8) ? columns[5].trim() : null;
                    String correctOption = (columns.length >= 8) ? columns[6].trim() : columns[4].trim();
                    String questionCategory = (columns.length >= 8) ? columns[7].trim() : columns[5].trim();

                    byte[] imageBlob = null;
                    if (columns.length == 7 || columns.length == 9) {
                        String imageName = columns[columns.length - 1].trim();
                        imageBlob = extractImageBlob(zipFile, folderName + "images/" + imageName);
                    }

                    // Create the Question object and persist it
                    Question question = new Question(
                            Integer.valueOf(sourceID),
                            questionText,
                            imageBlob,
                            optionA,
                            optionB,
                            optionC,
                            optionD,
                            correctOption,
                            questionCategory,
                            quizCategory
                    );
                    dbHelper.addQuestion(question);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error processing .txt file", Toast.LENGTH_LONG).show();
        }
    }

    private byte[] extractImageBlob(ZipFile zipFile, String imagePath) {
        try {
            ZipEntry imageEntry = zipFile.getEntry(imagePath);
            if (imageEntry == null) {
                return null;
            }

            InputStream inputStream = zipFile.getInputStream(imageEntry);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;

            while ((length = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, length);
            }

            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    public void startQuiz (String selectedCategory) {
        Intent intent = new Intent(this, QuizActivity.class);
        // Pass the selected category to QuizActivity
        intent.putExtra("CATEGORY", selectedCategory);
        startActivity(intent);
    }

    private void showQuizCategories() {
        setContentView(R.layout.activity_category);

        // Get categories from the database
        categories = dbHelper.getCategories(); // Get categories from the database

        categoryRecyclerView = findViewById(R.id.categoryRecyclerView);

        categoryRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        categoryAdapter = new CategoryAdapter(categories);
        categoryRecyclerView.setAdapter(categoryAdapter);

    }

    private class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

        private List<String> categories;
        private int selectedPosition = RecyclerView.NO_POSITION; // Track the selected position

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

            // Highlight the selected item
            holder.itemView.setSelected(position == selectedPosition);

            // Set click listener
            holder.itemView.setOnClickListener(v -> {
                // Start the quiz
                startQuiz(category);
            });
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        public class CategoryViewHolder extends RecyclerView.ViewHolder {
            TextView categoryText;

            public CategoryViewHolder(View itemView) {
                super(itemView);
                categoryText = itemView.findViewById(R.id.categoryText);
            }
        }
    }


}
