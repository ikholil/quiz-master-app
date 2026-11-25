# kotlinx.serialization DTOs
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class app.prostuti.core.network.dto.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class app.prostuti.core.network.dto.**$$serializer { *; }
# Retrofit keeps generic signatures of suspend API methods
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keepattributes Signature, Exceptions
