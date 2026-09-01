# Scizor keep rules, applied to consumers that minify a build containing the
# real artifact. Debug builds do not run R8, so these are inert in the common
# debug-only setup.

# Referenced by name from the library manifest, so R8 cannot see the reference.
-keep class com.scizor.feature.notifications.ScizorNotificationListenerService { *; }

# Host apps implement this to contribute a custom database source. Keeping the
# interface keeps the implementing classes' overridden members from being
# renamed out of alignment with it.
-keep interface com.scizor.feature.databasebrowser.ScizorDatabaseAdapter { *; }
-keep class * implements com.scizor.feature.databasebrowser.ScizorDatabaseAdapter { *; }

# Nothing else in Scizor depends on class or member names surviving. The
# DataStore keys are plain strings, not reflected field names, and no feature
# resolves a class by name at runtime. This file is short on purpose.
