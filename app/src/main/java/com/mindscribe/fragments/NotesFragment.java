package com.mindscribe.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Toast;
import android.widget.CalendarView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.mindscribe.R;
import com.mindscribe.activities.AddNoteActivity;
import com.mindscribe.activities.EditNoteActivity;
import com.mindscribe.activities.ViewNoteActivity;
import com.mindscribe.adapters.NotesAdapter;
import com.mindscribe.interfaces.NoteClickListener;
import com.mindscribe.models.Note;
import com.mindscribe.utils.AppLockManager;
import com.mindscribe.utils.FirebaseHelper;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.widget.EditText;
import android.text.InputType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Notes Fragment — main list screen.
 * Real-time Firestore listener + Search + Category filter + Date Range filter.
 * Professional implementation of Advanced Filtering.
 */
public class NotesFragment extends Fragment implements NoteClickListener {

    private RecyclerView       rvNotes;
    private NotesAdapter       adapter;
    private ArrayList<Note>    allNotesList = new ArrayList<>();
    private FloatingActionButton fabAdd;
    private SearchView         searchView;
    private ChipGroup          chipGroup;
    private LinearLayout       emptyState;
    private MaterialButton     btnDateFilter;
    private CalendarView       calendarView;

    private FirebaseHelper       firebaseHelper;
    private ListenerRegistration firestoreListener;

    // Filter states
    private String currentCategory = "All";
    private String currentSearchQuery = "";
    private long   startDate = 0;
    private long   endDate = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseHelper = FirebaseHelper.getInstance();
        initViews(view);
        setupRecyclerView();
        setupSearch();
        setupCategoryChips();
        setupDateFilter();
        attachFirestoreListener();
        setListeners();
    }

    private void initViews(View view) {
        rvNotes       = view.findViewById(R.id.rv_notes);
        fabAdd        = view.findViewById(R.id.fab_add);
        searchView    = view.findViewById(R.id.search_view);
        chipGroup     = view.findViewById(R.id.chip_group_categories);
        emptyState    = view.findViewById(R.id.empty_state);
        btnDateFilter = view.findViewById(R.id.btn_date_filter);
        calendarView  = view.findViewById(R.id.calendar_view);
    }

    private void setupRecyclerView() {
        adapter = new NotesAdapter(requireContext(), new ArrayList<>(), this);
        rvNotes.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvNotes.setAdapter(adapter);
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearchQuery = query;
                applyFilters();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText;
                applyFilters();
                return true;
            }
        });
    }

    private void setupCategoryChips() {
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            View checkedChip = group.findViewById(checkedIds.get(0));
            if (checkedChip instanceof Chip) {
                currentCategory = ((Chip) checkedChip).getText().toString();
                applyFilters();
            }
        });
    }

    private void setupDateFilter() {
        btnDateFilter.setOnClickListener(v -> {
            if (calendarView != null) {
                if (calendarView.getVisibility() == View.VISIBLE) {
                    calendarView.setVisibility(View.GONE);
                    startDate = 0;
                    endDate = 0;
                    btnDateFilter.setIconTint(android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.primary)));
                    applyFilters();
                } else {
                    calendarView.setVisibility(View.VISIBLE);
                    btnDateFilter.setIconTint(android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.favorite_active)));
                }
            }
        });

        if (calendarView != null) {
            calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
                Calendar cal = Calendar.getInstance();
                cal.set(year, month, dayOfMonth, 0, 0, 0);
                startDate = cal.getTimeInMillis();
                cal.set(year, month, dayOfMonth, 23, 59, 59);
                endDate = cal.getTimeInMillis();
                applyFilters();
            });
        }
    }

    private void attachFirestoreListener() {
        Query query = firebaseHelper.getNotesQuery();

        firestoreListener = query.addSnapshotListener((snapshots, error) -> {
            if (error != null || !isAdded()) return;

            allNotesList.clear();

            if (snapshots != null) {
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    try {
                        Note note = doc.toObject(Note.class);
                        if (note != null) {
                            note.setNoteId(doc.getId());
                            allNotesList.add(note);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            applyFilters();
        });
    }

    /**
     * Unified filtering logic — applies all active filters together.
     * Demonstrates: Functional-style filtering, ArrayList manipulation.
     */
    private void applyFilters() {
        ArrayList<Note> filteredList = new ArrayList<>(allNotesList);

        // 1. Filter by Category
        if (!currentCategory.equals("All")) {
            ArrayList<Note> temp = new ArrayList<>();
            for (Note note : filteredList) {
                if (currentCategory.equals("Favorites")) {
                    if (note.isFavorite()) temp.add(note);
                } else if (note.getCategory().equals(currentCategory)) {
                    temp.add(note);
                }
            }
            filteredList = temp;
        }

        // 2. Filter by Date Range
        if (startDate > 0 && endDate > 0) {
            ArrayList<Note> temp = new ArrayList<>();
            for (Note note : filteredList) {
                if (note.getCreatedAt() != null) {
                    long noteTime = note.getCreatedAt().toDate().getTime();
                    if (noteTime >= startDate && noteTime <= endDate) {
                        temp.add(note);
                    }
                }
            }
            filteredList = temp;
        }

        // 3. Filter by Search Query
        if (!currentSearchQuery.isEmpty()) {
            filteredList = adapter.filter(filteredList, currentSearchQuery);
        }

        adapter.updateList(filteredList);
        toggleEmptyState(filteredList.isEmpty());
    }

    private void toggleEmptyState(boolean isEmpty) {
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvNotes.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void setListeners() {
        fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), AddNoteActivity.class));
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    @Override
    public void onNoteClick(Note note) {
        if ("Journal".equals(note.getCategory()) && new AppLockManager(requireContext()).isLockEnabled()) {
            promptPinForAction(() -> openViewNoteActivity(note));
        } else {
            openViewNoteActivity(note);
        }
    }

    private void openViewNoteActivity(Note note) {
        Intent intent = new Intent(requireContext(), ViewNoteActivity.class);
        intent.putExtra(ViewNoteActivity.EXTRA_NOTE_ID,       note.getNoteId());
        intent.putExtra(ViewNoteActivity.EXTRA_NOTE_TITLE,    note.getTitle());
        intent.putExtra(ViewNoteActivity.EXTRA_NOTE_CONTENT,  note.getContent());
        intent.putExtra(ViewNoteActivity.EXTRA_NOTE_CATEGORY, note.getCategory());
        intent.putExtra(ViewNoteActivity.EXTRA_NOTE_MOOD,     note.getMood());
        intent.putExtra(ViewNoteActivity.EXTRA_NOTE_COLOR,    note.getColorTheme());
        intent.putExtra(ViewNoteActivity.EXTRA_NOTE_FAVORITE, note.isFavorite());
        intent.putExtra(ViewNoteActivity.EXTRA_NOTE_IMAGE_URL, note.getImageUrl());
        intent.putExtra(ViewNoteActivity.EXTRA_NOTE_REMINDER_TIME, note.getReminderTime());
        intent.putExtra(ViewNoteActivity.EXTRA_NOTE_DATE,
                note.getCreatedAt() != null
                        ? new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                              .format(note.getCreatedAt().toDate())
                        : "");
        startActivity(intent);
        requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void promptPinForAction(Runnable action) {
        EditText etPin = new EditText(requireContext());
        etPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        new MaterialAlertDialogBuilder(requireContext(), R.style.MindScribe_Dialog)
                .setTitle("Private Journal")
                .setMessage("Enter your PIN to access this entry.")
                .setView(etPin)
                .setPositiveButton("Unlock", (dialog, which) -> {
                    if (new AppLockManager(requireContext()).verifyPin(etPin.getText().toString())) {
                        action.run();
                    } else {
                        Toast.makeText(requireContext(), "Incorrect PIN", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onFavoriteClick(Note note, int position) {
        boolean newState = !note.isFavorite();
        firebaseHelper.toggleFavorite(note.getNoteId(), newState,
                new FirebaseHelper.OnCompleteListener() {
                    @Override public void onSuccess() {}
                    @Override public void onFailure(String err) {
                        Toast.makeText(requireContext(), "Failed to update favorite", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDeleteClick(Note note, int position) {
        new MaterialAlertDialogBuilder(requireContext(), R.style.MindScribe_Dialog)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    firebaseHelper.deleteNote(note.getNoteId(),
                            new FirebaseHelper.OnCompleteListener() {
                                @Override public void onSuccess() {
                                    Toast.makeText(requireContext(), "Note deleted", Toast.LENGTH_SHORT).show();
                                }
                                @Override public void onFailure(String err) {
                                    Toast.makeText(requireContext(), "Delete failed: " + err, Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onEditClick(Note note) {
        if ("Journal".equals(note.getCategory()) && new AppLockManager(requireContext()).isLockEnabled()) {
            promptPinForAction(() -> openEditNoteActivity(note));
        } else {
            openEditNoteActivity(note);
        }
    }

    private void openEditNoteActivity(Note note) {
        Intent intent = new Intent(requireContext(), EditNoteActivity.class);
        intent.putExtra(EditNoteActivity.EXTRA_NOTE_ID,       note.getNoteId());
        intent.putExtra(EditNoteActivity.EXTRA_NOTE_TITLE,    note.getTitle());
        intent.putExtra(EditNoteActivity.EXTRA_NOTE_CONTENT,  note.getContent());
        intent.putExtra(EditNoteActivity.EXTRA_NOTE_CATEGORY, note.getCategory());
        intent.putExtra(EditNoteActivity.EXTRA_NOTE_MOOD,     note.getMood());
        intent.putExtra(EditNoteActivity.EXTRA_NOTE_COLOR,    note.getColorTheme());
        intent.putExtra(EditNoteActivity.EXTRA_NOTE_FAVORITE, note.isFavorite());
        intent.putExtra(EditNoteActivity.EXTRA_NOTE_IMAGE_URL,note.getImageUrl());
        intent.putExtra(EditNoteActivity.EXTRA_NOTE_REMINDER_TIME, note.getReminderTime());
        startActivity(intent);
        requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (firestoreListener != null) firestoreListener.remove();
    }
}
