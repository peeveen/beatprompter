# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\steven.frew\AppData\Local\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-dontwarn com.dropbox.**
-dontwarn com.microsoft.**
-dontwarn com.nimbusds.**
-dontwarn com.rarepebble.**

-keepattributes *Annotation*,Signature

# The values of this enum are only used in annotations, which ProGuard seems to think makes
# them fair game for removal. We don't want that.
-keepclassmembers class com.stevenfrew.beatprompter.cache.parse.tag.find.Type { *; }
# And the constructors for the tag classes are only executed via reflection, so ProGuard
# thinks they are never called. We have to tell it otherwise.
-keep class com.stevenfrew.beatprompter.cache.parse.tag.** {*;}
# Google Drive stuff that ProGuard doesn't realise we actually need.
-keep class * extends com.google.api.client.json.GenericJson {*;}
-keep class com.google.api.services.drive.** {*;}
-keep class com.google.api.client.googleapis.** {*;}
# Kotlin ... still to figure out why this is needed.
-keep public class kotlin.** { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }
# Apparently this is a slight performance improvement ... stops NotNull checking.
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

# No heavy logging in production code please.
-assumenosideeffects class android.util.Log {
  public static *** v(...);
  public static *** d(...);
  public static *** i(...);
  public static *** w(...);
  public static *** e(...);
}

-assumenoexternalsideeffects class java.lang.StringBuilder {
    public java.lang.StringBuilder();
    public java.lang.StringBuilder(int);
    public java.lang.StringBuilder(java.lang.String);
    public java.lang.StringBuilder append(java.lang.Object);
    public java.lang.StringBuilder append(java.lang.String);
    public java.lang.StringBuilder append(java.lang.StringBuffer);
    public java.lang.StringBuilder append(char[]);
    public java.lang.StringBuilder append(char[], int, int);
    public java.lang.StringBuilder append(boolean);
    public java.lang.StringBuilder append(char);
    public java.lang.StringBuilder append(int);
    public java.lang.StringBuilder append(long);
    public java.lang.StringBuilder append(float);
    public java.lang.StringBuilder append(double);
    public java.lang.String toString();
}

-assumenoexternalreturnvalues public final class java.lang.StringBuilder {
    public java.lang.StringBuilder append(java.lang.Object);
    public java.lang.StringBuilder append(java.lang.String);
    public java.lang.StringBuilder append(java.lang.StringBuffer);
    public java.lang.StringBuilder append(char[]);
    public java.lang.StringBuilder append(char[], int, int);
    public java.lang.StringBuilder append(boolean);
    public java.lang.StringBuilder append(char);
    public java.lang.StringBuilder append(int);
    public java.lang.StringBuilder append(long);
    public java.lang.StringBuilder append(float);
    public java.lang.StringBuilder append(double);
}