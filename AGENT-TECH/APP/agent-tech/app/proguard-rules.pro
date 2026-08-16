# Agent Tech ProGuard rules (placeholder — Phase 6 will fill these in for release R8).
# Debug builds don't use ProGuard.
-dontwarn org.jetbrains.annotations.**
-keep class kotlinx.serialization.** { *; }
