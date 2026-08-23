package com.mindscribe.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.mindscribe.R;
import com.mindscribe.models.Note;
import com.mindscribe.receivers.NoteReminderReceiver;
import com.mindscribe.utils.FirebaseHelper;
import com.mindscribe.utils.ValidationUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

/**
 * Edit Note screen — UPDATE operation in CRUD.
 * Professional implementation using Repository pattern.
 */
public class EditNoteActivity extends AppCompatActivity {

    public static final String EXTRA_NOTE_ID       = "edit_note_id";
    public static final String EXTRA_NOTE_TITLE    = "edit_note_title";
    public static final String EXTRA_NOTE_CONTENT  = "edit_note_content";
    public static final String EXTRA_NOTE_CATEGORY = "edit_note_category";
    public static final String EXTRA_NOTE_MOOD     = "edit_note_mood";
    public static final String EXTRA_NOTE_COLOR    = "edit_note_color";
    public static final String EXTRA_NOTE_FAVORITE = "edit_note_favorite";
    public static final String EXTRA_NOTE_IMAGE_URL= "edit_note_image_url";
    public static final String EXTRA_NOTE_REMINDER_TIME= "edit_note_reminder_time";

    private TextInputLayout   tilTitle, tilContent;
    private TextInputEditText etTitle, etContent;
    private AutoCompleteTextView spinnerCategory;
    private ImageView         ivFavoriteStar, ivAttachment;
    private ChipGroup         chipGroupMood, chipGroupColor;
    private MaterialButton    btnUpdate, btnAttachImage, btnSetReminder;
    private LinearProgressIndicator progressBar;

    private String  noteId;
    private boolean isFavorite;
    private FirebaseHelper firebaseHelper;
    private ActivityResultLauncher<Intent> speechLauncher;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private Uri newImageUri;
    private String existingImageUrl;
    private long reminderTime = 0;

    private final String[] CATEGORIES = {
            Note.CATEGORY_PERSONAL, Note.CATEGORY_WORK,
            Note.CATEGORY_IDEAS, Note.CATEGORY_IMPORTANT
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        firebaseHelper = FirebaseHelper.getInstance();
        initLaunchers();
        initViews();
        setupToolbar();
        setupCategoryDropdown();
        populateExistingData();
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
                        newImageUri = uri;
                        ivAttachment.setImageURI(uri);
                        ivAttachment.setVisibility(View.VISIBLE);
                        btnAttachImage.setText("Change Image");
                    }
                }
        );
    }

    private void initViews() {
        tilTitle        = findViewById(R.id.til_title);
        tilContent      = findViewById(R.id.til_content);
        etTitle         = findViewById(R.id.et_title);
        etContent       = findViewById(R.id.et_content);
        spinnerCategory = findViewById(R.id.spinner_category);
        ivFavoriteStar  = findViewById(R.id.iv_favorite_star);
        ivAttachment    = findViewById(R.id.iv_attachment);
        chipGroupMood   = findViewById(R.id.chip_group_mood);
        chipGroupColor  = findViewById(R.id.chip_group_color);
        btnUpdate       = findViewById(R.id.btn_update);
        btnAttachImage  = findViewById(R.id.btn_attach_image);
        btnSetReminder  = findViewById(R.id.btn_set_reminder);
        progressBar     = findViewById(R.id.progress_bar);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupCategoryDropdown() {
        spinnerCategory.setAdapter(new ArrayAdapter<>(this, R.layout.item_dropdown, CATEGORIES));
    }

    private void populateExistingData() {
        Intent intent = getIntent();
        noteId     = intent.getStringExtra(EXTRA_NOTE_ID);
        isFavorite = intent.getBooleanExtra(EXTRA_NOTE_FAVORITE, false);
        reminderTime = intent.getLongExtra(EXTRA_NOTE_REMINDER_TIME, 0);
        existingImageUrl = intent.getStringExtra(EXTRA_NOTE_IMAGE_URL);

        etTitle.setText(intent.getStringExtra(EXTRA_NOTE_TITLE));
        etContent.setText(intent.getStringExtra(EXTRA_NOTE_CONTENT));
        
        String category = intent.getStringExtra(EXTRA_NOTE_CATEGORY);
        if ("Journal".equals(category)) {
            View tilCategory = findViewById(R.id.til_category);
            if (tilCategory != null) tilCategory.setVisibility(View.GONE);
        } else {
            spinnerCategory.setText(category, false);
        }
        
        checkChipByText(chipGroupMood, intent.getStringExtra(EXTRA_NOTE_MOOD), Note.MOOD_CALM);
        checkChipByText(chipGroupColor, intent.getStringExtra(EXTRA_NOTE_COLOR), Note.COLOR_DEFAULT);

        if (existingImageUrl != null && !existingImageUrl.isEmpty()) {
            ivAttachment.setVisibility(View.VISIBLE);
            btnAttachImage.setText("Change Image");
            Glide.with(this).load(existingImageUrl).into(ivAttachment);
        }

        if (reminderTime > 0) {
            btnSetReminder.setText("Reminder Set");
            btnSetReminder.setIconResource(android.R.drawable.ic_menu_today);
        }
        
        updateFavoriteIcon();
        applyColorPreview(intent.getStringExtra(EXTRA_NOTE_COLOR));
    }

    private void setListeners() {
        ivFavoriteStar.setOnClickListener(v -> {
            isFavorite = !isFavorite;
            updateFavoriteIcon();
        });

        btnUpdate.setOnClickListener(v -> updateNote());
        btnAttachImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnSetReminder.setOnClickListener(v -> pickReminder());
        tilContent.setEndIconOnClickListener(v -> startVoiceRecognition());

        chipGroupColor.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != View.NO_ID) {
                Chip chip = group.findViewById(checkedId);
                if (chip != null) applyColorPreview(chip.getText().toString());
            }
        });
    }

    private void updateNote() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (!validateInputs(title, content)) return;

        setLoading(true);

        Note note = new Note(title, content, 
                             "Journal".equals(spinnerCategory.getText().toString()) ? "Journal" : spinnerCategory.getText().toString(),
                             getCheckedChipText(chipGroupMood, Note.MOOD_CALM),
                             getCheckedChipText(chipGroupColor, Note.COLOR_DEFAULT), isFavorite);
        note.setNoteId(noteId);
        note.setImageUrl(existingImageUrl);
        note.setReminderTime(reminderTime);

        firebaseHelper.saveNote(note, newImageUri, new FirebaseHelper.OnCompleteListener() {
            @Override
            public void onSuccess() {
                if (reminderTime > 0) scheduleReminder(title, content, reminderTime);
                setLoading(false);
                Toast.makeText(EditNoteActivity.this, "Note updated!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(String err) {
                setLoading(false);
                Toast.makeText(EditNoteActivity.this, "Update failed: " + err, Toast.LENGTH_SHORT).show();
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
                btnSetReminder.setIconResource(android.R.drawable.ic_menu_today);
            });
            timePicker.show(getSupportFragmentManager(), "TIME");
        });
        datePicker.show(getSupportFragmentManager(), "DATE");
    }

    private void scheduleReminder(String title, String text, long time) {
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

    private void updateFavoriteIcon() {
        ivFavoriteStar.setImageResource(isFavorite ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
        ivFavoriteStar.setColorFilter(getColor(isFavorite ? R.color.favorite_active : R.color.favorite_inactive));
    }

    private boolean validateInputs(String title, String content) {
        if (ValidationUtils.isEmpty(title)) { tilTitle.setError("Title required"); return false; }
        if (ValidationUtils.isEmpty(content)) { tilContent.setError("Content required"); return false; }
        return true;
    }

    private void setLoading(boolean l) {
        progressBar.setVisibility(l ? View.VISIBLE : View.GONE);
        btnUpdate.setEnabled(!l);
    }

    private String getCheckedChipText(ChipGroup g, String d) {
        int id = g.getCheckedChipId();
        return id != View.NO_ID ? ((Chip)g.findViewById(id)).getText().toString() : d;
    }

    private void applyColorPreview(String colorName) {
        if (colorName == null) return;
        int colorRes;
        switch (colorName.toLowerCase(Locale.ROOT)) {
            case "sage":  colorRes = R.color.note_sage; break;
            case "sky":   colorRes = R.color.note_sky; break;
            case "peach": colorRes = R.color.note_peach; break;
            case "rose":  colorRes = R.color.note_rose; break;
            default:      colorRes = R.color.note_default;
        }
        View container = findViewById(R.id.container_content);
        if (container != null) container.setBackgroundColor(getColor(colorRes));
    }

    private void checkChipByText(ChipGroup group, String value, String fallback) {
        String target = value != null && !value.isEmpty() ? value : fallback;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof Chip && ((Chip) child).getText().toString().equals(target)) {
                group.check(child.getId());
                return;
            }
        }
    }
}
