package com.mindscribe.data.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mindscribe.models.Note;
import com.mindscribe.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;

public class NoteRepository {
    private FirebaseHelper firebaseHelper;
    private MutableLiveData<List<Note>> allNotes;

    public NoteRepository(Application application) {
        firebaseHelper = FirebaseHelper.getInstance();
        allNotes = new MutableLiveData<>();
        
        setupSnapshotListener();
    }

    private void setupSnapshotListener() {
        if (!firebaseHelper.isLoggedIn()) return;
        
        // Firestore caches this snapshot query locally automatically.
        firebaseHelper.getNotesQuery().addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.w("NoteRepository", "Listen failed.", error);
                return;
            }
            List<Note> notes = new ArrayList<>();
            if (value != null) {
                for (QueryDocumentSnapshot doc : value) {
                    Note note = doc.toObject(Note.class);
                    if (note != null) {
                        note.setNoteId(doc.getId());
                        notes.add(note);
                    }
                }
                allNotes.postValue(notes);
            }
        });
    }

    public LiveData<List<Note>> getAllNotes() {
        return allNotes;
    }

    public void saveNote(Note note) {
        firebaseHelper.saveNote(note, null, new FirebaseHelper.OnCompleteListener() {
            @Override
            public void onSuccess() {}
            @Override
            public void onFailure(String errorMessage) {}
        });
    }

    public void deleteNote(Note note) {
        if (note.getNoteId() != null) {
            firebaseHelper.deleteNote(note.getNoteId(), new FirebaseHelper.OnCompleteListener() {
                @Override
                public void onSuccess() {}
                @Override
                public void onFailure(String errorMessage) {}
            });
        }
    }
}
