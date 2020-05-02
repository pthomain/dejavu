# See https://github.com/krschultz/android-proguard-snippets
-dontobfuscate
-optimizationpasses 2
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-dontwarn java.lang.invoke.**
-dontwarn rx.internal.util.unsafe.**
-dontwarn com.squareup.okhttp.**
-dontwarn okhttp3.**
-dontnote okhttp3.**
-dontwarn autovalue.shaded.**

-keepclassmembers class **.R$* {public static <fields>;}
-keep class **.R$*

# Eclipse

-dontwarn org.eclipse.**

# autovalue
-dontwarn javax.lang.**
-dontwarn javax.tools.**
-dontwarn javax.annotation.**
-dontwarn autovalue.shaded.com.**
-dontwarn com.google.auto.value.**
-keep class com.google.auto.common.BasicAnnotationProcessor { *; }
-keep class com.google.auto.service.AutoService { *; }
-keep class java.nio.file.** { *; }

# autovalue gson extension
-keep class **.AutoParcelGson*
-keep class com.google.auto.**
-keepnames @auto.parcelgson.AutoParcelGson class *
-dontwarn com.google.common.**
-dontwarn com.google.javaformat.**
-dontwarn com.google.auto.**
-dontwarn com.google.googlejavaformat.**

# Platform calls Class.forName on types which do not exist on Android to determine platform.
-dontnote retrofit2.Platform
# Platform used when running on Java 8 VMs. Will not be used at runtime.
-dontwarn retrofit2.Platform$Java8
# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
# Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions

# Retrolambda
-dontwarn java.lang.invoke.*

# Play Services

-dontnote com.google.android.gms.**
-dontnote com.google.common.util.concurrent.**
-dontwarn android.support.v7.**
-keep class android.support.v7.** { *; }
-keep interface android.support.v7.** { *; }

# Okio

-keep class sun.misc.Unsafe { *; }
-dontwarn java.nio.file.*
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn okio.**

-keep class dev.pthomain.** { *; }
-keep interface dev.pthomain.** { *; }

## GSON 2.2.4 specific rules ##

# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# Gson specific classes
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }

# OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn com.squareup.javapoet.JavaFile

# Retrofit 2.X
## https://square.github.io/retrofit/ ##

-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Snappy
-dontwarn org.iq80.snappy.**
-dontwarn sun.misc.Unsafe
-dontwarn org.apache.hadoop.io.compress.*
-keep class org.apache.hadoop.io.compress.**. { *; }
-keep class org.iq80.snappy.** { *; }

# Tests
-dontwarn org.junit.**
-dontwarn org.robolectric.**

-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod,Synthetic,LocalVariableTable,LocalVariableTypeTable,RuntimeVisibleAnnotations
-keep class dev.pthomain.android.** { *; }