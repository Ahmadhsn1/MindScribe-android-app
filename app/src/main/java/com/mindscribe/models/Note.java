package com.mindscribe.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

/**
 * Note model class demonstrating OOP: Encapsulation, Constructors, Getters/Setters
 */
public class Note {

    // Encapsulated private fields
    private String noteId = "";
    private String title;
    private String content;
    private String category;
    private String mood;
    private String colorTheme;
    private String imageUrl;
    private long reminderTime;
    private boolean isFavorite;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Luna Journal fields
    private Timestamp journalDate;
    private String promptId;
    private boolean isTimeCapsule;
    private Timestamp releaseDate;
    private boolean isOneLine;
    private boolean isLocked;

    // Constants for categories
    public static final String CATEGORY_PERSONAL  = "Personal";
    public static final String CATEGORY_WORK      = "Work";
    public static final String CATEGORY_IDEAS     = "Ideas";
    public static final String CATEGORY_IMPORTANT = "Important";

    // Mood tags
    public static final String MOOD_HAPPY     = "Happy";
    public static final String MOOD_SAD       = "Sad";
    public static final String MOOD_MOTIVATED = "Motivated";
    public static final String MOOD_ANXIOUS   = "Anxious";
    public static final String MOOD_CALM      = "Calm";

    // Visual note themes
    public static final String COLOR_DEFAULT = "Default";
    public static final String COLOR_SAGE    = "Sage";
    public static final String COLOR_SKY     = "Sky";
    public static final String COLOR_PEACH   = "Peach";
    public static final String COLOR_ROSE    = "Rose";

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Required empty constructor for Firestore deserialization */
    public Note() {}

    /** Full constructor for creating new notes */
    public Note(String title, String content, String category, boolean isFavorite) {
        this.title      = title;
        this.content    = content;
        this.category   = category;
        this.mood       = MOOD_CALM;
        this.colorTheme = COLOR_DEFAULT;
        this.isFavorite = isFavorite;
        this.createdAt  = Timestamp.now();
        this.updatedAt  = Timestamp.now();
    }

    public Note(String title, String content, String category, String mood,
                String colorTheme, boolean isFavorite) {
        this.title      = title;
        this.content    = content;
        this.category   = category;
        this.mood       = mood;
        this.colorTheme = colorTheme;
        this.isFavorite = isFavorite;
        this.createdAt  = Timestamp.now();
        this.updatedAt  = Timestamp.now();
    }

    /** Full constructor including id (used when fetching from Firestore) */
    public Note(String noteId, String title, String content,
                String category, boolean isFavorite,
                Timestamp createdAt, Timestamp updatedAt) {
        this.noteId     = noteId;
        this.title      = title;
        this.content    = content;
        this.category   = category;
        this.mood       = MOOD_CALM;
        this.colorTheme = COLOR_DEFAULT;
        this.isFavorite = isFavorite;
        this.createdAt  = createdAt;
        this.updatedAt  = updatedAt;
    }

    // ── Getters & Setters (Encapsulation) ─────────────────────────────────────

    public String getNoteId()              { return noteId; }
    public void   setNoteId(String id)     { this.noteId = id; }

    public String getTitle()               { return title; }
    public void   setTitle(String title)   { this.title = title; }

    public String getContent()             { return content; }
    public void   setContent(String c)     { this.content = c; }

    public String getCategory()            { return category; }
    public void   setCategory(String cat)  { this.category = cat; }

    public String getMood() {
        return mood != null && !mood.isEmpty() ? mood : MOOD_CALM;
    }
    public void setMood(String mood) {
        this.mood = mood;
    }

    public String getColorTheme() {
        return colorTheme != null && !colorTheme.isEmpty() ? colorTheme : COLOR_DEFAULT;
    }
    public void setColorTheme(String colorTheme) {
        this.colorTheme = colorTheme;
    }

    public String getImageUrl()            { return imageUrl; }
    public void   setImageUrl(String url)  { this.imageUrl = url; }

    public long getReminderTime()          { return reminderTime; }
    public void setReminderTime(long time) { this.reminderTime = time; }

    @PropertyName("isFavorite")
    public boolean isFavorite()            { return isFavorite; }
    @PropertyName("isFavorite")
    public void    setFavorite(boolean fav){ this.isFavorite = fav; }

    public Timestamp getCreatedAt()        { return createdAt; }
    public void setCreatedAt(Timestamp t)  { this.createdAt = t; }

    public Timestamp getUpdatedAt()        { return updatedAt; }
    public void setUpdatedAt(Timestamp t)  { this.updatedAt = t; }

    // Luna Journal getters/setters
    public Timestamp getJournalDate()              { return journalDate; }
    public void setJournalDate(Timestamp d)        { this.journalDate = d; }

    public String getPromptId()                    { return promptId; }
    public void setPromptId(String p)              { this.promptId = p; }

    @PropertyName("isTimeCapsule")
    public boolean isTimeCapsule()                 { return isTimeCapsule; }
    @PropertyName("isTimeCapsule")
    public void setTimeCapsule(boolean t)          { this.isTimeCapsule = t; }

    public Timestamp getReleaseDate()              { return releaseDate; }
    public void setReleaseDate(Timestamp d)        { this.releaseDate = d; }

    @PropertyName("isOneLine")
    public boolean isOneLine()                     { return isOneLine; }
    @PropertyName("isOneLine")
    public void setOneLine(boolean o)              { this.isOneLine = o; }

    @PropertyName("isLocked")
    public boolean isLocked()                      { return isLocked; }
    @PropertyName("isLocked")
    public void setLocked(boolean l)               { this.isLocked = l; }

    // ── Helper Methods ────────────────────────────────────────────────────────

    /** Returns a short preview of note content (max 120 chars) */
    public String getContentPreview() {
        if (content == null || content.isEmpty()) return "";
        return content.length() > 120 ? content.substring(0, 120) + "…" : content;
    }

    /** Update timestamps when note is edited */
    public void markUpdated() {
        this.updatedAt = Timestamp.now();
    }

    @Override
    public String toString() {
        return "Note{id='" + noteId + "', title='" + title + "', category='" + category + "'}";
    }
}
