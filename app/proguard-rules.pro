# Add project specific ProGuard rules here.

# Keep UCrop activity
-keep class com.yalantis.ucrop.** { *; }
-dontwarn com.yalantis.ucrop.**

# Keep Room generated classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep data models (used in Room + MediaStore)
-keep class dev.prism.gallery.data.model.** { *; }
-keep class dev.prism.gallery.data.local.entity.** { *; }
