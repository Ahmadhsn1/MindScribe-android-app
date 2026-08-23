# Firebase Proguard Rules
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.internal.** { *; }
-keepnames class com.google.firebase.firestore.** { *; }

# Glide Rules
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public class * extends com.bumptech.glide.module.LibraryGlideModule
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# MPAndroidChart Rules
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# MindScribe Model Classes (Vital for Firestore deserialization)
-keep class com.mindscribe.models.** { *; }
-keepclassmembers class com.mindscribe.models.** {
    private <fields>;
    public <methods>;
}
