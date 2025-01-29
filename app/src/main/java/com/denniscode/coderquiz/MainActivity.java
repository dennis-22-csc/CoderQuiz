package com.denniscode.coderquiz;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
                showDownloadDialog(this, "https://denniskoko.gumroad.com/l/cq_questions");
            } else {
                showQuizCategories();
            }
        });


        // Initialize the ActivityResultLauncher for picking files
        pickFileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Intent data = result.getData();
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    // Create a ProgressDialog using Material Design principles
                    AlertDialog progressDialog = new MaterialAlertDialogBuilder(MainActivity.this)
                            .setCancelable(false) // Prevent user from dismissing
                            .setView(createProgressBar()) // Add styled ProgressBar
                            .create();

                    // Show the ProgressDialog
                    progressDialog.show();

                    // Handle the ZIP file in a background thread
                    new Thread(() -> {
                        try {
                            String zipResult = handleZipFile(uri); // Ensure exception handling
                            runOnUiThread(() ->
                                    Toast.makeText(MainActivity.this, zipResult, Toast.LENGTH_LONG).show()
                            );
                        } catch (Exception e) {
                            runOnUiThread(() ->
                                    Toast.makeText(MainActivity.this, "Error processing file", Toast.LENGTH_LONG).show()
                            );
                        } finally {
                            // Dismiss the dialog on the UI thread
                            runOnUiThread(progressDialog::dismiss);
                        }
                    }).start();
                } else {
                    // Notify the user if no file is selected
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "No file selected", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

// Helper method to create a ProgressBar with proper styling
        private View createProgressBar() {
            int padding = 16;
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(padding, padding, padding, padding);
            layout.setGravity(Gravity.CENTER);

            ProgressBar progressBar = new ProgressBar(this);
            progressBar.setIndeterminate(true); // Indeterminate progress

            layout.addView(progressBar);
            return layout;
        }

        private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/zip");
        pickFileLauncher.launch(intent);
    }

    private String handleZipFile(Uri uri) {
        String zipResult = null;
        try {
            // Open the zip file from the provided Uri
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return "Zip file could not be opened";
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
                            zipResult = processTxtFile(zipFile, folderName);
                        } else {
                            String errorResult = "Zip file not accepted";
                            zipFile.close();
                            tempFile.delete();
                            return errorResult;
                        }
                    }
                }
            }
            zipFile.close();
            tempFile.delete();
        } catch (Exception e) {
            return "Error processing Zip file";
        }
        return zipResult;
    }

    private String processTxtFile(ZipFile zipFile, String folderName) {
        try {
            ZipEntry txtEntry = zipFile.getEntry(folderName + "questions.txt");
            if (txtEntry == null) {
                return "Question file not found.";
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(zipFile.getInputStream(txtEntry)))) {
                String line;
                int lineNumber = 1;
                List<Question> validQuestions = new ArrayList<>();
                List<String> validCategories = new ArrayList<>();
                Pattern bracketPattern = Pattern.compile("\\[(.*?)]"); // Pattern to match text inside []

                // Read quiz category from the first line
                String quizCategory = reader.readLine();
                if (quizCategory != null && !quizCategory.trim().isEmpty()) {
                    validCategories.add(quizCategory.trim());
                }

                // Process each question line
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    // Extract text inside square brackets before splitting
                    Matcher matcher = bracketPattern.matcher(line);
                    List<String> extractedTexts = new ArrayList<>();
                    while (matcher.find()) {
                        extractedTexts.add(matcher.group(1)); // Store extracted text
                        line = line.replace(matcher.group(0), "PLACEHOLDER" + extractedTexts.size());
                    }

                    String[] columns = line.split(",");

                    // Restore extracted text in respective positions
                    for (int i = 0; i < columns.length; i++) {
                        if (columns[i].startsWith("PLACEHOLDER")) {
                            int index = Integer.parseInt(columns[i].replace("PLACEHOLDER", "")) - 1;
                            columns[i] = extractedTexts.get(index);
                        }
                        columns[i] = columns[i].trim(); // Trim whitespace
                    }

                    int columnCount = columns.length;

                    // Validate column count
                    if (columnCount < 6 || columnCount > 9) {
                        return "Line " + lineNumber + ": Invalid column count (" + columnCount + " columns)";
                    }

                    if (isTrueOrFalseQuestion(columns)) {
                        if (columnCount > 7 || columnCount < 6) {
                            return "Line " + lineNumber + ": True/False question must have 6 or 7 columns";
                        }
                        if (columnCount == 7) {
                            if (!hasImageFileName(columns)) {
                                return "Line " + lineNumber + ": True/False question with 7 columns must include an image file name";
                            }
                            if (!imageExistsInZip(zipFile, folderName + "images/" + getImageFileName(columns))) {
                                return "Line " + lineNumber + ": Image file '" + getImageFileName(columns) + "' does not exist";
                            }
                        }
                    } else {
                        if (columnCount > 9 || columnCount < 8) {
                            return "Line " + lineNumber + ": Non-True/False question must have 8 or 9 columns";
                        }
                        if (columnCount == 9) {
                            if (!hasImageFileName(columns)) {
                                return "Line " + lineNumber + ": Non-True/False question with 9 columns must include an image file name";
                            }
                            if (!imageExistsInZip(zipFile, folderName + "images/" + getImageFileName(columns))) {
                                return "Line " + lineNumber + ": Image file '" + getImageFileName(columns) + "' does not exist";
                            }
                        }
                    }

                    // Process the question data
                    try {
                        int sourceId = Integer.parseInt(columns[0]);
                        String questionText = columns[1];
                        String optionA = columns[2];
                        String optionB = columns[3];
                        String optionC = (columns.length >= 8) ? columns[4] : null;
                        String optionD = (columns.length >= 8) ? columns[5] : null;
                        String correctOption = (columns.length >= 8) ? columns[6] : columns[4];
                        String questionCategory = (columns.length >= 8) ? columns[7] : columns[5];

                        byte[] imageBlob = null;
                        if (hasImageFileName(columns)) {
                            String imageName = getImageFileName(columns);
                            imageBlob = extractImageBlob(zipFile, folderName + "images/" + imageName);
                        }

                        Question question = new Question(
                                sourceId, questionText, imageBlob, optionA, optionB,
                                optionC, optionD, correctOption, questionCategory, quizCategory);
                        validQuestions.add(question);
                    } catch (NumberFormatException e) {
                        return "Line " + lineNumber + ": Invalid source ID";
                    }
                }

                // Add categories and questions to the database
                for (String category : validCategories) {
                    dbHelper.addCategory(category);
                }

                for (Question question : validQuestions) {
                    dbHelper.addQuestion(question);
                }

                return "Questions added successfully";
            }
        } catch (Exception e) {
            return "Error processing .txt file: " + e.getMessage();
        }
    }

    private boolean hasImageFileName(String[] columns) {
        String lastColumn = columns[columns.length - 1].trim();
        return lastColumn.endsWith(".jpg") || lastColumn.endsWith(".png") || lastColumn.endsWith(".jpeg");
    }

    private String getImageFileName(String[] columns) {
        return columns[columns.length - 1].trim();
    }




    private boolean isTrueOrFalseQuestion(String[] columns) {
        for (String column : columns) {
            if (column != null) {
                String trimmedColumn = column.trim().toLowerCase();
                if (trimmedColumn.equals("true") || trimmedColumn.equals("false") ||
                        trimmedColumn.equals("yes") || trimmedColumn.equals("no")) {
                    return true;
                }
            }
        }
        return false;
    }


    private boolean hasImageFile(String[] columns) {
        return columns.length == 7 || columns.length == 9;
    }

    private boolean imageExistsInZip(ZipFile zipFile, String imagePath) {
        return zipFile.getEntry(imagePath) != null;
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

    private void showDownloadDialog(Context context, String downloadUrl) {
        // Use MaterialAlertDialogBuilder for a Material Design styled dialog
        new MaterialAlertDialogBuilder(context)
                .setTitle("Please load a quiz file first")
                .setMessage("Would you like to download a quiz file?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Open the URL in the browser
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
                    context.startActivity(intent);
                    dialog.dismiss();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    // Dismiss the dialog
                    dialog.dismiss();
                })
                .show();
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
