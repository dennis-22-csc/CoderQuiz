package com.denniscode.coderquiz;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "quiz.db";
    private static final int DATABASE_VERSION = 1;

    // Table names
    private static final String TABLE_CATEGORIES = "quiz_categories";
    private static final String TABLE_QUESTIONS = "quiz_questions";

    // Column names for categories
    private static final String COLUMN_CATEGORY_ID = "id";
    private static final String COLUMN_CATEGORY_NAME = "category_name";

    // Column names for questions
    private static final String COLUMN_QUESTION_ID = "id";
    private static final String COLUMN_SOURCE_ID = "source_id";

    private static final String COLUMN_QUESTION = "question";

    private static final String COLUMN_IMAGE = "image";
    private static final String COLUMN_OPTION_A = "option_a";
    private static final String COLUMN_OPTION_B = "option_b";
    private static final String COLUMN_OPTION_C = "option_c";
    private static final String COLUMN_OPTION_D = "option_d";
    private static final String COLUMN_CORRECT_OPTION = "correct_option";
    private static final String COLUMN_QUESTION_CATEGORY = "question_category";

    private static final String COLUMN_QUESTION_STATUS = "status";

    private static final String COLUMN_QUIZ_CATEGORY = "quiz_category";

    public QuizDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // Create categories table with a unique constraint on category_name
        String createCategoriesTable = "CREATE TABLE " + TABLE_CATEGORIES + " ("
                + COLUMN_CATEGORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_CATEGORY_NAME + " TEXT NOT NULL UNIQUE);"; // UNIQUE constraint added
        db.execSQL(createCategoriesTable);

        // Create questions table with a unique constraint on question
        String createQuestionsTable = "CREATE TABLE " + TABLE_QUESTIONS + " ("
                + COLUMN_QUESTION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_SOURCE_ID + " INTEGER NOT NULL, "
                + COLUMN_QUESTION + " TEXT NOT NULL UNIQUE, "
                + COLUMN_IMAGE + " BLOB, "
                + COLUMN_OPTION_A + " TEXT NOT NULL, "
                + COLUMN_OPTION_B + " TEXT NOT NULL, "
                + COLUMN_OPTION_C + " TEXT, "
                + COLUMN_OPTION_D + " TEXT, "
                + COLUMN_CORRECT_OPTION + " TEXT NOT NULL, "
                + COLUMN_QUESTION_CATEGORY + " TEXT NOT NULL, "
                + COLUMN_QUIZ_CATEGORY + " TEXT NOT NULL, "
                + "last_attempted TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "status TEXT DEFAULT 'NEW');"; // Track CORRECT, FAILED, NEW
        db.execSQL(createQuestionsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_QUESTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        onCreate(db);
    }

    // Add category to the database
    public void addCategory(String category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CATEGORY_NAME, category);
        db.insert(TABLE_CATEGORIES, null, values);
        db.close();
    }

    // Get all categories from the database
    /*public List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CATEGORIES, new String[]{COLUMN_CATEGORY_NAME}, null, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                categories.add(cursor.getString(cursor.getColumnIndex(COLUMN_CATEGORY_NAME)));
            } while (cursor.moveToNext());
            cursor.close();
        }

        db.close();
        return categories;
    }*/

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
        values.put(COLUMN_IMAGE, question.getImageBlob());
        values.put(COLUMN_OPTION_A, question.getOptionA());
        values.put(COLUMN_OPTION_B, question.getOptionB());
        values.put(COLUMN_OPTION_C, question.getOptionC());
        values.put(COLUMN_OPTION_D, question.getOptionD());
        values.put(COLUMN_CORRECT_OPTION, question.getCorrectOption());
        values.put(COLUMN_QUESTION_CATEGORY, question.getQuestionCategory());
        values.put(COLUMN_QUIZ_CATEGORY, question.getQuizCategory());

        db.insert(TABLE_QUESTIONS, null, values);
        db.close();
        return true;
    }

    // Get all questions for a specific category
    /*public List<Question> getQuestionsForCategory(String category) {
        List<Question> questions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Query to fetch questions for a specific category
        String selection = COLUMN_QUIZ_CATEGORY + " = ?";
        String[] selectionArgs = {category};

        Cursor cursor = db.query(TABLE_QUESTIONS, new String[]{
                        COLUMN_QUESTION_ID, COLUMN_QUESTION, COLUMN_IMAGE, COLUMN_OPTION_A, COLUMN_OPTION_B, COLUMN_OPTION_C,
                        COLUMN_OPTION_D, COLUMN_CORRECT_OPTION, COLUMN_QUESTION_CATEGORY, COLUMN_QUIZ_CATEGORY},
                selection, selectionArgs, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                int questionID = cursor.getInt(cursor.getColumnIndex(COLUMN_QUESTION_ID));
                String questionText = cursor.getString(cursor.getColumnIndex(COLUMN_QUESTION));

                // Extract the BLOB (byte[]) from the database directly
                byte[] questionImage = cursor.getBlob(cursor.getColumnIndex(COLUMN_IMAGE));

                String optionA = cursor.getString(cursor.getColumnIndex(COLUMN_OPTION_A));
                String optionB = cursor.getString(cursor.getColumnIndex(COLUMN_OPTION_B));
                String optionC = cursor.getString(cursor.getColumnIndex(COLUMN_OPTION_C));
                String optionD = cursor.getString(cursor.getColumnIndex(COLUMN_OPTION_D));
                String correctOption = cursor.getString(cursor.getColumnIndex(COLUMN_CORRECT_OPTION));
                String questionCategory = cursor.getString(cursor.getColumnIndex(COLUMN_QUESTION_CATEGORY));
                String quizCategory = cursor.getString(cursor.getColumnIndex(COLUMN_QUIZ_CATEGORY));

                // Create a Question object and add it to the list
                Question question = new Question(questionID, questionText, questionImage, optionA, optionB, optionC, optionD, correctOption, questionCategory, quizCategory);
                questions.add(question);
            } while (cursor.moveToNext());
            cursor.close();
        }

        db.close();
        return questions;
    }*/

    public List<Question> getQuizQuestions(String category, int limit) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<Question> questions = new ArrayList<>();

        // Query to fetch failed or new questions by category
        String query = "SELECT * FROM " + TABLE_QUESTIONS + " WHERE " + COLUMN_QUIZ_CATEGORY + " = ? "
                + "AND (status = 'FAILED' OR status = 'NEW') "
                + "ORDER BY CASE WHEN status = 'FAILED' THEN 1 "
                + "WHEN status = 'NEW' THEN 2 ELSE 3 END, last_attempted ASC";

        Cursor cursor = db.rawQuery(query, new String[]{category});

        Map<String, List<Question>> categoryMap = new HashMap<>();

        if (cursor.moveToFirst()) {
            do {
                // Extract question data and create Question objects
                Question question = new Question(
                        cursor.getInt(cursor.getColumnIndex(COLUMN_SOURCE_ID)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_QUESTION)),
                        cursor.getBlob(cursor.getColumnIndex(COLUMN_IMAGE)),
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
                if (!categoryMap.containsKey(questionCategory)) {
                    categoryMap.put(questionCategory, new ArrayList<>());
                }
                categoryMap.get(questionCategory).add(question);
            } while (cursor.moveToNext());
        }
        //cursor.close();

        // Randomly shuffle categories and select up to 5
        List<String> categories = new ArrayList<>(categoryMap.keySet());
        Collections.shuffle(categories);

        // Limit the categories to a maximum of 5
        int selectedCategoryCount = Math.min(categories.size(), 5);
        List<Question> selectedQuestions = new ArrayList<>();

        for (int i = 0; i < selectedCategoryCount; i++) {
            String cat = categories.get(i);
            List<Question> categoryQuestions = categoryMap.get(cat);

            // Shuffle the questions within this category
            Collections.shuffle(categoryQuestions);

            // Select up to the limit of questions
            int questionsToAdd = Math.min(categoryQuestions.size(), limit - selectedQuestions.size());
            selectedQuestions.addAll(categoryQuestions.subList(0, questionsToAdd));

            // If we have reached the limit, break early
            if (selectedQuestions.size() >= limit) {
                break;
            }
        }

        return selectedQuestions;
    }



    /*public List<Question> getQuizQuestions(String category, int limit) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<Question> questions = new ArrayList<>();

        String query = "SELECT * FROM " + TABLE_QUESTIONS + " WHERE " + COLUMN_QUIZ_CATEGORY + " = ? "
                + "ORDER BY CASE WHEN status = 'FAILED' THEN 1 "
                + "WHEN status = 'NEW' THEN 2 ELSE 3 END, last_attempted ASC "
                + "LIMIT ?";

        Cursor cursor = db.rawQuery(query, new String[]{category, String.valueOf(limit)});
        if (cursor.moveToFirst()) {
            do {
                // Extract question data and create Question objects
                Question question = new Question(
                        cursor.getInt(cursor.getColumnIndex(COLUMN_QUESTION_ID)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_QUESTION)),
                        cursor.getBlob(cursor.getColumnIndex(COLUMN_IMAGE)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_OPTION_A)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_OPTION_B)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_OPTION_C)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_OPTION_D)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_CORRECT_OPTION)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_QUESTION_CATEGORY)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_QUIZ_CATEGORY))
                );
                question.setQuestionStatus(cursor.getString((cursor.getColumnIndex(COLUMN_QUESTION_STATUS))));
                questions.add(question);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return questions;
    }*/

    public void updateQuestionStatus(int questionId, boolean isCorrect) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", isCorrect ? "CORRECT" : "FAILED");
        values.put("last_attempted", System.currentTimeMillis()); // Update timestamp
        db.update(TABLE_QUESTIONS, values, COLUMN_QUESTION_ID + " = ?", new String[]{String.valueOf(questionId)});
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
    }



}
