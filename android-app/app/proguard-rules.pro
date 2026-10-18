# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

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

# Keep Room entities
-keep class br.com.dojolb.graduacao.data.entity.** { *; }

# Keep Room DAOs
-keep class br.com.dojolb.graduacao.data.dao.** { *; }

# Keep Repository
-keep class br.com.dojolb.graduacao.data.repository.** { *; }

# Keep ViewModels
-keep class br.com.dojolb.graduacao.ui.viewmodel.** { *; }

# Keep BackupManager
-keep class br.com.dojolb.graduacao.data.backup.** { *; }

# Keep serialization
-keep class kotlinx.serialization.** { *; }

# Keep iText7
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**