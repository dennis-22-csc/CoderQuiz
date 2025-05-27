package com.denniscode.coderquiz;


import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.animation.ObjectAnimator;
import android.view.animation.BounceInterpolator;

public class QuizActivity extends AppCompatActivity {

    private QuizDatabaseHelper dbHelper;
    private TextView questionInfo, questionIdInfo, questionText, correctAnswerText, referenceTextView;
    private ImageView questionImage,questionThumbnail;
    private MaterialButton nextButton;
    private CardView imageCard, option1Card, option2Card, option3Card, option4Card;
    private TextView option1Text, option2Text, option3Text, option4Text;
    private CardView[] optionCards;
    private MaterialButton previousButton;
    private ScrollView quizSegment;
    private final Map<CardView, Integer> defaultColors = new HashMap<>();
    private CardView lastSelectedCard = null;
    private QuizManager quizManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_quiz);

        // Initialize views
        questionInfo = findViewById(R.id.questionInfo);
        questionIdInfo = findViewById(R.id.questionIdInfo);
        questionText = findViewById(R.id.questionText);
        questionImage = findViewById(R.id.questionImage);
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
        referenceTextView = findViewById(R.id.referenceText);
        nextButton = findViewById(R.id.nextButton);
        previousButton = findViewById(R.id.backButton);
        quizSegment = findViewById(R.id.quizSegment);



        // Set onClick listener for the thumbnail to open the modal
        questionThumbnail.setOnClickListener(v -> showImageModal());

        dbHelper = new QuizDatabaseHelper(this);

        // Retrieve the selected category from MainActivity
        String selectedCategory = getIntent().getStringExtra("CATEGORY");

        quizManager = new QuizManager(this, dbHelper, selectedCategory);

        dbHelper.resetCorrectQuestions(selectedCategory);

        // Load quiz
        quizManager.loadQuiz();

        // Display the first question
        displayCurrentQuestion();

        nextButton.setOnClickListener(v -> {
            if (quizManager.moveToNextQuestion()) {
                if (quizManager.getCurrentQuestionIndex() < quizManager.getQuestionList().size()) {
                    if (lastSelectedCard != null) {
                        resetOptionsBackground(lastSelectedCard); // Remove previous highlight
                    }
                    displayCurrentQuestion();
                }

            } else {
                previousButton.setVisibility(View.GONE);
                quizSegment.setVisibility(View.GONE);
                nextButton.setVisibility(View.GONE);
                handleQuizCompletion();
            }
        });

        previousButton.setOnClickListener(v -> {
            if (quizManager.moveToPreviousQuestion()) {
                if (lastSelectedCard != null) {
                    resetOptionsBackground(lastSelectedCard); // Remove previous highlight
                }
                enableOptionCards(optionCards, false);
                displayCurrentQuestion();
            } else {
                finish();
            }
        });

    }

        private void displayCurrentQuestion() {
            Question question = quizManager.getCurrentQuestion();
            String questionString = quizManager.unescape(question.getQuestion());
            if (questionString.equals("IMG")) {
                if (questionImage.getVisibility() == View.VISIBLE) {
                    questionImage.setVisibility(View.GONE);
                }
                questionText.setVisibility(View.GONE);
            } else if (questionString.startsWith("IMG_")) {
                try {
                    int imageId = Integer.parseInt(questionString.substring(4));
                    byte[] imageBlob = quizManager.getQuestionImage(imageId);
                    if (imageBlob != null) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBlob, 0, imageBlob.length);
                        questionImage.setImageBitmap(bitmap);
                        questionText.setVisibility(View.GONE);
                        questionImage.setVisibility(View.VISIBLE);
                    }

                } catch (NumberFormatException e) {

                }
            } else {
                if (questionImage.getVisibility() == View.VISIBLE) {
                    questionImage.setVisibility(View.GONE);
                }
                if (questionText.getVisibility() != View.VISIBLE) {
                    questionText.setVisibility(View.VISIBLE);
                }
                questionText.setText(questionString);
            }
            // Directly refer to the option components
            optionCards = new CardView[]{option1Card, option2Card, option3Card, option4Card};
            TextView[] optionTexts = {option1Text, option2Text, option3Text, option4Text};

            // Reset isAnswered flag and enable all options
            boolean isAnswered = Boolean.TRUE.equals(quizManager.getAnsweredQuestionsMap().get(quizManager.getCurrentQuestionIndex()));
            enableOptionCards(optionCards, !isAnswered);

            // Update the question number and remaining questions
            questionInfo.setText(getString(R.string.question_info, quizManager.getCurrentQuestionIndex() + 1, quizManager.getQuestionList().size()));

            // Update the question ID
            questionIdInfo.setText(getString(R.string.source_id_info, question.getSourceID()));

            // Update question thumbnail
            byte[] imageBlob = quizManager.getCurrentImage();
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
            correctAnswerText.setCompoundDrawables(null, null, null, null);
            referenceTextView.setText("");

            // Create a list of the available options dynamically
            List<String> options = new ArrayList<>();
            if (question.getOptionA() != null) options.add(question.getOptionA());
            if (question.getOptionB() != null) options.add(question.getOptionB());
            if (question.getOptionC() != null) options.add(question.getOptionC());
            if (question.getOptionD() != null) options.add(question.getOptionD());

            // Shuffle the options list
            Collections.shuffle(options);

            String correctAnswer = question.getCorrectOption();


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
                        // Store the default color only once
                        if (!defaultColors.containsKey(optionCard)) {
                            defaultColors.put(optionCard, optionCard.getCardBackgroundColor().getDefaultColor());
                        }
                        if (lastSelectedCard != null) {
                            resetOptionsBackground(lastSelectedCard); // Remove previous highlight
                        }
                        lastSelectedCard = optionCard;

                        if (optionText.getText().toString().equals(correctAnswer)) {
                            highlightCorrectAnswer(optionCard, true, question);
                            // Update the status of a question after an answer is submitted
                            dbHelper.updateQuestionStatus(question.getQuizCategory(), question.getQuestionID(), true);
                            question.setQuestionStatus("CORRECT");
                        } else {
                            highlightCorrectAnswer(optionCard, false, question);
                            // Update the status of a question after an answer is submitted
                            dbHelper.updateQuestionStatus(question.getQuizCategory(), question.getQuestionID(), false);
                            question.setQuestionStatus("FAILED");
                        }

                        // Mark the question as answered and disable further clicks
                        quizManager.getAnsweredQuestionsMap().put(quizManager.getCurrentQuestionIndex(), true);
                        enableOptionCards(optionCards, false);
                        quizManager.saveQuizState();
                    }
                });
            }
    }

    private void resetOptionsBackground(CardView selectedCard) {
        if (defaultColors.containsKey(selectedCard)) {
            Integer color = defaultColors.get(selectedCard);
            if (color != null) {
                selectedCard.setCardBackgroundColor(color);
            }
        }
    }

    private void highlightCorrectAnswer(CardView selectedCard, boolean isCorrect, Question question) {

        String referenceUrl = "https://dennis-22-csc.github.io/CoderQuiz/docs/"
                    + question.getQuizCategory()
                    + "/index.html#question"
                    + question.getQuestionID();

        correctAnswerText.setText("Correct Answer: " + question.getCorrectOption());

        SpannableString spannable = new SpannableString("View Reference");
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent intent = new Intent(QuizActivity.this, WebViewActivity.class);
                intent.putExtra("url", referenceUrl);
                startActivity(intent);
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.BLUE); // Link color
                ds.setUnderlineText(true); // Underline
            }
        };

        spannable.setSpan(clickableSpan, 0, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        referenceTextView.setText(spannable);
        referenceTextView.setMovementMethod(LinkMovementMethod.getInstance());

        if (isCorrect) {
            Drawable icon = ContextCompat.getDrawable(this, R.drawable.correct);
            correctAnswerText.setCompoundDrawablesWithIntrinsicBounds(resize(icon), null, null, null);
            //correctAnswerText.setText(getString(R.string.correct_answer_text, correctAnswer));
            selectedCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.correctColor));
            quizManager.incrementScore();
        } else {
            Drawable icon = ContextCompat.getDrawable(this, R.drawable.incorrect);
            correctAnswerText.setCompoundDrawablesWithIntrinsicBounds(resize(icon), null, null, null);
            //correctAnswerText.setText(getString(R.string.correct_answer_text, correctAnswer));
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

    private Drawable resize(Drawable image) {
        Bitmap b = ((BitmapDrawable)image).getBitmap();
        Bitmap bitmapResized = Bitmap.createScaledBitmap(b, 24, 24, false);
        return new BitmapDrawable(getResources(), bitmapResized);
    }

    private void enableOptionCards(CardView[] optionCards, boolean enable) {
        for (CardView optionCard : optionCards) {
            optionCard.setClickable(enable);
            optionCard.setFocusable(enable);
            optionCard.setAlpha(enable ? 1.0f : 0.5f); // Optional: visually indicate state
        }
    }
    private void showStats() {
        Map<String, Object> quizStat;
        // Instantiate the DB helper
        try (QuizDatabaseHelper dbHelper = new QuizDatabaseHelper(this)) {
            // Retrieve the stats associated with the statId
            quizStat = dbHelper.getQuizStatById(quizManager.getStatId());
        }

        // Check if the quizStat is not null before proceeding
        if (quizStat != null) {
            // Extract relevant data from the Map
            Integer correct = (Integer) quizStat.getOrDefault("correct_answers", 0);
            Integer incorrect = (Integer) quizStat.getOrDefault("incorrect_answers", 0);
            Integer total = (Integer) quizStat.getOrDefault("total_questions", 0);

            int correctAnswers = correct == null ? 0 : correct;
            int incorrectAnswers = incorrect == null ? 0 : incorrect;
            int totalQuestions = total == null ? 0 : total;

            String timeStamp = Util.formatTimestamp((String) quizStat.get("date_time"));

            String categoryPerformanceJson = (String) quizStat.get("category_performance");


            // Prepare the intent to pass the data to StatsActivity
                Intent intent = new Intent(this, StatsActivity.class);

                // Pass the data to the intent
                intent.putExtra("CORRECT_ANSWERS", correctAnswers);
                intent.putExtra("INCORRECT_ANSWERS", incorrectAnswers);
                intent.putExtra("TOTAL_QUESTIONS", totalQuestions);
                intent.putExtra("TIME_STAMP", timeStamp);

                Bundle categoryPerformanceBundle = quizManager.getCategoryPerformanceBundle(categoryPerformanceJson);
                intent.putExtra("CATEGORY_PERFORMANCE", categoryPerformanceBundle);

                // Start StatsActivity
                startActivity(intent);
            } else {
                Toast.makeText(this, "No data found for statId: " + quizManager.getStatId(), Toast.LENGTH_LONG).show();
            }
    }

    private void showImageModal() {
        // Create and configure the dialog
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_image);

        // Set the image in the modal
        ImageView fullImageView = dialog.findViewById(R.id.fullImageView);
        fullImageView.setImageDrawable(questionThumbnail.getDrawable()); // Use the same image as the thumbnail

        // Close button logic
        ImageButton closeButton = dialog.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }

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
            if (!MyBackupAgent.getPrefBackupDialogShown(this)) {
                showBackupDialog(this);
            } else {
                finish(); // Finish the activity
            }
        });

        showStatsButton.setOnClickListener(view -> showStats());

        // Show the dialog
        dialog.show();
    }

    private void showBackupDialog(Context context) {
        // 1. Inflate a custom layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_backup, null); // dialog_backup.xml

        // 2. Find the ImageView in your layout
        ImageView imageView = dialogView.findViewById(R.id.backup_image); // ID from dialog_backup.xml

        // 3. Set the image (you can load from resources, URI, etc.)
        imageView.setImageResource(R.drawable.backup_image);
        Button okayButton = dialogView.findViewById(R.id.okay_button);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle("Enable automatic backups of stats")
                .setMessage("If you would like your stats to backup to your Google Drive, toggle the option to enable mobile data backup in your settings screen")
                .setView(dialogView)
                .setCancelable(false)
                .create(); // Create the dialog

        okayButton.setOnClickListener(v -> { // Set listener on the custom button
            Intent intent = new Intent();
            intent.setAction("android.settings.BACKUP_AND_RESET_SETTINGS");
            startActivity(intent);
            dialog.dismiss();
        });

        dialog.show();
        MyBackupAgent.setPrefBackupDialogShown(this, true);
    }

    private void handleQuizCompletion() {
        quizManager.saveQuizResults();
        showQuizCompletedDialog();
    }





}
