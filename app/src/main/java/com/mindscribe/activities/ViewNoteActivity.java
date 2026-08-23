package com.mindscribe.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mindscribe.R;
import com.mindscribe.models.Note;
import com.mindscribe.utils.FirebaseHelper;

import java.io.IOException;
import java.io.OutputStream;

/**
 * View Note — READ single note, trigger Edit, Delete, Share, or Export.
 * Expert implementation with PDF generation and system sharing.
 */
public class ViewNoteActivity extends AppCompatActivity {

    public static final String EXTRA_NOTE_ID       = "note_id";
    public static final String EXTRA_NOTE_TITLE    = "note_title";
    public static final String EXTRA_NOTE_CONTENT  = "note_content";
    public static final String EXTRA_NOTE_CATEGORY = "note_category";
    public static final String EXTRA_NOTE_MOOD     = "note_mood";
    public static final String EXTRA_NOTE_COLOR    = "note_color";
    public static final String EXTRA_NOTE_FAVORITE = "note_favorite";
    public static final String EXTRA_NOTE_DATE     = "note_date";
    public static final String EXTRA_NOTE_IMAGE_URL = "note_image_url";
    public static final String EXTRA_NOTE_REMINDER_TIME = "note_reminder_time";

    private static final int CREATE_FILE_REQUEST_CODE = 101;

    private TextView  tvTitle, tvContent, tvDate;
    private Chip      chipCategory, chipMood;
    private ImageView ivFavorite, ivAttachment;

    private String noteId;
    private String noteColor;
    private boolean isFavorite;
    private String imageUrl;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_note);

        firebaseHelper = FirebaseHelper.getInstance();
        initViews();
        setupToolbar();
        loadNoteFromIntent();

        findViewById(R.id.container_content).startAnimation(
                android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_up));
    }

    private void initViews() {
        tvTitle      = findViewById(R.id.tv_title);
        tvContent    = findViewById(R.id.tv_content);
        tvDate       = findViewById(R.id.tv_date);
        chipCategory = findViewById(R.id.chip_category);
        chipMood     = findViewById(R.id.chip_mood);
        ivFavorite   = findViewById(R.id.iv_favorite);
        ivAttachment = findViewById(R.id.iv_attachment);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadNoteFromIntent() {
        Intent intent = getIntent();
        noteId     = intent.getStringExtra(EXTRA_NOTE_ID);
        noteColor  = intent.getStringExtra(EXTRA_NOTE_COLOR);
        isFavorite = intent.getBooleanExtra(EXTRA_NOTE_FAVORITE, false);
        imageUrl   = intent.getStringExtra(EXTRA_NOTE_IMAGE_URL);

        tvTitle.setText(intent.getStringExtra(EXTRA_NOTE_TITLE));
        tvContent.setText(intent.getStringExtra(EXTRA_NOTE_CONTENT));
        tvDate.setText(intent.getStringExtra(EXTRA_NOTE_DATE));
        chipCategory.setText(intent.getStringExtra(EXTRA_NOTE_CATEGORY));
        
        String mood = intent.getStringExtra(EXTRA_NOTE_MOOD);
        chipMood.setText(mood != null && !mood.isEmpty() ? mood : Note.MOOD_CALM);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            ivAttachment.setVisibility(View.VISIBLE);
            Glide.with(this).load(imageUrl).into(ivAttachment);
        }

        updateFavoriteIcon();
        applyCategoryColor(intent.getStringExtra(EXTRA_NOTE_CATEGORY));
        applyMoodStyle();
        applyNoteTheme(noteColor);

        ivFavorite.setOnClickListener(v -> toggleFavorite());
    }

    private void toggleFavorite() {
        isFavorite = !isFavorite;
        updateFavoriteIcon();

        firebaseHelper.toggleFavorite(noteId, isFavorite, new FirebaseHelper.OnCompleteListener() {
            @Override
            public void onSuccess() {
                String msg = isFavorite ? "Added to favorites" : "Removed from favorites";
                Toast.makeText(ViewNoteActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFailure(String err) {
                isFavorite = !isFavorite;
                updateFavoriteIcon();
            }
        });
    }

    private void updateFavoriteIcon() {
        ivFavorite.setImageResource(
                isFavorite ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
        ivFavorite.setColorFilter(getColor(
                isFavorite ? R.color.favorite_active : R.color.favorite_inactive));
    }

    private void applyCategoryColor(String category) {
        if (category == null) return;
        int color;
        switch (category) {
            case Note.CATEGORY_WORK:      color = getColor(R.color.cat_work);      break;
            case Note.CATEGORY_IDEAS:     color = getColor(R.color.cat_ideas);     break;
            case Note.CATEGORY_IMPORTANT: color = getColor(R.color.cat_important); break;
            default:                      color = getColor(R.color.cat_personal);  break;
        }
        chipCategory.setTextColor(color);
    }

    private void applyMoodStyle() {
        chipMood.setTextColor(getColor(R.color.text_secondary));
        chipMood.setChipBackgroundColor(ColorStateList.valueOf(getColor(R.color.bg_elevated)));
    }

    private void applyNoteTheme(String colorTheme) {
        View container = findViewById(R.id.container_content);
        if (container != null) {
            container.setBackgroundColor(getColor(getColorResForTheme(colorTheme)));
        }
    }

    private int getColorResForTheme(String colorTheme) {
        if (Note.COLOR_SAGE.equals(colorTheme)) return R.color.note_sage;
        if (Note.COLOR_SKY.equals(colorTheme)) return R.color.note_sky;
        if (Note.COLOR_PEACH.equals(colorTheme)) return R.color.note_peach;
        if (Note.COLOR_ROSE.equals(colorTheme)) return R.color.note_rose;
        return R.color.note_default;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.view_note_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_edit) {
            openEditScreen();
            return true;
        } else if (id == R.id.menu_delete) {
            showDeleteDialog();
            return true;
        } else if (id == R.id.menu_share) {
            shareAsText();
            return true;
        } else if (id == R.id.menu_export_pdf) {
            exportAsPdf();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openEditScreen() {
        Intent intent = new Intent(this, EditNoteActivity.class);
        intent.putExtra(EditNoteActivity.EXTRA_NOTE_ID,       noteId);
        intent.putExtra(EditNoteActivity.EXTRA_NOTE_TITLE,    tvTitle.getText().toString());
        intent.putExtra(EditNoteActivity.EXTRA_NOTE_CONTENT,  tvContent.getText().toString());
        intent.putExtra(EditNoteActivity.EXTRA_NOTE_CATEGORY, chipCategory.getText().toString());
        intent.putExtra(EditNoteActivity.EXTRA_NOTE_MOOD,     chipMood.getText().toString());
        intent.putExtra(EditNoteActivity.EXTRA_NOTE_COLOR,    noteColor);
        intent.putExtra(EditNoteActivity.EXTRA_NOTE_FAVORITE, isFavorite);
        intent.putExtra(EditNoteActivity.EXTRA_NOTE_IMAGE_URL, imageUrl);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }

    private void showDeleteDialog() {
        new MaterialAlertDialogBuilder(this, R.style.MindScribe_Dialog)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Delete", (dialog, which) -> deleteNote())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteNote() {
        firebaseHelper.deleteNote(noteId, new FirebaseHelper.OnCompleteListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(ViewNoteActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                finish();
            }
            @Override
            public void onFailure(String err) {
                Toast.makeText(ViewNoteActivity.this, "Delete failed: " + err, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void shareAsText() {
        String shareContent = "📝 " + tvTitle.getText().toString() + "\n\n" +
                tvContent.getText().toString() + "\n\n" +
                "Shared via MindScribe Diary";
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, tvTitle.getText().toString());
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareContent);
        startActivity(Intent.createChooser(shareIntent, "Share note via"));
    }

    private void exportAsPdf() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, tvTitle.getText().toString().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf");
        startActivityForResult(intent, CREATE_FILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CREATE_FILE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                generatePdf(uri);
            }
        }
    }

    private void generatePdf(Uri uri) {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();

        // Header
        paint.setColor(Color.BLACK);
        paint.setTextSize(24);
        paint.setFakeBoldText(true);
        canvas.drawText(tvTitle.getText().toString(), 50, 50, paint);

        // Date & Category
        paint.setTextSize(12);
        paint.setFakeBoldText(false);
        paint.setColor(Color.GRAY);
        canvas.drawText(tvDate.getText().toString() + " | " + chipCategory.getText().toString(), 50, 80, paint);

        // Content
        paint.setColor(Color.BLACK);
        paint.setTextSize(14);
        String text = tvContent.getText().toString();
        
        // Simple line wrapping
        int x = 50;
        int y = 120;
        int maxWidth = 500;
        String[] lines = text.split("\n");
        for (String line : lines) {
            if (y > 800) break; // Basic page limit check
            canvas.drawText(line, x, y, paint);
            y += 20;
        }

        document.finishPage(page);

        try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
            document.writeTo(outputStream);
            Toast.makeText(this, "PDF saved successfully", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            document.close();
        }
    }
}
