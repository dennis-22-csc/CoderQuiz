package com.denniscode.coderquiz;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
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
                            if (zipFile != null) {
                                if (!zipFile.delete()) {
                                    return "Failed to delete file: " + zipFile.getAbsolutePath();
                                }
                            }
                            return errorResult;
                        }

                    }
                }
            }
            if (zipFile != null) {
                if (!zipFile.delete()) {
                    return "Failed to delete file: " + zipFile.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            if (zipFile != null) {
                if (!zipFile.delete()) {
                    return "Failed to delete file: " + zipFile.getAbsolutePath();
                }
            }
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
                if (!quizCategory.isEmpty()) {
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
                        String fullMatch = matcher.group(0);
                        String extractedText = matcher.group(1);

                        if (fullMatch != null) {
                            extractedTexts.add(extractedText);
                            line = line.replace(fullMatch, "PLACEHOLDER" + extractedTexts.size());
                        }
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
                        if (columnCount > 7) {
                            return "Line " + lineNumber + ": True/False question must have 6 or 7 columns";
                        }
                        if (columnCount == 7) {
                            if (!FileUtil.hasImageFileName(columns)) {
                                return "Line " + lineNumber + ": True/False question with 7 columns must include an image file name";
                            }
                            if (FileUtil.imageNotInZip(zipFile, folderName + "images/" + FileUtil.getImageFileName(columns))) {
                                return "Line " + lineNumber + ": Image file '" + FileUtil.getImageFileName(columns) + "' does not exist";
                            }
                        }
                    } else {
                        if (columnCount < 8) {
                            return "Line " + lineNumber + ": Non-True/False question must have 8 or 9 columns";
                        }
                        if (columnCount == 9) {
                            if (!FileUtil.hasImageFileName(columns)) {
                                return "Line " + lineNumber + ": Non-True/False question with 9 columns must include an image file name";
                            }
                            if (FileUtil.imageNotInZip(zipFile, folderName + "images/" + FileUtil.getImageFileName(columns))) {
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


                        int imageId = -1;
                        if (FileUtil.hasImageFileName(columns)) {
                            String imageName = FileUtil.getImageFileName(columns);
                            byte[] imageBlob = FileUtil.getImageBlob(zipFile, imageCache, folderName + "images/" + imageName);
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

}
