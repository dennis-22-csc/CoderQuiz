package com.denniscode.coderquiz;

import android.content.Context;
import android.content.res.AssetManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipProcessor {
    private static final Pattern csvPattern = Pattern.compile(
            "\"([^\"]*)\"|'([^']*)'|\\[([^\\]]+)\\]|([^,]+)");

    public static String handleZipFile(QuizDatabaseHelper dbHelper, File zipFile) {
        Map<String, byte[]> imageCache =  new HashMap<>();
        String zipResult = null;

        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                if (entry.isDirectory()) {
                    String folderName = entry.getName();

                    // Check if it's the main folder (e.g., "fund/")
                    if (folderName.matches("^[^/]+/$")) { // Matches folders with no subfolders
                        boolean hasTxtFile = false;

                        // Check for sub-entries within the main folder
                        Enumeration<? extends ZipEntry> subEntries = zip.entries();
                        while (subEntries.hasMoreElements()) {
                            ZipEntry subEntry = subEntries.nextElement();
                            if (subEntry.getName().startsWith(folderName) && subEntry.getName().endsWith(".txt")) {
                                hasTxtFile = true;
                            }
                        }

                        // Process only if the folder has txt file
                        if (hasTxtFile) {
                            zipResult = processTxtFile(dbHelper, zip, folderName, imageCache);
                        } else {
                            String errorResult = "Zip file not accepted";
                            zip.close();
                            return errorResult;
                        }

                    }
                }
            }

        } catch (Exception e) {
            return "Error processing Zip file";
        }
        return zipResult;
    }

    private static String processTxtFile(QuizDatabaseHelper dbHelper, ZipFile zipFile, String folderName, Map<String, byte[]> imageCache) {
        try {
            ZipEntry txtEntry = zipFile.getEntry(folderName + "questions.txt");
            if (txtEntry == null) {
                return "Question file not found.";
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(zipFile.getInputStream(txtEntry)))) {
                String line;
                int lineNumber = 0;
                List<Question> validQuestions = new ArrayList<>();
                List<String> validCategories = new ArrayList<>();

                // Read quiz category from the first line
                String quizCategory = reader.readLine().trim();
                if (!quizCategory.isEmpty()) {
                    validCategories.add(quizCategory);
                }
                lineNumber++;

                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    List<String> columns = new ArrayList<>();
                    Matcher matcher = csvPattern.matcher(line.trim());

                    while (matcher.find()) {
                        if (matcher.group(1) != null) {
                            columns.add(matcher.group(1));  // Matches "double-quoted"
                        } else if (matcher.group(2) != null) {
                            columns.add(matcher.group(2));  // Matches 'single-quoted'
                        } else if (matcher.group(3) != null) {
                            columns.add(matcher.group(3));  // Matches [bracketed list] → Removed extra brackets
                        } else if (matcher.group(4) != null) {
                            columns.add(matcher.group(4).trim());  // Matches regular text
                        }
                    }

                    int columnCount = columns.size();

                    // Step 2: Validate column count
                    if (columnCount < 6 || columnCount > 9) {
                        return "Line " + lineNumber + ": Invalid column count (" + columnCount + " columns)";
                    }

                    // Step 3: Process extracted data
                    try {
                        int sourceId = Integer.parseInt(columns.get(0).trim());
                        String questionText = columns.get(1).trim();
                        String optionA = columns.get(2).trim();
                        String optionB = columns.get(3).trim();
                        String optionC = (columnCount >= 8) ? columns.get(4).trim() : null;
                        String optionD = (columnCount >= 8) ? columns.get(5).trim() : null;
                        String correctOption = (columnCount >= 8) ? columns.get(6).trim() : columns.get(4).trim();
                        String questionCategory = (columnCount >= 8) ? columns.get(7).trim() : columns.get(5).trim();

                        int imageId = -1;
                        if (FileUtil.hasImageFileName(columns.toArray(new String[0]))) {
                            String imageName = FileUtil.getImageFileName(columns.toArray(new String[0]));
                            byte[] imageBlob = FileUtil.getImageBlob(zipFile, imageCache, folderName + "images/" + imageName);
                            imageId = dbHelper.getOrInsertImage(imageName, imageBlob);
                        }
                        if (FileUtil.isImageFileName(questionText)) {
                            byte[] imageBlob = FileUtil.getImageBlob(zipFile, imageCache, folderName + "images/" + questionText);
                            int questionImageId = dbHelper.getOrInsertImage(questionText, imageBlob);
                            questionText = "IMG_" + questionImageId;
                        }

                        Question question = new Question(
                                sourceId, questionText, imageId, optionA, optionB,
                                optionC, optionD, correctOption, questionCategory, quizCategory);
                        validQuestions.add(question);

                    } catch (NumberFormatException e) {
                        return "Line " + lineNumber + ": Invalid source ID";
                    }
                }

                // Step 4: Save to database
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



    public static String loadFromZipAsset(Context context, QuizDatabaseHelper dbHelper) {
        String assetFileName = "sample_questions.zip";
        AssetManager assetManager = context.getAssets();

        try {
            InputStream inputStream = assetManager.open(assetFileName);
            File tempFile = createTempFileFromInputStream(context, inputStream, assetFileName); // See helper function below


            String result = handleZipFile(dbHelper, tempFile);
            tempFile.delete();
            return result;

        } catch (IOException e) {
            return "Failed to load questions from " + assetFileName + ": " + e.getMessage();
        }
    }
    private static File createTempFileFromInputStream(Context context, InputStream inputStream, String fileName) throws IOException {
        File tempFile = File.createTempFile(fileName.substring(0, fileName.lastIndexOf('.')), "." + fileName.substring(fileName.lastIndexOf('.') + 1), context.getCacheDir());
        try (OutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
        }
        return tempFile;
    }


}
