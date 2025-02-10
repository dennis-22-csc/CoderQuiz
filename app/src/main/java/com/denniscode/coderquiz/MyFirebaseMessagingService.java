package com.denniscode.coderquiz;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String PREFS_NAME = "FCM_Preferences";
    private static final String KEY_TOKEN_SENT = "token_sent";
    private static final String KEY_SAVED_TOKEN = "saved_token";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        String savedToken = getSavedToken();

        if (savedToken == null || !savedToken.equals(token)) {
            saveToken(token);
            sendTokenToServer(token);
        }

    }


    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Check if the message contains data
        if (remoteMessage.getData().size() > 0) {
            String title = remoteMessage.getData().get("title");
            String message = remoteMessage.getData().get("message");
            String url = remoteMessage.getData().get("url"); // URL may be null

            // Store in SharedPreferences
            saveFCMData(title, message, url);

        }
    }

    private void saveFCMData(String title, String message, String url) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("title", title);
        editor.putString("message", message);
        editor.putString("url", url); // Null-safe, will be empty if null
        editor.putBoolean("data_available", true); // Set flag to true

        editor.apply(); // Apply changes asynchronously
    }

    // Helper function to get current locale (handles API differences)
    private Locale getCurrentLocale(Context context) {
        return context.getResources().getConfiguration().getLocales().get(0);
    }

    private void sendTokenToServer(final String token) {
        if (isTokenSent()) {
            return;
        }

        new Thread(() -> {
            int retryCount = 0;
            final int maxRetries = 3;

            while (retryCount < maxRetries) {
                try {
                    String url = "https://coderquiz.vercel.app/save_token";

                    OkHttpClient client = new OkHttpClient();

                    // Create JSON payload with token and locale
                    JSONObject json = new JSONObject();
                    json.put("user_id", MyBackupAgent.getUserID(this));
                    json.put("firebase_token", token);

                    // Get and add locale information
                    Locale currentLocale = getCurrentLocale(getApplicationContext());
                    json.put("language", currentLocale.getLanguage());

                    String jsonString = json.toString();

                    RequestBody body = RequestBody.create(jsonString, MediaType.parse("application/json; charset=utf-8"));
                    Request request = new Request.Builder()
                            .url(url)
                            .post(body)
                            .build();

                    Response response = client.newCall(request).execute();

                    if (response.isSuccessful()) {
                        setTokenSent(true);
                        saveToken(token);
                        return;
                    } else {
                        retryCount++;
                        Thread.sleep(2000);
                    }
                    response.close();
                } catch (IOException | InterruptedException | JSONException e) {
                    retryCount++;
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

        }).start();
    }

    private boolean isTokenSent() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_TOKEN_SENT, false);
    }

    private void setTokenSent(boolean sent) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_TOKEN_SENT, sent).apply();
    }

    private void saveToken(String token) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_SAVED_TOKEN, token).apply();
    }

    private String getSavedToken() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_SAVED_TOKEN, null);
    }


}
