package com.denniscode.coderquiz;

import android.app.Application;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Get the cache directory for the log file
        File logFile = new File(getCacheDir(), "app_logcat.txt");

        try {
            // Clear previous logs
            Runtime.getRuntime().exec("logcat -c");

            // ✅ Ensure the log file is empty before writing new logs
            FileWriter writer = new FileWriter(logFile, false); // `false` ensures overwrite
            writer.write(""); // Clear the file
            writer.close();

            // ✅ Redirect logcat to file with filtering
            String cmd = "logcat CoderQuiz:D *:S -f " + logFile.getAbsolutePath();
            Runtime.getRuntime().exec(cmd);

        } catch (IOException e) {
            //Log.e("CoderQuiz", e.toString());
        }
    }
}

