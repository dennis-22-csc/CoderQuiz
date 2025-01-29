package com.denniscode.coderquiz;


import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import android.animation.ObjectAnimator;
import android.view.animation.BounceInterpolator;

import org.json.JSONException;
import org.json.JSONObject;

public class QuizActivity extends AppCompatActivity {

    private QuizDatabaseHelper dbHelper;
    private TextView questionInfo, questionIdInfo, questionText, correctAnswerText;
    private ImageView questionThumbnail, correctAnswerIcon;
    private MaterialButton nextButton;
    private String selectedCategory;
    private List<Question> questionList;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private CardView imageCard, option1Card, option2Card, option3Card, option4Card;
    private TextView option1Text, option2Text, option3Text, option4Text;
    private CardView[] optionCards;
    private boolean isAnswered = false;

    private MaterialButton previousButton;
    private ScrollView quizSegment;
    private String statId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_quiz);

        // Initialize views
        questionInfo = findViewById(R.id.questionInfo);
        questionIdInfo = findViewById(R.id.questionIdInfo);
        questionText = findViewById(R.id.questionText);
        imageCard = findViewById(R.id.imageCard);
        questionThumbnail = findViewById(R.id.questionThumbnail);
        option1Card = findViewById(R.id.option1Card);
        option1Text = findViewById(R.id.option1Text);
        option2Card = findViewById(R.id.option2Card);
        option2Text = findViewById(R.id.option2Text);
        option3Card = findViewById(R.id.option3Card);
        option3Text = findViewById(R.id.option3Text);
        option4Card = findViewById(R.id.option4Card);
        option4Text = findViewById(R.id.option4Text);
        correctAnswerText = findViewById(R.id.correctAnswerText);
        correctAnswerIcon = findViewById(R.id.correctAnswerIcon);
        nextButton = findViewById(R.id.nextButton);
        previousButton = findViewById(R.id.backButton);
        quizSegment = findViewById(R.id.quizSegment);



        // Set onClick listener for the thumbnail to open the modal
        questionThumbnail.setOnClickListener(v -> showImageModal());

        dbHelper = new QuizDatabaseHelper(this);

        // Retrieve the selected category from MainActivity
        selectedCategory = getIntent().getStringExtra("CATEGORY");

        //Toast.makeText(getApplicationContext(), selectedCategory, Toast.LENGTH_SHORT).show();


        dbHelper.resetCorrectQuestions(selectedCategory);

        // Load questions
        loadQuestions();

        // Display the first question
        displayQuestion(currentQuestionIndex);

        nextButton.setOnClickListener(v -> {
            currentQuestionIndex++;
            if (currentQuestionIndex < questionList.size()) {
                resetOptionsBackground(optionCards);
                displayQuestion(currentQuestionIndex);
            } else {
                // Handle quiz completion
                previousButton.setVisibility(View.GONE);
                quizSegment.setVisibility(View.GONE);
                nextButton.setVisibility(View.GONE);
                showQuizCompletedDialog();
                int correctAnswers = score;
                int totalQuestions = questionList.size();
                int incorrectAnswers = totalQuestions - correctAnswers;
                Map<String, Float> categoryPerformance = getCategoryPerformance();
                String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
                statId = generateStatId(timeStamp);

                dbHelper.addQuizStat(statId, selectedCategory, correctAnswers, incorrectAnswers, totalQuestions, categoryPerformance, timeStamp);
                }
        });

        previousButton.setOnClickListener(v -> {
            //Toast.makeText(getApplicationContext(), String.valueOf(currentQuestionIndex), Toast.LENGTH_SHORT).show();
            if (currentQuestionIndex >= 0)
                currentQuestionIndex--;
            if (currentQuestionIndex < 0) {
                finish();
            } else {
                resetOptionsBackground(optionCards);
                displayQuestion(currentQuestionIndex);
            }
        });

    }

    private void loadQuestions() {
        // Get all questions for the selected category
        questionList = dbHelper.getQuizQuestions(selectedCategory, 60);
        //Toast.makeText(this, String.valueOf(questionList.size()), Toast.LENGTH_LONG).show();
    }

    private void displayQuestion(int index) {
        Question question = questionList.get(index);
        questionText.setText(question.getQuestion());

        // Update the question number and remaining questions
        questionInfo.setText("Question " + (index + 1) + " of " + questionList.size());

        // Update the question ID
        questionIdInfo.setText("Source ID: " + question.getSourceID());

        // Update question thumbnail
        byte[] imageBlob = question.getImageBlob();
        if (imageBlob != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBlob, 0, imageBlob.length);
            questionThumbnail.setImageBitmap(bitmap);
            imageCard.setVisibility(View.VISIBLE);
            questionThumbnail.setVisibility(View.VISIBLE);
        } else {
            questionThumbnail.setVisibility(View.GONE);
            imageCard.setVisibility(View.GONE);
        }

        correctAnswerText.setText("");
        correctAnswerIcon.setImageDrawable(null);


        // Create a list of the available options dynamically
        List<String> options = new ArrayList<>();
        if (question.getOptionA() != null) options.add(question.getOptionA());
        if (question.getOptionB() != null) options.add(question.getOptionB());
        if (question.getOptionC() != null) options.add(question.getOptionC());
        if (question.getOptionD() != null) options.add(question.getOptionD());

        // Shuffle the options list
        Collections.shuffle(options);

        String correctAnswer = question.getCorrectOption();


        // Directly refer to the option components
        optionCards = new CardView[] { option1Card, option2Card, option3Card, option4Card };
        TextView[] optionTexts = {option1Text, option2Text, option3Text, option4Text};

        // Reset isAnswered flag and enable all options
        isAnswered = false;
        enableOptionCards(optionCards, true);

        // Ensure the last option is in focus
        option4Card.getParent().requestChildFocus(option4Card, option4Card);

        // Show or hide cards based on the number of options
        for (int i = 0; i < optionCards.length; i++) {
            CardView optionCard = optionCards[i];
            TextView optionText = optionTexts[i];

            if (i < options.size()) {
                // Set the option text and make the card visible
                optionText.setText(options.get(i));
                optionCard.setVisibility(View.VISIBLE);
            } else {
                // Hide the card if there are fewer than 4 options
                optionCard.setVisibility(View.GONE);
            }

            // Set onClickListener for options
            optionCard.setOnClickListener(v -> {
                if (!isAnswered) {  // Only process if the question hasn't been answered
                    resetOptionsBackground(optionCards);
                    optionCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.optionsColor));

                    if (optionText.getText().toString().equals(correctAnswer)) {
                        highlightCorrectAnswer(optionCard, true, optionText.getText().toString());
                        // Update the status of a question after an answer is submitted
                        dbHelper.updateQuestionStatus(question.getQuestionID(), true);
                        question.setQuestionStatus("CORRECT");
                    } else {
                        highlightCorrectAnswer(optionCard, false, correctAnswer);
                        // Update the status of a question after an answer is submitted
                        dbHelper.updateQuestionStatus(question.getQuestionID(), false);
                        question.setQuestionStatus("FAILED");
                    }

                    // Mark the question as answered and disable further clicks
                    isAnswered = true;
                    enableOptionCards(optionCards, false);
                }
            });
        }
    }

    private void resetOptionsBackground(CardView[] optionCards) {
        for (CardView optionCard : optionCards) {
            optionCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.defaultCardColor));
        }
    }


    private void highlightCorrectAnswer(CardView selectedCard, boolean isCorrect, String correctAnswer) {
        if (isCorrect) {
            correctAnswerIcon.setImageResource(R.drawable.correct);
            correctAnswerText.setText("Correct Answer: " + correctAnswer);
            selectedCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.correctColor));
            score++;
        } else {
            correctAnswerIcon.setImageResource(R.drawable.incorrect);
            correctAnswerText.setText("Correct Answer: " + correctAnswer);
            selectedCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.incorrectColor));
        }



        // Request focus on the correct answer text
        correctAnswerText.requestFocus();

        // Scroll the ScrollView to ensure the correct answer text is fully visible
        quizSegment.post(() -> {
            View lastChild = quizSegment.getChildAt(quizSegment.getChildCount() - 1);
            int bottom = lastChild.getBottom() + quizSegment.getPaddingBottom();
            int sy = quizSegment.getScrollY();
            int sh = quizSegment.getHeight();
            int delta = bottom - (sy + sh);

            quizSegment.smoothScrollBy(0, delta);
        });
    }

    private void enableOptionCards(CardView[] optionCards, boolean enable) {
        for (CardView optionCard : optionCards) {
            optionCard.setEnabled(enable);
        }
    }


    private void showStats() {
        // Instantiate the DB helper
        QuizDatabaseHelper dbHelper = new QuizDatabaseHelper(this);

        // Retrieve the stats associated with the statId
        Map<String, Object> quizStat = dbHelper.getQuizStatById(statId);

        // Check if the quizStat is not null before proceeding
        if (quizStat != null) {
                // Extract relevant data from the Map
                int correctAnswers = (int) quizStat.get("correct_answers");
                int incorrectAnswers = (int) quizStat.get("incorrect_answers");
                int totalQuestions = (int) quizStat.get("total_questions");
                String timeStamp = formatTimestamp((String) quizStat.get("date_time"));

                String categoryPerformanceJson = (String) quizStat.get("category_performance");

                // Prepare the intent to pass the data to StatsActivity
                Intent intent = new Intent(this, StatsActivity.class);

                // Pass the data to the intent
                intent.putExtra("CORRECT_ANSWERS", correctAnswers);
                intent.putExtra("INCORRECT_ANSWERS", incorrectAnswers);
                intent.putExtra("TOTAL_QUESTIONS", totalQuestions);
                intent.putExtra("TIME_STAMP", timeStamp);

            try {
                JSONObject jsonObject = new JSONObject(categoryPerformanceJson);

                // Create a Bundle to hold the category performance values
                Bundle categoryPerformanceBundle = new Bundle();

                // Loop through the keys in the JSON object and put them into the Bundle
                Iterator<String> keys = jsonObject.keys();

                while (keys.hasNext()) {
                    String key = keys.next();
                    categoryPerformanceBundle.putFloat(key, (float) jsonObject.getInt(key));
                }

                // Pass the categoryPerformanceBundle with Intent
                intent.putExtra("CATEGORY_PERFORMANCE", categoryPerformanceBundle);
            } catch (JSONException e) {
                // Handle the JSONException appropriately
                e.printStackTrace();
                // Optionally show a user-friendly message or take other actions
            }

            // Start StatsActivity
                startActivity(intent);
        } else {
            Toast.makeText(this, "No data found for statId: " + statId, Toast.LENGTH_LONG).show();
        }
    }

    private void showImageModal() {
        // Create and configure the dialog
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_image);
        dialog.getWindow().setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
        );

        // Set the image in the modal
        ImageView fullImageView = dialog.findViewById(R.id.fullImageView);
        fullImageView.setImageDrawable(questionThumbnail.getDrawable()); // Use the same image as the thumbnail

        // Close button logic
        ImageButton closeButton = dialog.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }


    private Map<String, Float> getCategoryPerformance() {
        // Map to store category performance
        Map<String, Integer[]> categoryData = new HashMap<>();

        for (Question question : questionList) {
            String category = question.getQuestionCategory();

            boolean isCorrect = question.getQuestionStatus().equals("CORRECT");

            // Initialize category data if not already present
            categoryData.putIfAbsent(category, new Integer[]{0, 0}); // {correctCount, totalCount}

            // Update category data
            Integer[] counts = categoryData.get(category);
            if (isCorrect) {
                counts[0]++; // Increment correct count
            }
            counts[1]++; // Increment total count
        }

        // Calculate performance for each category
        Map<String, Float> categoryPerformance = new HashMap<>();
        for (Map.Entry<String, Integer[]> entry : categoryData.entrySet()) {
            String category = entry.getKey();
            Integer[] counts = entry.getValue();
            float performance = (counts[0] / (float) counts[1]) * 100; // Correct calculation as float
            categoryPerformance.put(category, performance);
        }

        return categoryPerformance;
    }

    private void showQuizCompletedDialog() {
        // Create a dialog instance
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_quiz_completed);
        dialog.setCancelable(false); // Make the dialog non-cancellable

        // Set the dialog size
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        // Find views
        TextView animatedText = dialog.findViewById(R.id.animatedText);
        MaterialButton finishButton = dialog.findViewById(R.id.finishButton);
        MaterialButton showStatsButton = dialog.findViewById(R.id.showStatsButton);

        // Add animation to the text
        ObjectAnimator animator = ObjectAnimator.ofFloat(animatedText, "translationY", -50f, 50f);
        animator.setDuration(1000);
        animator.setInterpolator(new BounceInterpolator());
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setRepeatMode(ObjectAnimator.REVERSE);
        animator.start();

        // Set button actions
        finishButton.setOnClickListener(view -> {
            finish(); // Finish the activity
        });

        showStatsButton.setOnClickListener(view -> {
            showStats();
        });

        // Show the dialog
        dialog.show();
    }

    public String generateStatId(String timeStamp) {
        String uuid = UUID.randomUUID().toString();
        return uuid + "_" + timeStamp;
    }

    private String formatTimestamp(String timestampStr) {
        // Define the input format (yyyyMMddHHmmss)
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        // Parse the input timestamp to a LocalDateTime object
        LocalDateTime dateTime = LocalDateTime.parse(timestampStr, inputFormatter);

        // Define the desired output format (e.g., "Tuesday Jan 2 2025 4:30 PM")
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("EEEE MMM d yyyy h:mm a");

        // Format the LocalDateTime to the desired string
        return dateTime.format(outputFormatter);
    }

}
