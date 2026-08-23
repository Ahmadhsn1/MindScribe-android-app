package com.mindscribe.interfaces;

import com.mindscribe.models.Note;

/**
 * Interface for RecyclerView item interactions.
 * Demonstrates OOP: Abstraction + Interfaces
 */
public interface NoteClickListener {
    /** Called when note card is tapped */
    void onNoteClick(Note note);

    /** Called when favorite star is toggled */
    void onFavoriteClick(Note note, int position);

    /** Called when delete is triggered from long press menu */
    void onDeleteClick(Note note, int position);

    /** Called when edit is triggered from long press menu */
    void onEditClick(Note note);
}
