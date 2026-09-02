# Scizor keep rules, applied to consumers that minify a build containing the
# real artifact. Debug builds do not run R8, so these are inert in the common
# debug-only setup.

# play-services-location and play-services-code-scanner are compileOnly, so a
# consumer who has not opted into them has no such classes on the classpath.
# FusedLocationMocker and QrScanner still reference those types directly from
# reachable code — each is gated at runtime by a Class.forName probe, which is
# what makes the missing classes harmless — and since AGP 7.1 R8 reports a
# missing class as a build error rather than a warning. Without these the
# consumer's minified build fails, blaming Scizor's packages.
-dontwarn com.google.android.gms.**
-dontwarn com.google.mlkit.**
-dontwarn com.apollographql.apollo.**
-dontwarn io.ktor.**
-dontwarn kotlinx.io.**

# Manifest-declared components — this service and ScizorActivity — are already kept by
# the rules AAPT generates from the merged manifest, and their framework-override members
# are never renamed. This explicit rule is belt-and-braces for the one component whose
# failure mode is silent: a listener service R8 removed just never binds.
-keep class com.scizor.feature.notifications.ScizorNotificationListenerService { *; }

# Nothing else in Scizor depends on class or member names surviving. The
# DataStore keys are plain strings, not reflected field names, and the only
# name-based lookups are the two Class.forName probes above, whose whole point
# is to tolerate the class being absent. This file is short on purpose.
