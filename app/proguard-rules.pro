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

# Native model runtimes resolve Java classes and members by name.
-keep class ai.onnxruntime.** { *; }
-keep class com.google.ai.edge.litertlm.** { *; }
-keep class !com.google.mediapipe.framework.Graph,!com.google.mediapipe.framework.GraphProfiler,com.google.mediapipe.** { *; }
-keep class com.google.mediapipe.framework.Graph {
    <fields>;
    native <methods>;
}
-keep class com.google.mediapipe.framework.GraphProfiler {
    <fields>;
    native <methods>;
}

# Orbit does not load MediaPipe graph templates through the optional Java proto overload.
-dontwarn com.google.mediapipe.proto.GraphTemplateProto$CalculatorGraphTemplate

# MediaPipe's generated classes use annotations newer than the packaged protobuf runtime.
-dontwarn com.google.protobuf.Internal$ProtoMethodMayReturnNull
-dontwarn com.google.protobuf.Internal$ProtoNonnullApi
-dontwarn com.google.protobuf.ProtoField
-dontwarn com.google.protobuf.ProtoPresenceBits
-dontwarn com.google.protobuf.ProtoPresenceCheckedField

# PDFBox checks for the optional JPEG 2000 decoder before it attempts to call it.
-dontwarn com.gemalto.jp2.JP2Decoder

# The Gemini SDK reads Jackson annotations and generated model builders at runtime.
-keep class com.google.genai.types.** { *; }

# LiteRT discovers bubble tool methods and their parameters through Kotlin reflection.
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault,Signature
-keep class com.example.orbitai.feature.bubble.OrbitBubbleToolSchema {
    @com.google.ai.edge.litertlm.Tool <methods>;
}

# Gson uses field names in custom model preferences and exported backup manifests.
-keep class com.example.orbitai.core.model.CustomModelSpec { <fields>; }
-keep class com.example.orbitai.feature.backup.BackupPreview { <fields>; }
-keepclassmembers enum com.example.orbitai.core.model.ModelFormat { public static <fields>; }
-keepclassmembers enum com.example.orbitai.core.model.PromptStyle { public static <fields>; }
