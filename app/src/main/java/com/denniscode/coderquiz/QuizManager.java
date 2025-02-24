package com.denniscode.coderquiz;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class QuizManager {
    Context context;
    QuizDatabaseHelper dbHelper;
    private final String selectedCategory;
    private List<Question> questionList = new ArrayList<>();
    private Map<Integer, byte[]> imageBlobsMap = new HashMap<>();
    private final Map<Integer, Boolean> answeredQuestionsMap = new HashMap<>();
    private final Map<String, Float> categoryPerformance = new HashMap<>();

    private int currentQuestionIndex = 0;
    private int score = 0;
    private String statId;

    public QuizManager(Context context, QuizDatabaseHelper dbHelper, String selectedCategory) {
        this.context = context;
        this.dbHelper = dbHelper;
        this.selectedCategory = selectedCategory;
    }
    public void saveQuizState() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("QuizState", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        String categoryKey = "quiz_" + selectedCategory; // Unique key per category
        editor.putInt(categoryKey + "_currentIndex", currentQuestionIndex);
        editor.putInt(categoryKey + "_score", score);

        // Save the sequence of question IDs
        JSONArray questionIdsArray = new JSONArray();
        for (Question question : questionList) {
            questionIdsArray.put(question.getQuestionID());
        }
        editor.putString(categoryKey + "_questionList", questionIdsArray.toString());

        // Save answered state
        JSONObject answeredMap = new JSONObject();
        for (Map.Entry<Integer, Boolean> entry : answeredQuestionsMap.entrySet()) {
            try {
                answeredMap.put(String.valueOf(entry.getKey()), entry.getValue());
            } catch (JSONException e) {
                //Log.e("CoderQuiz", e.toString());
            }
        }
        editor.putString(categoryKey + "_answeredMap", answeredMap.toString());

        editor.apply(); // Save changes
    }

    public void loadQuiz() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("QuizState", Context.MODE_PRIVATE);
        String categoryKey = "quiz_" + selectedCategory;

        if (sharedPreferences.contains(categoryKey + "_currentIndex")) {
            currentQuestionIndex = sharedPreferences.getInt(categoryKey + "_currentIndex", 0);
            currentQuestionIndex++;
            score = sharedPreferences.getInt(categoryKey + "_score", 0);

            // Restore question sequence
            String questionListJson = sharedPreferences.getString(categoryKey + "_questionList", "");
            if (!questionListJson.isEmpty()) {
                try {
                    JSONArray jsonArray = new JSONArray(questionListJson);
                    List<Integer> questionIds = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        questionIds.add(jsonArray.getInt(i));
                    }
                    //Log.d("CoderQuiz", "Question Ids succesfully restored");
                    questionList = dbHelper.getQuestionsByIds(questionIds);
                    loadImages();
                    //Log.d("CoderQuiz", "Question List succesfully restored");

                } catch (JSONException e) {
                    //Log.e("CoderQuiz", e.toString());
                }
            }

            // Restore answered state
            String answeredMapJson = sharedPreferences.getString(categoryKey + "_answeredMap", "");
            if (!answeredMapJson.isEmpty()) {
                try {
                    JSONObject jsonObject = new JSONObject(answeredMapJson);
                    for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
                        String key = it.next();
                        answeredQuestionsMap.put(Integer.parseInt(key), jsonObject.getBoolean(key));
                    }
                    //Log.d("CoderQuiz", "Answered Questions Map succesfully restored");
                } catch (JSONException e) {
                    //Log.e("CoderQuiz", e.toString());
                }
            }

        } else {
            loadQuestions();  // Load normally if no saved state
        }
    }


    public void clearQuizState() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("QuizState", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String categoryKey = "quiz_" + selectedCategory;

        editor.remove(categoryKey + "_currentIndex");
        editor.remove(categoryKey + "_score");
        editor.remove(categoryKey + "_questionList");
        editor.remove(categoryKey + "_answeredMap");

        editor.apply(); // Clear state
    }

    public Map<String, Float> getCategoryPerformance() {
        // Map to store category performance
        Map<String, Integer[]> categoryData = new HashMap<>();

        for (Question question : questionList) {
            String category = question.getQuestionCategory();

            boolean isCorrect = question.getQuestionStatus().equals("CORRECT");

            // Initialize category data if not already present
            categoryData.computeIfAbsent(category, k -> new Integer[]{0, 0});

            Integer[] counts = categoryData.get(category);

            if (counts != null) {
                if (isCorrect) {
                    counts[0]++; // Increment correct count
                }
                counts[1]++; // Increment total count
            }
        }

        // Calculate performance for each category
        for (Map.Entry<String, Integer[]> entry : categoryData.entrySet()) {
            String category = entry.getKey();
            Integer[] counts = entry.getValue();
            float performance = (counts[0] / (float) counts[1]) * 100; // Correct calculation as float
            categoryPerformance.put(category, performance);
        }

        return categoryPerformance;
    }

    public Bundle getCategoryPerformanceBundle(String categoryPerformanceJson) {
        Bundle categoryPerformanceBundle = new Bundle();

        if (categoryPerformanceJson == null) {
            return categoryPerformanceBundle; // Return empty bundle if JSON is null
        }

        try {
            JSONObject jsonObject = new JSONObject(categoryPerformanceJson);
            Iterator<String> keys = jsonObject.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                categoryPerformanceBundle.putFloat(key, (float) jsonObject.getInt(key));
            }
        } catch (JSONException e) {
            Log.e("CoderQuiz", "Error parsing category performance JSON", e);
        }

        return categoryPerformanceBundle;
    }

    public void loadQuestions() {
        // Get all questions for the selected category
        questionList = dbHelper.getQuizQuestions(selectedCategory, 60);
        loadImages();
    }

    public void loadImages() {
        // Collect all image IDs from questions
        Set<Integer> imageIds = new HashSet<>();
        for (Question q : questionList) {
            if (q.getImageId() > 0) {
                imageIds.add(q.getImageId());
            }
        }

        // Fetch all images at once
        if (!imageIds.isEmpty()) {
            imageBlobsMap = dbHelper.getImageBlobsByIds(imageIds.stream().mapToInt(Integer::intValue).toArray());
        } else {
            imageBlobsMap = new HashMap<>();
        }
    }


    public Question getCurrentQuestion() {
        return questionList.get(currentQuestionIndex);
    }

    public byte[] getCurrentImage() {
        return imageBlobsMap.get(getCurrentQuestion().getImageId());
    }

    public byte[] getQuestionImage(int imageId) {
        if (imageBlobsMap.containsKey(imageId)) {
            return imageBlobsMap.get(imageId);
        } else {
            byte[] imageData = dbHelper.getImageBlobById(imageId);
            if (imageData != null) {
                imageBlobsMap.put(imageId, imageData);
                return imageData;
            }
        }
        return null;
    }

    public boolean moveToNextQuestion() {
        if (currentQuestionIndex < questionList.size() - 1) {
            currentQuestionIndex++;
            return true;
        }
        return false;
    }

    public boolean moveToPreviousQuestion() {
        if (currentQuestionIndex > 0) {
            currentQuestionIndex--;
            return true;
        }
        return false;
    }

    public void saveQuizResults() {
        int correctAnswers = score;
        int totalQuestions = questionList.size();
        int incorrectAnswers = totalQuestions - correctAnswers;
        Map<String, Float> categoryPerformance = getCategoryPerformance();
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
        statId = Util.generateStatId(timeStamp);
        dbHelper.addQuizStat(statId, selectedCategory, correctAnswers, incorrectAnswers, totalQuestions, categoryPerformance, timeStamp);
        clearQuizState();
        MyBackupAgent.backupQuizStats(context, dbHelper.getAllQuizStats());
    }

    public int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public List<Question> getQuestionList(){
        return questionList;
    }

    public void incrementScore(){
        score++;
    }

    public String getStatId(){
        return statId;
    }

    public Map<Integer, Boolean>  getAnsweredQuestionsMap() {
        return answeredQuestionsMap;
    }

    public String unescape(String text) {
        return text.replaceAll("\\\\n", "\\\n");
    }
}
