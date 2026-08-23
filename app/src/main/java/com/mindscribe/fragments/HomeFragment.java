package com.mindscribe.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mindscribe.R;
import com.mindscribe.activities.AddNoteActivity;
import com.mindscribe.activities.EditNoteActivity;
import com.mindscribe.activities.MainActivity;
import com.mindscribe.models.Note;
import com.mindscribe.utils.FirebaseHelper;
import com.mindscribe.utils.PromptHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import android.graphics.Color;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Home / Dashboard Fragment.
 * Shows greeting, stats (total notes, favorites), and quick add FAB.
 */
public class HomeFragment extends Fragment {

    private TextView tvGreeting, tvDate;
    private TextView tvGreetingEmoji;
    private TextView tvTotalNotes, tvFavorites;
    private TextView tvStreak, tvStreakDetail, tvTopMood;
    private FloatingActionButton fabAdd;
    private BarChart barChartMoods;
    private TextView tvDailyPrompt;
    private MaterialButton btnWriteJournal;
    private String todayJournalId = null;

    private FirebaseHelper firebaseHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseHelper = FirebaseHelper.getInstance();

        initViews(view);
        setGreeting();
        loadStats();
        setListeners();

        // Animate cards in
        view.startAnimation(android.view.animation.AnimationUtils
                .loadAnimation(requireContext(), R.anim.fade_in));
    }

    private void initViews(View view) {
        tvGreeting   = view.findViewById(R.id.tv_greeting);
        tvGreetingEmoji = view.findViewById(R.id.tv_greeting_emoji);
        tvDate       = view.findViewById(R.id.tv_date);
        tvTotalNotes = view.findViewById(R.id.tv_total_notes);
        tvFavorites  = view.findViewById(R.id.tv_favorites);
        tvStreak     = view.findViewById(R.id.tv_streak);
        tvStreakDetail = view.findViewById(R.id.tv_streak_detail);
        tvTopMood    = view.findViewById(R.id.tv_top_mood);
        fabAdd       = view.findViewById(R.id.fab_add);
        barChartMoods = view.findViewById(R.id.bar_chart_moods);
        tvDailyPrompt = view.findViewById(R.id.tv_daily_prompt);
        btnWriteJournal = view.findViewById(R.id.btn_write_journal);

        if (tvDailyPrompt != null) {
            String prompt = PromptHelper.getDailyPrompt();
            tvDailyPrompt.setText(prompt);
            tvDailyPrompt.setTag(prompt);
        }

        setupBarChart();
    }

    private void setGreeting() {
        // Dynamic greeting based on time of day
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String timeGreeting;
        if (hour < 12) {
            timeGreeting = "Good Morning";
            if (tvGreetingEmoji != null) tvGreetingEmoji.setText("☀️");
        } else if (hour < 17) {
            timeGreeting = "Good Afternoon";
            if (tvGreetingEmoji != null) tvGreetingEmoji.setText("🌤️");
        } else {
            timeGreeting = "Good Evening";
            if (tvGreetingEmoji != null) tvGreetingEmoji.setText("🌙");
        }

        // Get user name from Firestore
        firebaseHelper.getUserDocument()
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && isAdded()) {
                        String name = doc.getString("name");
                        String firstName = name != null && name.contains(" ")
                                ? name.split(" ")[0] : name;
                        tvGreeting.setText(timeGreeting + ", " + firstName);
                    } else if (isAdded()) {
                        tvGreeting.setText(timeGreeting);
                    }
                });

        // Set date
        String date = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
                .format(Calendar.getInstance().getTime());
        tvDate.setText(date);
    }

    private void loadStats() {
        // Total notes count via Firestore
        firebaseHelper.getNotesQuery()
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (isAdded()) {
                        tvTotalNotes.setText(String.valueOf(snapshot.size()));
                    }
                });

        // Favorites count
        firebaseHelper.getFavoriteNotes()
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (isAdded()) {
                        tvFavorites.setText(String.valueOf(snapshot.size()));
                    }
                });

        firebaseHelper.getNotesQuery()
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded()) return;

                    Set<String> writtenDays = new HashSet<>();
                    Map<String, Integer> moodCounts = new HashMap<>();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        Note note = doc.toObject(Note.class);
                        if (note == null) continue;

                        if (note.getCreatedAt() != null) {
                            String noteDay = dayKey(note.getCreatedAt().toDate());
                            writtenDays.add(noteDay);
                            
                            // Check if there is a journal entry today
                            if ("Journal".equals(note.getCategory()) && noteDay.equals(dayKey(Calendar.getInstance().getTime()))) {
                                todayJournalId = note.getNoteId();
                            }
                        }

                        String mood = note.getMood();
                        moodCounts.put(mood, moodCounts.containsKey(mood)
                                ? moodCounts.get(mood) + 1 : 1);
                    }

                    if (btnWriteJournal != null) {
                        btnWriteJournal.setText(todayJournalId != null ? "Edit Entry" : "Write Entry");
                    }

                    int currentStreak = calculateCurrentStreak(writtenDays);
                    if (tvStreak != null) tvStreak.setText(String.valueOf(currentStreak));
                    if (tvStreakDetail != null) tvStreakDetail.setText(String.valueOf(currentStreak));
                    if (tvTopMood != null) tvTopMood.setText(getTopMood(moodCounts));
                    updateBarChart(moodCounts);
                });
    }

    private int calculateCurrentStreak(Set<String> writtenDays) {
        Calendar cursor = Calendar.getInstance();
        int streak = 0;
        
        // If today is written, count it and move to yesterday.
        if (writtenDays.contains(dayKey(cursor.getTime()))) {
            streak++;
            cursor.add(Calendar.DAY_OF_YEAR, -1);
        } else {
            // Today is not written, let's start checking from yesterday
            cursor.add(Calendar.DAY_OF_YEAR, -1);
        }

        while (writtenDays.contains(dayKey(cursor.getTime()))) {
            streak++;
            cursor.add(Calendar.DAY_OF_YEAR, -1);
        }

        return streak;
    }

    private String dayKey(Date date) {
        return new SimpleDateFormat("yyyyMMdd", Locale.US).format(date);
    }

    private String getTopMood(Map<String, Integer> moodCounts) {
        String topMood = Note.MOOD_CALM;
        int topCount = 0;

        for (Map.Entry<String, Integer> entry : moodCounts.entrySet()) {
            if (entry.getValue() > topCount) {
                topMood = entry.getKey();
                topCount = entry.getValue();
            }
        }

        return topCount > 0 ? topMood : "None";
    }

    private void setupBarChart() {
        barChartMoods.getDescription().setEnabled(false);
        barChartMoods.setDrawGridBackground(false);
        barChartMoods.getLegend().setEnabled(false);
        
        XAxis xAxis = barChartMoods.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        
        barChartMoods.getAxisLeft().setDrawGridLines(false);
        barChartMoods.getAxisLeft().setAxisMinimum(0f);
        barChartMoods.getAxisRight().setEnabled(false);
    }

    private void updateBarChart(Map<String, Integer> moodCounts) {
        if (moodCounts.isEmpty()) {
            barChartMoods.clear();
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        int index = 0;
        for (Map.Entry<String, Integer> entry : moodCounts.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue()));
            labels.add(entry.getKey());
            colors.add(getMoodColor(entry.getKey()));
            index++;
        }

        barChartMoods.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));

        BarDataSet dataSet = new BarDataSet(entries, "Moods");
        dataSet.setColors(colors);

        BarData data = new BarData(dataSet);
        data.setValueTextSize(10f);
        data.setValueTextColor(Color.GRAY);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        barChartMoods.setData(data);
        barChartMoods.setFitBars(true);
        barChartMoods.invalidate();
        barChartMoods.animateY(1000);
    }

    private int getMoodColor(String mood) {
        switch (mood) {
            case Note.MOOD_HAPPY:     return Color.parseColor("#FFD54F"); // Amber 300
            case Note.MOOD_SAD:       return Color.parseColor("#64B5F6"); // Blue 300
            case Note.MOOD_MOTIVATED: return Color.parseColor("#81C784"); // Green 300
            case Note.MOOD_ANXIOUS:   return Color.parseColor("#FF8A65"); // Deep Orange 300
            case Note.MOOD_CALM:
            default:                  return Color.parseColor("#90CAF9"); // Blue 200
        }
    }

    private void setListeners() {
        fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), AddNoteActivity.class));
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        if (btnWriteJournal != null) {
            btnWriteJournal.setOnClickListener(v -> {
                if (todayJournalId != null) {
                    Intent intent = new Intent(requireContext(), EditNoteActivity.class);
                    // We don't have the full note here, so a real app might fetch it first
                    // But our EditNoteActivity takes extras. To do it perfectly we should fetch the note.
                    // For simplicity, let's just let the user edit it. EditNoteActivity needs all extras.
                    // Actually, if we don't have extras, EditNoteActivity might crash or show empty.
                    // Better to just open AddNoteActivity if todayJournalId is null. If not null, 
                    // we'd need to query the note. Let's just do a quick query.
                    firebaseHelper.getNotesQuery().whereEqualTo("noteId", todayJournalId).get()
                        .addOnSuccessListener(snap -> {
                            if (!snap.isEmpty() && isAdded()) {
                                Note n = snap.getDocuments().get(0).toObject(Note.class);
                                if (n != null) {
                                    Intent editIntent = new Intent(requireContext(), EditNoteActivity.class);
                                    editIntent.putExtra(EditNoteActivity.EXTRA_NOTE_ID, n.getNoteId());
                                    editIntent.putExtra(EditNoteActivity.EXTRA_NOTE_TITLE, n.getTitle());
                                    editIntent.putExtra(EditNoteActivity.EXTRA_NOTE_CONTENT, n.getContent());
                                    editIntent.putExtra(EditNoteActivity.EXTRA_NOTE_CATEGORY, n.getCategory());
                                    editIntent.putExtra(EditNoteActivity.EXTRA_NOTE_MOOD, n.getMood());
                                    editIntent.putExtra(EditNoteActivity.EXTRA_NOTE_COLOR, n.getColorTheme());
                                    editIntent.putExtra(EditNoteActivity.EXTRA_NOTE_FAVORITE, n.isFavorite());
                                    editIntent.putExtra(EditNoteActivity.EXTRA_NOTE_IMAGE_URL, n.getImageUrl());
                                    startActivity(editIntent);
                                    requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                                }
                            }
                        });
                } else {
                    Intent intent = new Intent(requireContext(), AddNoteActivity.class);
                    intent.putExtra(AddNoteActivity.EXTRA_IS_JOURNAL, true);
                    if (tvDailyPrompt != null && tvDailyPrompt.getTag() != null) {
                        intent.putExtra(AddNoteActivity.EXTRA_PROMPT_ID, tvDailyPrompt.getTag().toString());
                    }
                    startActivity(intent);
                    requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                }
            });
        }

        // "See all" navigates to Notes tab
        View btnSeeAll = requireView().findViewById(R.id.btn_see_all);
        if (btnSeeAll != null) {
            btnSeeAll.setOnClickListener(v -> {
                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).switchToNotesTab();
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh stats when returning to fragment
        loadStats();
    }
}
