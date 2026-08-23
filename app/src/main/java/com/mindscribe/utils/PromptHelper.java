package com.mindscribe.utils;

import java.util.Calendar;

public class PromptHelper {

    private static final String[] PROMPTS = {
            "What made you smile today?",
            "What's a small victory you had today?",
            "What's something you're grateful for right now?",
            "Describe a moment of peace you experienced today.",
            "What challenged you today, and how did you handle it?",
            "What is one thing you learned today?",
            "If today were a color, what would it be and why?",
            "What's a habit you're trying to build or break right now?",
            "Who is someone that inspired you today?",
            "Write about a fleeting thought you had today.",
            "What's something you wish you had done differently today?",
            "What are you looking forward to tomorrow?",
            "How did you take care of yourself today?",
            "What was the most surprising part of your day?"
    };

    public static String getDailyPrompt() {
        Calendar cal = Calendar.getInstance();
        int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
        return PROMPTS[dayOfYear % PROMPTS.length];
    }
}
