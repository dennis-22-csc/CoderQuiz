package com.denniscode.coderquiz;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;


public class QuizDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "quiz.db";

    private static final int DATABASE_VERSION = 2;


    private static final String TABLE_CATEGORIES = "quiz_categories";

    private static final String TABLE_QUESTIONS = "quiz_questions";
    private static final String TABLE_QUIZ_STAT = "quiz_stat";

    // Column names for categories
    private static final String COLUMN_CATEGORY_ID = "id";
    private static final String COLUMN_CATEGORY_NAME = "category_name";

    // Column names for questions
    private static final String COLUMN_QUESTION_ID = "id";
    private static final String COLUMN_SOURCE_ID = "source_id";
    private static final String COLUMN_QUESTION = "question";
    private static final String COLUMN_IMAGE_ID = "image_id";
    private static final String COLUMN_OPTION_A = "option_a";
    private static final String COLUMN_OPTION_B = "option_b";
    private static final String COLUMN_OPTION_C = "option_c";
    private static final String COLUMN_OPTION_D = "option_d";
    private static final String COLUMN_CORRECT_OPTION = "correct_option";
    private static final String COLUMN_QUESTION_CATEGORY = "question_category";
    private static final String COLUMN_QUIZ_CATEGORY = "quiz_category";
    private static final String COLUMN_LAST_ATTEMPTED = "last_attempted";
    private static final String COLUMN_QUESTION_STATUS = "status";

    // Column names for quiz_stat
    private static final String COLUMN_STAT_ID = "id";
    private static final String COLUMN_CORRECT_ANSWERS = "correct_answers";
    private static final String COLUMN_INCORRECT_ANSWERS = "incorrect_answers";
    private static final String COLUMN_TOTAL_QUESTIONS = "total_questions";
    private static final String COLUMN_CATEGORY_PERFORMANCE = "category_performance";
    private static final String COLUMN_DATE_TIME = "date_time";

    // Images Table
    private static final String TABLE_IMAGES = "images";
    private static final String COLUMN_IMAGE_ID_PK = "id";
    private static final String COLUMN_IMAGE_NAME = "name";
    private static final String COLUMN_IMAGE_BLOB = "data";

    public QuizDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Images Table
        String CREATE_IMAGES_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_IMAGES + " ("
                + COLUMN_IMAGE_ID_PK + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_IMAGE_NAME + " TEXT UNIQUE, " // Prevent duplicate images
                + COLUMN_IMAGE_BLOB + " BLOB)";
        db.execSQL(CREATE_IMAGES_TABLE);

        // Create categories table
        String createCategoriesTable = "CREATE TABLE " + TABLE_CATEGORIES + " ("
                + COLUMN_CATEGORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_CATEGORY_NAME + " TEXT NOT NULL UNIQUE);";
        db.execSQL(createCategoriesTable);

        // Create questions table
        String createQuestionsTable = "CREATE TABLE " + TABLE_QUESTIONS + " ("
                + COLUMN_QUESTION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_SOURCE_ID + " INTEGER NOT NULL, "
                + COLUMN_QUESTION + " TEXT NOT NULL UNIQUE, "
                + COLUMN_IMAGE_ID + " INTEGER, "
                + COLUMN_OPTION_A + " TEXT NOT NULL, "
                + COLUMN_OPTION_B + " TEXT NOT NULL, "
                + COLUMN_OPTION_C + " TEXT, "
                + COLUMN_OPTION_D + " TEXT, "
                + COLUMN_CORRECT_OPTION + " TEXT NOT NULL, "
                + COLUMN_QUESTION_CATEGORY + " TEXT NOT NULL, "
                + COLUMN_QUIZ_CATEGORY + " TEXT NOT NULL, "
                + COLUMN_LAST_ATTEMPTED + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + COLUMN_QUESTION_STATUS + " TEXT DEFAULT 'NEW', "
                + "FOREIGN KEY (" + COLUMN_IMAGE_ID + ") REFERENCES " + TABLE_IMAGES + "(" + COLUMN_IMAGE_ID_PK + "))";
        db.execSQL(createQuestionsTable);

        // Create quiz_stat table
        String createQuizStatTable = "CREATE TABLE " + TABLE_QUIZ_STAT + " ("
                + COLUMN_STAT_ID + " TEXT PRIMARY KEY, "
                + COLUMN_QUIZ_CATEGORY + " TEXT NOT NULL, "
                + COLUMN_CORRECT_ANSWERS + " INTEGER NOT NULL, "
                + COLUMN_INCORRECT_ANSWERS + " INTEGER NOT NULL, "
                + COLUMN_TOTAL_QUESTIONS + " INTEGER NOT NULL, "
                + COLUMN_CATEGORY_PERFORMANCE + " TEXT NOT NULL, "
                + COLUMN_DATE_TIME + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP);";
        db.execSQL(createQuizStatTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_QUIZ_STAT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_QUESTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_IMAGES);
        onCreate(db);
    }

    // Add data to quiz_stat table
    public void addQuizStat(String statId, String quizCategory, int correctAnswers, int incorrectAnswers, int totalQuestions, Map<String, Float> categoryPerformance, String dateTime) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Convert Map<String, Float> to JSON string
        JSONObject jsonObject = new JSONObject(categoryPerformance);
        String categoryPerformanceJson = jsonObject.toString();

        ContentValues values = new ContentValues();
        values.put(COLUMN_STAT_ID, statId);
        values.put(COLUMN_QUIZ_CATEGORY, quizCategory);
        values.put(COLUMN_CORRECT_ANSWERS, correctAnswers);
        values.put(COLUMN_INCORRECT_ANSWERS, incorrectAnswers);
        values.put(COLUMN_TOTAL_QUESTIONS, totalQuestions);
        values.put(COLUMN_CATEGORY_PERFORMANCE, categoryPerformanceJson);
        values.put(COLUMN_DATE_TIME, dateTime);

        db.insert(TABLE_QUIZ_STAT, null, values);
        db.close();
    }




    public List<Map<String, Object>> getAllQuizStats() {
        SQLiteDatabase db = this.getReadableDatabase();
        List<Map<String, Object>> quizStatList = new ArrayList<>();

        // Order results by dateTime in ascending order
        String query = "SELECT * FROM " + TABLE_QUIZ_STAT + " ORDER BY " + COLUMN_DATE_TIME + " ASC";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                Map<String, Object> quizStat = new HashMap<>();
                quizStat.put(COLUMN_STAT_ID, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STAT_ID)));
                quizStat.put(COLUMN_QUIZ_CATEGORY, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_QUIZ_CATEGORY)));
                quizStat.put(COLUMN_CORRECT_ANSWERS, cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CORRECT_ANSWERS)));
                quizStat.put(COLUMN_INCORRECT_ANSWERS, cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_INCORRECT_ANSWERS)));
                quizStat.put(COLUMN_TOTAL_QUESTIONS, cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_QUESTIONS)));

                // Retrieve and deserialize the category performance JSON string
                String categoryPerformanceJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_PERFORMANCE));
                Map<String, Float> categoryPerformance = new HashMap<>();
                try {
                    JSONObject jsonObject = new JSONObject(categoryPerformanceJson);
                    Iterator<String> keys = jsonObject.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        categoryPerformance.put(key, (float) jsonObject.getDouble(key));
                    }
                } catch (Exception e) {
                    Log.e("CoderQuiz", e.toString());
                }
                quizStat.put(COLUMN_CATEGORY_PERFORMANCE, categoryPerformance);
                quizStat.put(COLUMN_DATE_TIME, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_TIME)));

                quizStatList.add(quizStat);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return quizStatList;
    }

    public void addQuizStats(List<Map<String, Object>> quizStatsList) {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            db.beginTransaction();  // Begin transaction

            for (Map<String, Object> quizStat : quizStatsList) {
                // Extract values from the map
                String statId = (String) quizStat.get("id");
                String quizCategory = (String) quizStat.get("quiz_category");  // Fixed key
                int correctAnswers = (int) quizStat.get("correct_answers");
                int incorrectAnswers = (int) quizStat.get("incorrect_answers");
                int totalQuestions = (int) quizStat.get("total_questions");
                Map<String, Float> categoryPerformance = (Map<String, Float>) quizStat.get("category_performance");
                String dateTime = (String) quizStat.get("date_time");

                // Convert category performance Map<String, Float> to JSON string
                JSONObject jsonObject = new JSONObject(categoryPerformance);
                String categoryPerformanceJson = jsonObject.toString();

                // Insert the stat into the database
                ContentValues values = new ContentValues();
                values.put(COLUMN_STAT_ID, statId);
                values.put(COLUMN_QUIZ_CATEGORY, quizCategory);
                values.put(COLUMN_CORRECT_ANSWERS, correctAnswers);
                values.put(COLUMN_INCORRECT_ANSWERS, incorrectAnswers);
                values.put(COLUMN_TOTAL_QUESTIONS, totalQuestions);
                values.put(COLUMN_CATEGORY_PERFORMANCE, categoryPerformanceJson);
                values.put(COLUMN_DATE_TIME, dateTime);

                // Insert into database
                db.insert(TABLE_QUIZ_STAT, null, values);
            }

            db.setTransactionSuccessful();  // Commit transaction
        } catch (Exception e) {
            // e.getMessage();

        } finally {
            db.endTransaction();  // Ensure transaction is always ended
        }
    }


    public Map<String, Object> getQuizStatById(String statId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Map<String, Object> quizStat = null;

        // Query to get the stat by its ID
        String query = "SELECT * FROM " + TABLE_QUIZ_STAT + " WHERE " + COLUMN_STAT_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{statId});

        if (cursor.moveToFirst()) {
            quizStat = new HashMap<>();
            quizStat.put(COLUMN_STAT_ID, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STAT_ID)));
            quizStat.put(COLUMN_QUIZ_CATEGORY, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_QUIZ_CATEGORY)));
            quizStat.put(COLUMN_CORRECT_ANSWERS, cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CORRECT_ANSWERS)));
            quizStat.put(COLUMN_INCORRECT_ANSWERS, cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_INCORRECT_ANSWERS)));
            quizStat.put(COLUMN_TOTAL_QUESTIONS, cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_QUESTIONS)));

            quizStat.put(COLUMN_CATEGORY_PERFORMANCE, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_PERFORMANCE)));

            quizStat.put(COLUMN_DATE_TIME, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_TIME)));
        }

        cursor.close();
        db.close();
        return quizStat;
    }


    // Add category to the database
    public void addCategory(String category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CATEGORY_NAME, category);
        db.insert(TABLE_CATEGORIES, null, values);
        db.close();
    }

    public List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // SQL query to fetch categories that have at least one question
        String query = "SELECT DISTINCT " + COLUMN_CATEGORY_NAME + " FROM " + TABLE_CATEGORIES +
                " WHERE EXISTS (SELECT 1 FROM " + TABLE_QUESTIONS +
                " WHERE " + TABLE_QUESTIONS + "." + COLUMN_QUIZ_CATEGORY + " = " +
                TABLE_CATEGORIES + "." + COLUMN_CATEGORY_NAME + ")";

        Cursor cursor = db.rawQuery(query, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                categories.add(cursor.getString(cursor.getColumnIndex(COLUMN_CATEGORY_NAME)));
            } while (cursor.moveToNext());
            cursor.close();
        }

        db.close();
        return categories;
    }


    // Add a question to the database
    public boolean addQuestion(Question question) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SOURCE_ID, question.getSourceID());
        values.put(COLUMN_QUESTION, question.getQuestion());
        values.put(COLUMN_OPTION_A, question.getOptionA());
        values.put(COLUMN_OPTION_B, question.getOptionB());
        values.put(COLUMN_OPTION_C, question.getOptionC());
        values.put(COLUMN_OPTION_D, question.getOptionD());
        values.put(COLUMN_CORRECT_OPTION, question.getCorrectOption());
        values.put(COLUMN_QUESTION_CATEGORY, question.getQuestionCategory());
        values.put(COLUMN_QUIZ_CATEGORY, question.getQuizCategory());
        values.put(COLUMN_IMAGE_ID, question.getImageId());

        db.insert(TABLE_QUESTIONS, null, values);
        db.close();
        return true;
    }

    public List<Question> getQuizQuestions(String category, int limit) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<Question> questions = new ArrayList<>();
        Map<String, List<Question>> categoryMap = new HashMap<>();

        // Query to fetch failed or new questions by category
        String query = "SELECT * FROM " + TABLE_QUESTIONS + " WHERE " + COLUMN_QUIZ_CATEGORY + " = ? "
                + "AND (status = 'FAILED' OR status = 'NEW') "
                + "ORDER BY CASE WHEN status = 'FAILED' THEN 1 "
                + "WHEN status = 'NEW' THEN 2 ELSE 3 END, last_attempted ASC";

        Cursor cursor = db.rawQuery(query, new String[]{category});

        if (cursor.moveToFirst()) {
            do {
                // Extract question data and create Question objects
                Question question = new Question(
                        cursor.getInt(cursor.getColumnIndex(COLUMN_SOURCE_ID)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_QUESTION)),
                        cursor.getInt(cursor.getColumnIndex(COLUMN_IMAGE_ID)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_OPTION_A)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_OPTION_B)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_OPTION_C)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_OPTION_D)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_CORRECT_OPTION)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_QUESTION_CATEGORY)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_QUIZ_CATEGORY))
                );
                question.setQuestionID(cursor.getInt(cursor.getColumnIndex(COLUMN_QUESTION_ID)));
                question.setQuestionStatus(cursor.getString(cursor.getColumnIndex(COLUMN_QUESTION_STATUS)));

                // Group questions by category
                String questionCategory = question.getQuestionCategory();
                categoryMap.putIfAbsent(questionCategory, new ArrayList<>());
                categoryMap.get(questionCategory).add(question);
            } while (cursor.moveToNext());
        }
        cursor.close();

        // Sort categories by number of questions
        List<String> categories = new ArrayList<>(categoryMap.keySet());
        categories.sort(Comparator.comparingInt(cat -> categoryMap.get(cat).size()));

        // Split into bottom half and top half
        int midIndex = categories.size() / 2;
        List<String> bottomHalf = categories.subList(0, midIndex);
        List<String> topHalf = categories.subList(midIndex, categories.size());

        // Randomly select 3 categories from the bottom half
        Collections.shuffle(bottomHalf);
        List<String> selectedCategories = new ArrayList<>(bottomHalf.subList(0, Math.min(3, bottomHalf.size())));

        // Randomly select 2 categories from the top half
        Collections.shuffle(topHalf);
        selectedCategories.addAll(topHalf.subList(0, Math.min(2, topHalf.size())));

        // Collect questions from selected categories
        List<Question> selectedQuestions = new ArrayList<>();
        for (String cat : selectedCategories) {
            List<Question> categoryQuestions = categoryMap.get(cat);

            // Add all questions from this category
            selectedQuestions.addAll(categoryQuestions);

            // Stop if we reach the limit
            if (selectedQuestions.size() >= limit) {
                break;
            }
        }

        // If still under the limit, add more questions from remaining categories
        if (selectedQuestions.size() < limit) {
            List<String> remainingCategories = new ArrayList<>(categories);
            remainingCategories.removeAll(selectedCategories);
            Collections.shuffle(remainingCategories);

            for (String cat : remainingCategories) {
                List<Question> categoryQuestions = categoryMap.get(cat);

                int remainingLimit = limit - selectedQuestions.size();
                selectedQuestions.addAll(categoryQuestions.subList(0, Math.min(remainingLimit, categoryQuestions.size())));

                if (selectedQuestions.size() >= limit) {
                    break;
                }
            }
        }

        // Limit the total number of questions to the specified limit
        return selectedQuestions.subList(0, Math.min(selectedQuestions.size(), limit));
    }

    public void updateQuestionStatus(int questionId, boolean isCorrect) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", isCorrect ? "CORRECT" : "FAILED");
        values.put("last_attempted", System.currentTimeMillis()); // Update timestamp
        db.update(TABLE_QUESTIONS, values, COLUMN_QUESTION_ID + " = ?", new String[]{String.valueOf(questionId)});
        db.close();
    }

    public void resetCorrectQuestions(String category) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Check if all questions in the category are marked as CORRECT
        String checkQuery = "SELECT COUNT(*) FROM " + TABLE_QUESTIONS + " WHERE " + COLUMN_QUIZ_CATEGORY + " = ? AND status != 'CORRECT'";
        Cursor cursor = db.rawQuery(checkQuery, new String[]{category});
        cursor.moveToFirst();
        int incompleteCount = cursor.getInt(0);
        //cursor.close();

        if (incompleteCount == 0) {
            // Reset all CORRECT questions to NEW
            ContentValues values = new ContentValues();
            values.put("status", "NEW");
            db.update(TABLE_QUESTIONS, values, COLUMN_QUIZ_CATEGORY + " = ? AND status = 'CORRECT'", new String[]{category});
        }
        db.close();
    }

    public int getOrInsertImage(String imageName, byte[] imageBlob) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Check if the image already exists
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_IMAGE_ID_PK + " FROM " + TABLE_IMAGES + " WHERE " + COLUMN_IMAGE_NAME + " = ?", new String[]{imageName});
        if (cursor.moveToFirst()) {
            int imageId = cursor.getInt(0);
            cursor.close();
            return imageId; // Return existing image ID
        }
        cursor.close();

        // Insert new image
        ContentValues values = new ContentValues();
        values.put(COLUMN_IMAGE_NAME, imageName);
        values.put(COLUMN_IMAGE_BLOB, imageBlob);

        long newImageId = db.insert(TABLE_IMAGES, null, values);
        return (int) newImageId;
    }

    public Map<Integer, byte[]> getImageBlobsByIds(int[] imageIds) {
        Map<Integer, byte[]> imageMap = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < imageIds.length; i++) {
            placeholders.append("?");
            if (i < imageIds.length - 1) {
                placeholders.append(",");
            }
        }

        Cursor cursor = db.rawQuery(
                "SELECT " + COLUMN_IMAGE_ID_PK + ", " + COLUMN_IMAGE_BLOB +
                        " FROM " + TABLE_IMAGES +
                        " WHERE " + COLUMN_IMAGE_ID_PK + " IN (" + placeholders + ")",
                Arrays.stream(imageIds).mapToObj(String::valueOf).toArray(String[]::new)
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                int imageId = cursor.getInt(0);
                byte[] blob = cursor.getBlob(1);
                imageMap.put(imageId, blob);
            }
            cursor.close();
        }

        db.close();
        return imageMap;
    }






}
