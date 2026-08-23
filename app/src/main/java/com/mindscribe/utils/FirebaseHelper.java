package com.mindscribe.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.Timestamp;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.mindscribe.models.Note;
import com.mindscribe.models.User;
import com.mindscribe.workers.ImageUploadWorker;

import java.util.HashMap;
import java.util.Map;

/**
 * Advanced Firebase Repository.
 * Handles Firestore CRUD and Storage uploads centrally.
 * Follows Clean Architecture by keeping business logic out of Activities.
 */
public class FirebaseHelper {

    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_NOTES = "notes";
    private static final String FOLDER_IMAGES     = "note_images";
    private static final String FOLDER_PROFILES   = "profile_images";

    public static final String FIELD_TITLE      = "title";
    public static final String FIELD_CONTENT    = "content";
    public static final String FIELD_CATEGORY   = "category";
    public static final String FIELD_MOOD       = "mood";
    public static final String FIELD_COLOR_THEME = "colorTheme";
    public static final String FIELD_IMAGE_URL  = "imageUrl";
    public static final String FIELD_REMINDER_TIME = "reminderTime";
    public static final String FIELD_FAVORITE   = "isFavorite";
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_UPDATED_AT = "updatedAt";

    // Luna Journal fields
    public static final String FIELD_JOURNAL_DATE = "journalDate";
    public static final String FIELD_PROMPT_ID = "promptId";
    public static final String FIELD_IS_TIME_CAPSULE = "isTimeCapsule";
    public static final String FIELD_RELEASE_DATE = "releaseDate";
    public static final String FIELD_IS_ONE_LINE = "isOneLine";
    public static final String FIELD_IS_LOCKED = "isLocked";

    private static FirebaseHelper instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth      auth;
    private final FirebaseStorage   storage;

    private Context context;
    public FirebaseHelper(Context context) {
        this.context = context;
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public static FirebaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseHelper(context.getApplicationContext());
        } else if (instance.context == null && context != null) {
            instance.context = context.getApplicationContext();
        }
        return instance;
    }

    public static FirebaseHelper getInstance() {
        if (instance == null) instance = new FirebaseHelper(null);
        return instance;
    }

    public FirebaseAuth getAuth() { return auth; }
public FirebaseUser getCurrentUser() { return auth.getCurrentUser(); }

    public boolean isLoggedIn() { return auth.getCurrentUser() != null; }
    public String getCurrentUserId() { return auth.getUid(); }

    public DocumentReference getUserDocument() {
        return db.collection(COLLECTION_USERS).document(getCurrentUserId());
    }

    public CollectionReference getNotesCollection() {
        return getUserDocument().collection(COLLECTION_NOTES);
    }

    public void saveUser(User user, OnCompleteListener listener) {
        db.collection(COLLECTION_USERS).document(user.getUserId()).set(user)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void uploadProfileImage(Uri uri, OnCompleteListener listener) {
        if (getCurrentUserId() == null) {
            listener.onFailure("Not logged in");
            return;
        }
        String fileName = getCurrentUserId() + ".jpg";
        StorageReference ref = storage.getReference().child(FOLDER_PROFILES).child(fileName);
        
        ref.putFile(uri)
                .addOnSuccessListener(task -> ref.getDownloadUrl()
                        .addOnSuccessListener(url -> {
                            getUserDocument().update("profileImageUrl", url.toString())
                                    .addOnSuccessListener(aVoid -> listener.onSuccess())
                                    .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
                        }))
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    // ── Note Operations (Repository Style) ────────────────────────────────────

    /**
     * Professional Save Note: Handles optional image upload before saving to Firestore.
     */
    public void saveNote(Note note, Uri localImageUri, OnCompleteListener listener) {
        if (localImageUri != null) {
            if (isNetworkAvailable()) {
                uploadImage(localImageUri, new OnImageUploadListener() {
                    @Override
                    public void onComplete(String downloadUrl) {
                        note.setImageUrl(downloadUrl);
                        performFirestoreSave(note, listener, null);
                    }
                    @Override
                    public void onError(String error) {
                        listener.onFailure("Image upload failed: " + error);
                    }
                });
            } else {
                performFirestoreSave(note, listener, localImageUri);
            }
        } else {
            performFirestoreSave(note, listener, null);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void scheduleImageUpload(String noteId, Uri imageUri) {
        Data data = new Data.Builder()
                .putString("noteId", noteId)
                .putString("imageUri", imageUri.toString())
                .build();

        OneTimeWorkRequest uploadWork = new OneTimeWorkRequest.Builder(ImageUploadWorker.class)
                .setInputData(data)
                .build();
        if (context != null) {
            WorkManager.getInstance(context).enqueue(uploadWork);
        }
    }

    private void performFirestoreSave(Note note, OnCompleteListener listener, Uri localImageUriToSchedule) {
        Map<String, Object> data = noteToMap(note);
        
        if (note.getNoteId() == null) {
            // CREATE
            getNotesCollection().add(data)
                    .addOnSuccessListener(ref -> {
                        if (localImageUriToSchedule != null) scheduleImageUpload(ref.getId(), localImageUriToSchedule);
                        listener.onSuccess();
                    })
                    .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
        } else {
            // UPDATE
            getNotesCollection().document(note.getNoteId()).set(data)
                    .addOnSuccessListener(aVoid -> {
                        if (localImageUriToSchedule != null) scheduleImageUpload(note.getNoteId(), localImageUriToSchedule);
                        listener.onSuccess();
                    })
                    .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
        }
    }

    private void uploadImage(Uri uri, OnImageUploadListener listener) {
        String fileName = System.currentTimeMillis() + ".jpg";
        StorageReference ref = storage.getReference().child(FOLDER_IMAGES).child(fileName);
        
        ref.putFile(uri)
                .addOnSuccessListener(task -> ref.getDownloadUrl()
                        .addOnSuccessListener(url -> listener.onComplete(url.toString())))
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public Query getNotesQuery() {
        return getNotesCollection().orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING);
    }

    public Query getFavoriteNotes() {
        return getNotesCollection().whereEqualTo(FIELD_FAVORITE, true)
                .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING);
    }

    public void toggleFavorite(String noteId, boolean isFavorite, OnCompleteListener listener) {
        getNotesCollection().document(noteId)
                .update(FIELD_FAVORITE, isFavorite, FIELD_UPDATED_AT, Timestamp.now())
                .addOnSuccessListener(v -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void deleteNote(String noteId, OnCompleteListener listener) {
        getNotesCollection().document(noteId).delete()
                .addOnSuccessListener(v -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    private Map<String, Object> noteToMap(Note note) {
        Map<String, Object> map = new HashMap<>();
        map.put(FIELD_TITLE,         note.getTitle());
        map.put(FIELD_CONTENT,       note.getContent());
        map.put(FIELD_CATEGORY,      note.getCategory());
        map.put(FIELD_MOOD,          note.getMood());
        map.put(FIELD_COLOR_THEME,   note.getColorTheme());
        map.put(FIELD_IMAGE_URL,     note.getImageUrl());
        map.put(FIELD_REMINDER_TIME, note.getReminderTime());
        map.put(FIELD_FAVORITE,      note.isFavorite());
        map.put(FIELD_CREATED_AT,    note.getCreatedAt() != null ? note.getCreatedAt() : Timestamp.now());
        map.put(FIELD_UPDATED_AT,    Timestamp.now());

        // Luna fields
        if (note.getJournalDate() != null) map.put(FIELD_JOURNAL_DATE, note.getJournalDate());
        if (note.getPromptId() != null) map.put(FIELD_PROMPT_ID, note.getPromptId());
        map.put(FIELD_IS_TIME_CAPSULE, note.isTimeCapsule());
        if (note.getReleaseDate() != null) map.put(FIELD_RELEASE_DATE, note.getReleaseDate());
        map.put(FIELD_IS_ONE_LINE, note.isOneLine());
        map.put(FIELD_IS_LOCKED, note.isLocked());

        return map;
    }

    public interface OnCompleteListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    private interface OnImageUploadListener {
        void onComplete(String downloadUrl);
        void onError(String error);
    }
}
