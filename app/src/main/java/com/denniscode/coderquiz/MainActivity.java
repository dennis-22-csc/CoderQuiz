package com.denniscode.coderquiz;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.HashMap;
import java.util.Map;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    private QuizDatabaseHelper dbHelper;
    SharedPreferences sharedPreferences;
    boolean dataAvailable;
    AlertDialog dashboardProgressDialog;
    private ProgressBar progressBar;
    private TextView progressText;

    private Map<String, byte[]> imageCache = new HashMap<>();
    private ActivityResultLauncher<Intent> pickFileLauncher;
    private boolean isQuizCategoriesLayout = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseMessaging.getInstance().subscribeToTopic("cq_all");

        dbHelper = new QuizDatabaseHelper(this);

        sharedPreferences = getSharedPreferences("FCM_Preferences", Context.MODE_PRIVATE);
        dataAvailable = sharedPreferences.getBoolean("data_available", false);

        // Button to open file picker
        Button syncButton = findViewById(R.id.syncButton);
        syncButton.setOnClickListener(v -> openFilePicker());

        // Button to view dashboard
        Button dashboardButton = findViewById(R.id.viewDashboardButton);
        dashboardButton.setOnClickListener(v -> {
         showDashboard();
        });


        // Button to start the quiz
        Button startQuizButton = findViewById(R.id.startQuizButton);
        startQuizButton.setOnClickListener(v -> {
            if (dbHelper.getCategories().isEmpty()) {
                showDialog(this, "Please load a quiz file first", "Would you like to download a quiz file?", "https://dennis-22-csc.github.io/CoderQuiz/quiz_download.html");
            } else {
                showFCMData(this);
                //showQuizCategories();
                showQuizCategoriesFragment();
            }
        });

        pickFileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Intent data = result.getData();
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    AlertDialog progressDialog = new MaterialAlertDialogBuilder(MainActivity.this)
                            .setCancelable(false)
                            .setView(createProgressBar())
                            .create();
                    progressDialog.show();

                    new Thread(() -> {
                        try {
                            String zipResult = FileUtil.handleZipFile(this, uri, dbHelper, imageCache);

                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, zipResult, Toast.LENGTH_LONG).show();
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, "Error processing file", Toast.LENGTH_LONG).show();
                            });
                        } finally {
                            runOnUiThread(progressDialog::dismiss);
                        }
                    }).start();
                } else {
                    Toast.makeText(MainActivity.this, "No file selected", Toast.LENGTH_SHORT).show();
                }
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isQuizCategoriesLayout) {
                    getSupportFragmentManager().popBackStack();
                    findViewById(R.id.rootLayout).setVisibility(View.VISIBLE);
                    isQuizCategoriesLayout = false;
                } else {
                    finish();
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

        progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true); // Indeterminate progress

        progressText = new TextView(this);
        progressText.setText("Processing...");
        progressText.setGravity(Gravity.CENTER);

        layout.addView(progressBar);
        layout.addView(progressText);
        return layout;
    }




    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/zip");
        pickFileLauncher.launch(intent);
    }

    private void showFCMData(Context context) {
        if (dataAvailable) {
            String title = sharedPreferences.getString("title", "No Title");
            String message = sharedPreferences.getString("message", "No Message");
            String url = sharedPreferences.getString("url", null); // URL can be null

            showDialog(context, title, message, url);
            // Reset flag after reading data
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("data_available", false);
            editor.apply();
        }
    }

    private void showDialog(Context context, String title, String message, String url) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (url != null && url.matches("^(https?|ftp)://.*$")) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        context.startActivity(intent);
                    }
                    dialog.dismiss();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(false) // Prevent dismissing by touching outside
                .show();
    }

    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // Dismiss the ProgressDialog
                    dashboardProgressDialog.dismiss();

                }
            }
    );


    private void showDashboard() {
        // Create a ProgressDialog using Material Design principles
        dashboardProgressDialog = new MaterialAlertDialogBuilder(MainActivity.this)
                .setCancelable(false) // Prevent user from dismissing
                .setView(createProgressBar()) // Add styled ProgressBar
                .create();

        // Show the ProgressDialog
        dashboardProgressDialog.show();

        // Start DashboardActivity
        Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
        activityResultLauncher.launch(intent);
    }

    private void showQuizCategoriesFragment() {
        isQuizCategoriesLayout = true;
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new QuizCategoriesFragment()) // Use a FragmentContainerView
                .addToBackStack(null) // Allows back navigation
                .commit();
        // Hide the main layout
        findViewById(R.id.rootLayout).setVisibility(View.GONE);

    }

}
