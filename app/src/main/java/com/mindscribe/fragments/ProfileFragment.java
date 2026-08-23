package com.mindscribe.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mindscribe.R;
import com.mindscribe.activities.AppLockActivity;
import com.mindscribe.activities.LoginActivity;
import com.mindscribe.databinding.FragmentProfileBinding;
import com.mindscribe.utils.AppLockManager;
import com.mindscribe.utils.FirebaseHelper;

/**
 * Profile Fragment — ViewBinding + MVVM approach + Image Picker
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseHelper firebaseHelper;
    private AppLockManager appLockManager;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                uploadProfilePicture(uri);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseHelper = FirebaseHelper.getInstance();
        appLockManager = new AppLockManager(requireContext());
        
        loadUserData();
        updateAppLockUi();
        setListeners();

        view.startAnimation(android.view.animation.AnimationUtils
                .loadAnimation(requireContext(), R.anim.fade_in));
    }

    private void loadUserData() {
        firebaseHelper.getUserDocument()
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    if (doc.exists()) {
                        String name  = doc.getString("name");
                        String email = doc.getString("email");
                        String profileImageUrl = doc.getString("profileImageUrl");
                        
                        binding.tvName.setText(name != null ? name : "User");
                        binding.tvEmail.setText(email != null ? email : "");
                        
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            binding.tvInitial.setVisibility(View.GONE);
                            Glide.with(this)
                                 .load(profileImageUrl)
                                 .apply(RequestOptions.circleCropTransform())
                                 .into(binding.ivProfilePicture);
                        } else {
                            binding.tvInitial.setVisibility(View.VISIBLE);
                            binding.tvInitial.setText(name != null && !name.isEmpty()
                                    ? String.valueOf(name.charAt(0)).toUpperCase() : "U");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded() && firebaseHelper.getCurrentUser() != null) {
                        binding.tvEmail.setText(firebaseHelper.getCurrentUser().getEmail());
                    }
                });

        firebaseHelper.getNotesQuery()
                .get()
                .addOnSuccessListener(snap -> {
                    if (isAdded()) {
                        binding.tvTotalNotes.setText(snap.size() + " Notes");
                    }
                });
    }

    private void setListeners() {
        binding.btnLogout.setOnClickListener(v -> showLogoutDialog());
        binding.btnAppLock.setOnClickListener(v -> {
            if (appLockManager.isLockEnabled()) {
                showDisableLockDialog();
            } else {
                Intent intent = new Intent(requireContext(), AppLockActivity.class);
                intent.putExtra(AppLockActivity.EXTRA_MODE, AppLockActivity.MODE_SETUP);
                startActivity(intent);
            }
        });
        
        binding.ivProfilePicture.setOnClickListener(v -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });
    }
    
    private void uploadProfilePicture(Uri uri) {
        Toast.makeText(requireContext(), "Uploading picture...", Toast.LENGTH_SHORT).show();
        firebaseHelper.uploadProfileImage(uri, new FirebaseHelper.OnCompleteListener() {
            @Override
            public void onSuccess() {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Profile picture updated", Toast.LENGTH_SHORT).show();
                    loadUserData();
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Failed to upload: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateAppLockUi() {
        boolean enabled = appLockManager.isLockEnabled();
        binding.tvAppLockStatus.setText(enabled
                ? "App lock is on. MindScribe will ask for your PIN or fingerprint on startup."
                : "App lock is off. Enable it to protect your private diary.");
        binding.btnAppLock.setText(enabled ? "Disable App Lock" : "Enable App Lock");
        binding.btnAppLock.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                requireContext().getColor(enabled ? R.color.error : R.color.primary)));
    }

    private void showDisableLockDialog() {
        new MaterialAlertDialogBuilder(requireContext(), R.style.MindScribe_Dialog)
                .setTitle("Disable App Lock?")
                .setMessage("MindScribe will open without PIN or fingerprint protection.")
                .setPositiveButton("Disable", (dialog, which) -> {
                    appLockManager.disableLock();
                    updateAppLockUi();
                    Toast.makeText(requireContext(), "App lock disabled", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showLogoutDialog() {
        new MaterialAlertDialogBuilder(requireContext(), R.style.MindScribe_Dialog)
                .setTitle(getString(R.string.logout_title))
                .setMessage(getString(R.string.logout_message))
                .setPositiveButton(getString(R.string.logout), (dialog, which) -> logout())
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void logout() {
        firebaseHelper.getAuth().signOut();
        Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserData();
        updateAppLockUi();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
