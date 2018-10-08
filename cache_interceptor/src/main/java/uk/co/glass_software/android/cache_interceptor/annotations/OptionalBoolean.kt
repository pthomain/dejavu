package uk.co.glass_software.android.cache_interceptor.annotations

enum class OptionalBoolean(val value: Boolean?) {
    TRUE(true),
    FALSE(false),
    DEFAULT(null);
}