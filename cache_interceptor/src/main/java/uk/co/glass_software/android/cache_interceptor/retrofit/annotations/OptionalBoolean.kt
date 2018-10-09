package uk.co.glass_software.android.cache_interceptor.retrofit.annotations

enum class OptionalBoolean(val value: Boolean?) {
    TRUE(true),
    FALSE(false),
    DEFAULT(null);
}