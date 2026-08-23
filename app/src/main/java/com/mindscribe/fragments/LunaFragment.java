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
import com.mindscribe.R;
import com.mindscribe.activities.AddNoteActivity;

public class LunaFragment extends Fragment {

    public LunaFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_luna, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FloatingActionButton fab = view.findViewById(R.id.fab_new_journal);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddNoteActivity.class);
            // Optionally pass extra that this is a Luna journal entry
            startActivity(intent);
        });

        MaterialButton btnAnswerPrompt = view.findViewById(R.id.btn_answer_prompt);
        btnAnswerPrompt.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddNoteActivity.class);
            // Pre-fill with today's prompt
            intent.putExtra("prompt", "What is one thing that made you smile today, and why?");
            startActivity(intent);
        });
    }
}
