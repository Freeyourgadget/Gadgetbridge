# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/andi/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# Pebble BG-JS
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers class nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.webview.JSInterface {
    public *;
}
-keepattributes JavascriptInterface

# https://github.com/tony19/logback-android/issues/29
-dontwarn javax.mail.**, javax.naming.Context, javax.naming.InitialContext

# To avoid any stacktrace ambiguity
-keepattributes SourceFile,LineNumberTable

# GreenDAO 3 - http://greenrobot.org/greendao/documentation/technical-faq/
-keepclassmembers class * extends de.greenrobot.dao.AbstractDao {
    public static final java.lang.String TABLENAME;
}

-keep class **$Properties
