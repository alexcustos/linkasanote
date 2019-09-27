# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/custos/Programs/android-sdk/tools/proguard/proguard-android.txt
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

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-optimizationpasses 5
-verbose
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
#-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

#-assumenosideeffects class com.bytesforge.linkasanote.utils.CommonUtils  {
#    public static void logStackTrace(...);
#}
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}
#public static int e(...);

-keeppackagenames org.jsoup.nodes

-keep class org.apache.commons.httpclient.** { *; }
-dontwarn org.apache.commons.**

-keep class org.apache.jackrabbit.webdav.** { *; }
-dontwarn org.apache.jackrabbit.**

-dontwarn javax.servlet.http.**
-dontwarn org.slf4j.*

-dontwarn sun.misc.Unsafe
-dontwarn java.lang.ClassValue
-dontwarn javax.lang.model.element.Modifier

-dontwarn android.util.Pair

-dontwarn afu.org.checkerframework.**
-dontwarn org.checkerframework.**

-keep class android.support.v7.** { *; }
-keep class com.owncloud.android.lib.** { *; }