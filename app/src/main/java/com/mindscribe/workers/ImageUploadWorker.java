package com.mindscribe.workers;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.HashMap;
import java.util.Map;

public class ImageUploadWorker extends Worker {

    public ImageUploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String noteId = getInputData().getString("noteId");
        String imageUriString = getInputData().getString("imageUri");
        if (noteId == null || imageUriString == null) return Result.failure();

        Uri imageUri = Uri.parse(imageUriString);
        FirebaseStorage storage = FirebaseStorage.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        if (auth.getCurrentUser() == null) return Result.failure();
        
        String userId = auth.getCurrentUser().getUid();
        String fileName = System.currentTimeMillis() + ".jpg";
        StorageReference ref = storage.getReference().child("note_images").child(fileName);
        
        try {
            // Upload synchronously (WorkManager runs in background thread)
            com.google.android.gms.tasks.Task<com.google.firebase.storage.UploadTask.TaskSnapshot> uploadTask = ref.putFile(imageUri);
            com.google.android.gms.tasks.Tasks.await(uploadTask);
            
            // Get download URL synchronously
            com.google.android.gms.tasks.Task<Uri> urlTask = ref.getDownloadUrl();
            Uri downloadUri = com.google.android.gms.tasks.Tasks.await(urlTask);
            
            // Update Firestore document
            com.google.android.gms.tasks.Task<Void> updateTask = db.collection("users").document(userId)
                    .collection("notes").document(noteId)
                    .update("imageUrl", downloadUri.toString());
            com.google.android.gms.tasks.Tasks.await(updateTask);
            
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.retry();
        }
    }
}
