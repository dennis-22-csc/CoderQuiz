package com.denniscode.coderquiz;

public class Question {

    private int questionID;
    private final int sourceID;

    private final String question;

    private final int imageId;

    private final String optionA;
    private final String optionB;
    private final String optionC;
    private final String optionD;
    private final String correctOption;
    private final String questionCategory;

    private final String quizCategory;
    //private String imageName;
    private String questionStatus;

    public Question(int sourceID, String question, int imageId, String optionA, String optionB, String optionC, String optionD, String correctOption, String questionCategory, String quizCategory) {
        this.sourceID = sourceID;
        this.question = question;
        this.imageId = imageId;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctOption = correctOption;
        this.questionCategory = questionCategory;
        this.quizCategory = quizCategory;
    }

    public int getQuestionID() {
        return questionID;
    }

    public String getQuestion() {
        return question;
    }

    public int getImageId() {
        return imageId;
    }

    public String getOptionA() {
        return optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public String getOptionD() {
        return optionD;
    }

    public String getCorrectOption() {
        return correctOption;
    }

    public String getQuestionCategory() {
        return questionCategory;
    }

    public String getQuizCategory() {
        return quizCategory;
    }
    public String getQuestionStatus() {
        return questionStatus;
    }
    public void setQuestionStatus(String questionStatus) {
        this.questionStatus = questionStatus;
    }

    public void setQuestionID(int questionID) {
        this.questionID = questionID;
    }

    public int getSourceID() {
        return sourceID;
    }

}
