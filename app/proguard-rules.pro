# ProGuard / R8 rules for Sepi Marketplace
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
-keep class com.sepi.market.data.remote.dto.** { *; }
-keepclassmembers class com.sepi.market.data.remote.dto.** { *; }
-dontwarn timber.log.**
-dontwarn okhttp3.**
-dontwarn org.codehaus.mojo.animal_sniffer.*
