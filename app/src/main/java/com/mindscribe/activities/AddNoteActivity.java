package com.mindscribe.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.mindscribe.R;
import com.mindscribe.models.Note;
import com.mindscribe.receivers.NoteReminderReceiver;
import com.mindscribe.utils.FirebaseHelper;
import com.mindscribe.utils.ValidationUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import com.google.firebase.Timestamp;

/**
 * Add Note screen — CREATE operation in CRUD.
 * Professional implementation using Repository pattern and Material Design 3.
 */
public class AddNoteActivity extends AppCompatActivity {

    public static final String EXTRA_IS_JOURNAL = "is_journal";
    public static final String EXTRA_PROMPT_ID  = "prompt_id";

    private TextInputLayout   tilTitle, tilContent;
    private TextInputEditText etTitle, etContent;
    private AutoCompleteTextView spinnerCategory;
    private ImageView         ivFavoriteStar, ivAttachment, ivDeleteAttachment;
    private ChipGroup         chipGroupMood, chipGroupColor;
    private MaterialButton    btnSave, btnAttachImage, btnSetReminder;
    private LinearProgressIndicator progressBar;
    private SwitchMaterial    switchOneLine, switchTimeCapsule;
    private TextView          tvReleaseDate;
    private long              releaseDateMillis = 0;

    private boolean isFavorite = false;
    private FirebaseHelper firebaseHelper;
    private ActivityResultLauncher<Intent> speechLauncher;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private Uri localImageUri;
    private long reminderTime = 0;
    
    private boolean isJournal = false;
    private String promptId = null;

    private final String[] CATEGORIES = {
            Note.CATEGORY_PERSONAL, Note.CATEGORY_WORK,
            Note.CATEGORY_IDEAS, Note.CATEGORY_IMPORTANT
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_note);

        firebaseHelper = FirebaseHelper.getInstance();
        isJournal = getIntent().getBooleanExtra(EXTRA_IS_JOURNAL, false);
        promptId = getIntent().getStringExtra(EXTRA_PROMPT_ID);

        initLaunchers();
        initViews();
        setupToolbar();
        setupCategoryDropdown();
        setListeners();

        findViewById(R.id.container_content).startAnimation(
                android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_up));
    }

    private void initLaunchers() {
        speechLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ArrayList<String> res = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (res != null && !res.isEmpty()) {
                            String spokenText = res.get(0);
                            String current = etContent.getText().toString();
                            etContent.setText(current.isEmpty() ? spokenText : current + " " + spokenText);
                        }
                    }
                }
        );

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        localImageUri = uri;
                        ivAttachment.setImageURI(uri);
                        ivAttachment.setVisibility(View.VISIBLE);
                        ivDeleteAttachment.setVisibility(View.VISIBLE);
                    }
                }
        );
    }

    private void initViews() {
        tilTitle       = findViewById(R.id.til_title);
        tilContent     = findViewById(R.id.til_content);
        etTitle        = findViewById(R.id.et_title);
        etContent      = findViewById(R.id.et_content);
        spinnerCategory= findViewById(R.id.spinner_category);
        ivFavoriteStar = findViewById(R.id.iv_favorite_star);
        ivAttachment   = findViewById(R.id.iv_attachment);
        ivDeleteAttachment = findViewById(R.id.iv_delete_attachment);
        ivDeleteAttachment.setOnClickListener(v -> {
            localImageUri = null;
            ivAttachment.setImageURI(null);
            ivAttachment.setVisibility(View.GONE);
            ivDeleteAttachment.setVisibility(View.GONE);
        });
        chipGroupMood  = findViewById(R.id.chip_group_mood);
        chipGroupColor = findViewById(R.id.chip_group_color);
        btnSave        = findViewById(R.id.btn_save);
        btnAttachImage = findViewById(R.id.btn_attach_image);
        btnSetReminder = findViewById(R.id.btn_set_reminder);
        progressBar    = findViewById(R.id.progress_bar);
        switchOneLine  = findViewById(R.id.switch_one_line);
        switchTimeCapsule = findViewById(R.id.switch_time_capsule);
        tvReleaseDate  = findViewById(R.id.tv_release_date);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupCategoryDropdown() {
        if (isJournal) {
            View tilCategory = findViewById(R.id.til_category);
            if (tilCategory != null) tilCategory.setVisibility(View.GONE);
            
            View llJournalOptions = findViewById(R.id.ll_journal_options);
            if (llJournalOptions != null) llJournalOptions.setVisibility(View.VISIBLE);

            if (switchTimeCapsule != null && tvReleaseDate != null) {
                switchTimeCapsule.setOnCheckedChangeListener((btn, isChecked) -> {
                    tvReleaseDate.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                    if (!isChecked) releaseDateMillis = 0;
                });
                tvReleaseDate.setOnClickListener(v -> pickReleaseDate());
            }

            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMM dd", Locale.getDefault());
            etTitle.setText(sdf.format(new Date()));
            // Don't disable etTitle completely so they can add to it, but it starts with date.
        } else {
            spinnerCategory.setAdapter(new ArrayAdapter<>(this, R.layout.item_dropdown, CATEGORIES));
            spinnerCategory.setText(CATEGORIES[0], false);
        }
    }

    private void setListeners() {
        ivFavoriteStar.setOnClickListener(v -> {
            isFavorite = !isFavorite;
            ivFavoriteStar.setImageResource(isFavorite ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
            ivFavoriteStar.setColorFilter(getColor(isFavorite ? R.color.favorite_active : R.color.favorite_inactive));
        });

        btnSave.setOnClickListener(v -> saveNote());
        btnAttachImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnSetReminder.setOnClickListener(v -> pickReminder());
        tilContent.setEndIconOnClickListener(v -> startVoiceRecognition());
    }

    private void saveNote() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (ValidationUtils.isEmpty(title)) { tilTitle.setError("Title required"); return; }
        if (ValidationUtils.isEmpty(content)) { tilContent.setError("Content required"); return; }

        setLoading(true);

        Note note = new Note(title, content, 
                             isJournal ? "Journal" : spinnerCategory.getText().toString(), 
                             getCheckedChipText(chipGroupMood, Note.MOOD_CALM),
                             getCheckedChipText(chipGroupColor, Note.COLOR_DEFAULT), isFavorite);
        note.setReminderTime(reminderTime);
        if (isJournal) {
            note.setJournalDate(Timestamp.now());
            if (promptId != null) note.setPromptId(promptId);
            
            if (switchOneLine != null) note.setOneLine(switchOneLine.isChecked());
            if (switchTimeCapsule != null) {
                boolean isTimeCap = switchTimeCapsule.isChecked();
                note.setTimeCapsule(isTimeCap);
                if (isTimeCap && releaseDateMillis > 0) {
                    note.setReleaseDate(new Timestamp(new Date(releaseDateMillis)));
                }
            }
        }

        firebaseHelper.saveNote(note, localImageUri, new FirebaseHelper.OnCompleteListener() {
            @Override
            public void onSuccess() {
                if (reminderTime > 0) scheduleAlarm(title, content, reminderTime);
                finish();
            }

            @Override
            public void onFailure(String err) {
                setLoading(false);
                Toast.makeText(AddNoteActivity.this, err, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pickReminder() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker().build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            MaterialTimePicker timePicker = new MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_12H).build();
            timePicker.addOnPositiveButtonClickListener(v -> {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(selection);
                cal.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                cal.set(Calendar.MINUTE, timePicker.getMinute());
                reminderTime = cal.getTimeInMillis();
                btnSetReminder.setText("Reminder Set");
            });
            timePicker.show(getSupportFragmentManager(), "TIME");
        });
        datePicker.show(getSupportFragmentManager(), "DATE");
    }

    private void pickReleaseDate() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Release Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            releaseDateMillis = selection;
            tvReleaseDate.setText(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(releaseDateMillis)));
        });
        datePicker.show(getSupportFragmentManager(), "RELEASE_DATE");
    }

    private void scheduleAlarm(String title, String text, long time) {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, NoteReminderReceiver.class);
        intent.putExtra(NoteReminderReceiver.EXTRA_TITLE, title);
        intent.putExtra(NoteReminderReceiver.EXTRA_TEXT, text);

        PendingIntent pi = PendingIntent.getBroadcast(this, (int)System.currentTimeMillis(), 
                           intent, PendingIntent.FLAG_IMMUTABLE);
        if (am != null) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi);
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        try { speechLauncher.launch(intent); } catch (Exception e) { Toast.makeText(this, "Voice not supported", Toast.LENGTH_SHORT).show(); }
    }

    private void setLoading(boolean l) {
        progressBar.setVisibility(l ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!l);
    }

    private String getCheckedChipText(ChipGroup g, String d) {
        int id = g.getCheckedChipId();
        return id != View.NO_ID ? ((Chip)g.findViewById(id)).getText().toString() : d;
    }
}
