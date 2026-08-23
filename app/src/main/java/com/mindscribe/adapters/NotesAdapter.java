package com.mindscribe.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.mindscribe.R;
import com.mindscribe.interfaces.NoteClickListener;
import com.mindscribe.models.Note;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * RecyclerView Adapter for Notes list.
 * Demonstrates: ArrayList, Lambda Expressions, Polymorphism via NoteClickListener interface
 */
public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private final Context          context;
    private       ArrayList<Note>  notesList;   // ArrayList — OOP concept
    private final NoteClickListener listener;   // Interface — OOP concept
    private       int              lastAnimatedPosition = -1;

    // Constructor — OOP concept
    public NotesAdapter(Context context, ArrayList<Note> notesList, NoteClickListener listener) {
        this.context   = context;
        this.notesList = notesList;
        this.listener  = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_note_card, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notesList.get(position);

        // Bind data
        holder.tvTitle.setText(note.getTitle());
        holder.tvContent.setText(note.getContentPreview());
        holder.tvDate.setText(formatDate(note.getCreatedAt() != null
                ? note.getCreatedAt().toDate() : new Date()));

        // Category chip
        holder.chipCategory.setText(note.getCategory());
        applyCategoryStyle(holder.chipCategory, note.getCategory());

        holder.chipMood.setText(note.getMood());
        applyMoodStyle(holder.chipMood);
        holder.cardView.setCardBackgroundColor(context.getColor(getColorResForTheme(note.getColorTheme())));

        // Favorite state
        holder.ivFavorite.setImageResource(
                note.isFavorite() ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
        holder.ivFavorite.setColorFilter(context.getColor(
                note.isFavorite() ? R.color.favorite_active : R.color.favorite_inactive));

        // Click listeners — Lambda Expressions OOP concept
        holder.cardView.setOnClickListener(v -> listener.onNoteClick(note));

        holder.ivFavorite.setOnClickListener(v -> {
            listener.onFavoriteClick(note, holder.getAdapterPosition());
        });

        // Long press for popup menu
        holder.cardView.setOnLongClickListener(v -> {
            showPopupMenu(v, note, holder.getAdapterPosition());
            return true;
        });

        // Staggered entry animation
        animateItem(holder.itemView, position);
    }

    private void showPopupMenu(View anchor, Note note, int position) {
        PopupMenu popup = new PopupMenu(context, anchor);
        popup.inflate(R.menu.note_item_menu);

        // Lambda for menu item click
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_edit) {
                listener.onEditClick(note);
                return true;
            } else if (id == R.id.menu_delete) {
                listener.onDeleteClick(note, position);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void applyCategoryStyle(Chip chip, String category) {
        int textColor;
        int bgColor;

        switch (category) {
            case Note.CATEGORY_WORK:
                textColor = context.getColor(R.color.cat_work);
                bgColor   = context.getColor(R.color.cat_work_bg);
                break;
            case Note.CATEGORY_IDEAS:
                textColor = context.getColor(R.color.cat_ideas);
                bgColor   = context.getColor(R.color.cat_ideas_bg);
                break;
            case Note.CATEGORY_IMPORTANT:
                textColor = context.getColor(R.color.cat_important);
                bgColor   = context.getColor(R.color.cat_important_bg);
                break;
            default: // Personal
                textColor = context.getColor(R.color.cat_personal);
                bgColor   = context.getColor(R.color.cat_personal_bg);
                break;
        }
        chip.setTextColor(textColor);
        chip.setChipBackgroundColor(
                android.content.res.ColorStateList.valueOf(bgColor));
    }

    private void applyMoodStyle(Chip chip) {
        chip.setTextColor(context.getColor(R.color.text_secondary));
        chip.setChipBackgroundColor(ColorStateList.valueOf(context.getColor(R.color.bg_elevated)));
    }

    private int getColorResForTheme(String colorTheme) {
        if (Note.COLOR_SAGE.equals(colorTheme)) return R.color.note_sage;
        if (Note.COLOR_SKY.equals(colorTheme)) return R.color.note_sky;
        if (Note.COLOR_PEACH.equals(colorTheme)) return R.color.note_peach;
        if (Note.COLOR_ROSE.equals(colorTheme)) return R.color.note_rose;
        return R.color.note_default;
    }

    private void animateItem(View view, int position) {
        if (position > lastAnimatedPosition) {
            view.startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_up));
            lastAnimatedPosition = position;
        }
    }

    private String formatDate(Date date) {
        return DateFormat.format("MMM dd, yyyy", date).toString();
    }

    // ── Public Data Methods ───────────────────────────────────────────────────

    public void updateList(ArrayList<Note> newList) {
        this.notesList = newList;
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        notesList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, notesList.size());
    }

    public void updateItem(int position, Note updatedNote) {
        notesList.set(position, updatedNote);
        notifyItemChanged(position);
    }

    /** Filter notes by search query */
    public ArrayList<Note> filter(List<Note> original, String query) {
        ArrayList<Note> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase().trim();
        for (Note note : original) {
            if (note.getTitle().toLowerCase().contains(lowerQuery) ||
                note.getContent().toLowerCase().contains(lowerQuery) ||
                note.getCategory().toLowerCase().contains(lowerQuery) ||
                note.getMood().toLowerCase().contains(lowerQuery) ||
                note.getColorTheme().toLowerCase().contains(lowerQuery)) {
                filtered.add(note);
            }
        }
        return filtered;
    }

    @Override
    public int getItemCount() { return notesList != null ? notesList.size() : 0; }

    // ── ViewHolder (Inner Class — OOP) ────────────────────────────────────────
    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        CardView  cardView;
        TextView  tvTitle, tvContent, tvDate;
        Chip      chipCategory, chipMood;
        ImageView ivFavorite;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView     = itemView.findViewById(R.id.card_note);
            tvTitle      = itemView.findViewById(R.id.tv_note_title);
            tvContent    = itemView.findViewById(R.id.tv_note_content);
            tvDate       = itemView.findViewById(R.id.tv_note_date);
            chipCategory = itemView.findViewById(R.id.chip_category);
            chipMood     = itemView.findViewById(R.id.chip_mood);
            ivFavorite   = itemView.findViewById(R.id.iv_favorite);
        }
    }
}
