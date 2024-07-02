# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/andi/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

-dontobfuscate

# Pebble BG-JS
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers class nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.webview.JSInterface {
    public *;
}
# Required for reflection in BangleJSDeviceSupport
-keepclassmembers class nodomain.freeyourgadget.gadgetbridge.model.CallSpec {
    public static *;
}
# Required for reflection in method GattCharacteristic.initDebugMap()
-keepclassmembers class nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic {
    public static *;
}
# Keep constructors for support classes, as they're called by reflection in DeviceSupportFactory#createServiceDeviceSupport
-keep public class * extends nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport
-keepclassmembers class * extends nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport {
    public <init>(nodomain.freeyourgadget.gadgetbridge.model.DeviceType);
    public <init>();
}
-keepattributes JavascriptInterface

# Keep coordinators, they're only referenced from DeviceType
-keep public class * implements nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator

# Keep parseIncoming for GFDIMessage classes, as it is called by reflection in GFDIMessage#parseIncoming
-keep public class * extends nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.GFDIMessage
-keepclassmembers class * extends nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.GFDIMessage {
    public static *** parseIncoming(...);
}

# https://github.com/tony19/logback-android/issues/29
-dontwarn javax.mail.**

# To avoid any stacktrace ambiguity
-keepattributes SourceFile,LineNumberTable

# GreenDAO 2 - http://greenrobot.org/greendao/documentation/technical-faq/
-keepclassmembers class * extends de.greenrobot.dao.AbstractDao {
    public static java.lang.String TABLENAME;
}

-keep class **$Properties

-keep class **$Properties { *; }

# Keep database migration classes accessed trough reflection
-keep class **.gadgetbridge.database.schema.* { *; }

# Keep Nordic DFU library
-keep class no.nordicsemi.android.dfu.** { *; }

# Keep dependency android-emojify (io.wax911.emojify) uses
-keep class org.hamcrest.** { *; }
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor

# Keep logback classes
-keep class ch.qos.** { *; }
-keep class org.slf4j.** { *; }

# Keep data classes
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
# Somehow the rule above was not enough for some
-keep class nodomain.freeyourgadget.gadgetbridge.devices.pinetime.InfiniTimeDFU* { *; }

# Keep generated protobuf classes
-keep class nodomain.freeyourgadget.gadgetbridge.proto.** { *; }
# https://github.com/protocolbuffers/protobuf/blob/main/java/lite.md#r8-rule-to-make-production-app-builds-work
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
