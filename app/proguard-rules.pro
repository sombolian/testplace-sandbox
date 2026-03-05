# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.nutrition.tracker.**$$serializer { *; }
-keepclassmembers class com.nutrition.tracker.** { *** Companion; }
-keepclasseswithmembers class com.nutrition.tracker.** { kotlinx.serialization.KSerializer serializer(...); }
