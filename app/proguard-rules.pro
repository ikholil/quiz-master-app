# App-specific R8 rules. Library modules ship their own consumer rules.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Navigation type-safe routes are @Serializable classes resolved by name.
-keep @kotlinx.serialization.Serializable class app.prostuti.** { *; }
-keepclassmembers class app.prostuti.** {
    *** Companion;
}
-keepclasseswithmembers class app.prostuti.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn org.slf4j.**
