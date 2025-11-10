# 保持库中的公共类和方法
-keep class com.example.mylibrary.** { *; }

# 保持序列化类
-keepclassmembers class com.example.mylibrary.model.** {
    public <init>();
}

# 保持注解
-keep @interface com.example.mylibrary.annotations.**
