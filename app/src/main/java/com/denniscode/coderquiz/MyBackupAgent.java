package com.denniscode.coderquiz;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.BackupManager;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MyBackupAgent extends BackupAgentHelper {

    public static final String PREFS = "user_prefs";
    private static String uniqueID = null;
    private static final String PREF_BACKUP_DIALOG_SHOWN = "PREF_BACKUP_DIALOG_SHOWN";
    private static final String PREF_UNIQUE_ID = "PREF_UNIQUE_ID";
    private static final String PREF_QUIZ_STATS = "PREF_QUIZ_STATS";

    @Override
    public void onCreate() {
        super.onCreate();
        addHelper(PREFS, new SharedPreferencesBackupHelper(this, PREFS));
    }

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) throws IOException {
        super.onBackup(oldState, data, newState);
    }
    @Override
    public void onRestore(BackupDataInput data, int size, ParcelFileDescriptor newState) throws IOException {
        super.onRestore(data, size, newState);
        List<Map<String, Object>> stats = MyBackupAgent.restoreQuizStats(this);

        try (QuizDatabaseHelper dbHelper = new QuizDatabaseHelper(this)) {
            dbHelper.addQuizStats(stats);
        }
    }

    public static void setPrefBackupDialogShown(Context context, boolean state) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean(PREF_BACKUP_DIALOG_SHOWN, state);
        editor.apply();

    }

    public static boolean getPrefBackupDialogShown(Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sharedPrefs.getBoolean(PREF_BACKUP_DIALOG_SHOWN, false);
    }

    public static String getUserID(Context context) {
        if (uniqueID == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null);
            if (uniqueID == null) {
                uniqueID = UUID.randomUUID().toString();
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString(PREF_UNIQUE_ID, uniqueID);
                editor.apply();

                // Force backup update
                BackupManager backupManager = new BackupManager(context);
                backupManager.dataChanged();
            }
        }
        return uniqueID;
    }

    public static void backupQuizStats(Context context, List<Map<String, Object>> quizStatList) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();

        try {
            JSONObject quizStatsJson = new JSONObject();

            // Loop through each quiz stat in the list
            for (Map<String, Object> quizStat : quizStatList) {
                JSONObject statJson = new JSONObject();
                statJson.put("id", quizStat.get("id"));
                statJson.put("quiz_category", quizStat.get("quiz_category"));
                statJson.put("correct_answers", quizStat.get("correct_answers"));
                statJson.put("incorrect_answers", quizStat.get("incorrect_answers"));
                statJson.put("total_questions", quizStat.get("total_questions"));

                @SuppressWarnings("unchecked")
                Map<String, Float> categoryPerformance = (Map<String, Float>) quizStat.get("category_performance");
                JSONObject categoryPerformanceJson = new JSONObject();
                if (categoryPerformance != null) {
                    for (Map.Entry<String, Float> entry : categoryPerformance.entrySet()) {
                        categoryPerformanceJson.put(entry.getKey(), entry.getValue());
                    }
                }
                statJson.put("category_performance", categoryPerformanceJson);
                statJson.put("date_time", quizStat.get("date_time"));

                Object idObj = quizStat.get("id");
                if (idObj != null) {
                    quizStatsJson.put((String) idObj, statJson);
                }
            }

            // Save the JSON object to SharedPreferences
            editor.putString(PREF_QUIZ_STATS, quizStatsJson.toString());
            editor.apply();

            // Force backup update
            BackupManager backupManager = new BackupManager(context);
            backupManager.dataChanged();

            //Toast.makeText(context, "Quiz Stats Backed Up!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            //Toast.makeText(context, "Error Saving Quiz Stats!", Toast.LENGTH_LONG).show();
        }
    }

    public static List<Map<String, Object>> restoreQuizStats(Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        String quizStatsJsonString = sharedPrefs.getString(PREF_QUIZ_STATS, null);
        List<Map<String, Object>> quizStatsList = new ArrayList<>();

        if (quizStatsJsonString != null) {
            try {
                // Handle possible encoding issues: Replace &quot; with normal double quotes
                quizStatsJsonString = quizStatsJsonString.replace("&quot;", "\"");

                // Parse the JSON string into a JSONObject
                JSONObject quizStatsJson = new JSONObject(quizStatsJsonString);

                Iterator<String> keys = quizStatsJson.keys();

                // Loop through each stat in the JSON object
                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONObject statJson = quizStatsJson.getJSONObject(key);

                    Map<String, Object> restoredStat = new HashMap<>();

                    // Extract the values from statJson and put them into restoredStat map
                    restoredStat.put("id", statJson.getString("id"));
                    restoredStat.put("quiz_category", statJson.getString("quiz_category"));
                    restoredStat.put("correct_answers", statJson.getInt("correct_answers"));
                    restoredStat.put("incorrect_answers", statJson.getInt("incorrect_answers"));
                    restoredStat.put("total_questions", statJson.getInt("total_questions"));

                    // Handle the category performance map (nested JSONObject)
                    JSONObject categoryPerformanceJson = statJson.getJSONObject("category_performance");
                    Map<String, Float> categoryPerformance = new HashMap<>();
                    Iterator<String> categoryKeys = categoryPerformanceJson.keys();
                    while (categoryKeys.hasNext()) {
                        String categoryKey = categoryKeys.next();
                        categoryPerformance.put(categoryKey, (float) categoryPerformanceJson.getDouble(categoryKey));
                    }

                    // Add category performance and date_time to the map
                    restoredStat.put("category_performance", categoryPerformance);
                    restoredStat.put("date_time", statJson.getString("date_time"));

                    // Add the restored stat to the list
                    quizStatsList.add(restoredStat);
                }
            } catch (Exception e) {
                //Log.e("CoderQuiz", e.toString());
            }
        }

        return quizStatsList;
    }

}