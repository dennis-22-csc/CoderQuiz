package com.denniscode.coderquiz;

import android.content.Context;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileUtil {

    public static String handleZipFile(Context context, Uri uri, QuizDatabaseHelper dbHelper, Map<String, byte[]> imageCache) {
        String zipResult = null;
        File tempFile = null;

        try {
            // Open the zip file from the provided Uri
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return "Zip file could not be opened";
            }
            tempFile = File.createTempFile("tempZip", ".zip", context.getCacheDir());
            tempFile.deleteOnExit();
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
                            zipResult = processTxtFile(zipFile, folderName, dbHelper, imageCache);
                        } else {
                            String errorResult = "Zip file not accepted";
                            zipFile.close();
                            return errorResult;
                        }

                    }
                }
            }

            if (tempFile!=null) {
                tempFile.delete();
            }
            zipFile.close();
        } catch (Exception e) {
            if (tempFile!=null) {
                tempFile.delete();
            }
            return "Error processing Zip file";
        } finally {
            if (tempFile!=null) {
                tempFile.delete();
            }
        }
        return zipResult;
    }

    private static String processTxtFile(ZipFile zipFile, String folderName, QuizDatabaseHelper dbHelper, Map<String, byte[]> imageCache) {
        try {

            ZipEntry txtEntry = zipFile.getEntry(folderName + "questions.txt");

            if (txtEntry == null) {
                return "Question file not found.";
            }

            try (BufferedReader reader = new
                    BufferedReader(new
                    InputStreamReader(zipFile.getInputStream(txtEntry)))) {

                String line;
                int lineNumber = 1;

                List<Question> validQuestions = new ArrayList<>();
                List<String> validCategories = new ArrayList<>();
                Pattern bracketPattern = Pattern.compile("\\[(.*?)]"); // Pattern to match text inside []

                // Read quiz category from the first line
                String quizCategory = reader.readLine().trim();
                if (quizCategory != null && !quizCategory.isEmpty()) {
                    validCategories.add(quizCategory);
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

                    if (FileUtil.isTrueOrFalseQuestion(columns)) {
                        if (columnCount > 7 || columnCount < 6) {
                            return "Line " + lineNumber + ": True/False question must have 6 or 7 columns";
                        }
                        if (columnCount == 7) {
                            if (!FileUtil.hasImageFileName(columns)) {
                                return "Line " + lineNumber + ": True/False question with 7 columns must include an image file name";
                            }
                            if (!FileUtil.imageExistsInZip(zipFile, folderName + "images/" + FileUtil.getImageFileName(columns))) {
                                return "Line " + lineNumber + ": Image file '" + FileUtil.getImageFileName(columns) + "' does not exist";
                            }
                        }
                    } else {
                        if (columnCount > 9 || columnCount < 8) {
                            return "Line " + lineNumber + ": Non-True/False question must have 8 or 9 columns";
                        }
                        if (columnCount == 9) {
                            if (!FileUtil.hasImageFileName(columns)) {
                                return "Line " + lineNumber + ": Non-True/False question with 9 columns must include an image file name";
                            }
                            if (!FileUtil.imageExistsInZip(zipFile, folderName + "images/" + FileUtil.getImageFileName(columns))) {
                                return "Line " + lineNumber + ": Image file '" + FileUtil.getImageFileName(columns) + "' does not exist";
                            }
                        }

                    }

                    // Process the question data
                    try {
                        int sourceId = Integer.parseInt(columns[0].trim());
                        String questionText = columns[1].trim();
                        String optionA = columns[2].trim();
                        String optionB = columns[3].trim();
                        String optionC = (columns.length >= 8) ? columns[4].trim() : null;
                        String optionD = (columns.length >= 8) ? columns[5].trim() : null;
                        String correctOption = (columns.length >= 8) ? columns[6].trim() : columns[4].trim();
                        String questionCategory = (columns.length >= 8) ? columns[7].trim() : columns[5].trim();

                        byte[] imageBlob = null;
                        int imageId = -1;
                        if (FileUtil.hasImageFileName(columns)) {
                            String imageName = FileUtil.getImageFileName(columns);
                            imageBlob = FileUtil.getImageBlob(zipFile, imageCache, folderName + "images/" + imageName);
                            imageId = dbHelper.getOrInsertImage(imageName, imageBlob);
                        }

                        Question question = new Question(
                                sourceId, questionText, imageId, optionA, optionB,
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


    private static boolean hasImageFileName(String[] columns) {
        String lastColumn = columns[columns.length - 1].trim();
        return lastColumn.endsWith(".jpg") || lastColumn.endsWith(".png") || lastColumn.endsWith(".jpeg");
    }

    private static String getImageFileName(String[] columns) {
        return columns[columns.length - 1].trim();
    }

    private static boolean isTrueOrFalseQuestion(String[] columns) {
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

    private static boolean imageExistsInZip(ZipFile zipFile, String imagePath) {
        return zipFile.getEntry(imagePath) != null;
    }

    private static byte[] getImageBlob(ZipFile zipFile, Map<String, byte[]> imageCache, String imagePath) {
        if (imageCache.containsKey(imagePath)) {
            return imageCache.get(imagePath); // Use cached image
        }

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

            byte[] imageBlob = baos.toByteArray();
            imageCache.put(imagePath, imageBlob); // Cache the image
            return imageBlob;
        } catch (Exception e) {
            return null;
        }
    }


}
