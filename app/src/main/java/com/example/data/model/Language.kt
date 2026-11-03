package com.example.data.model

import java.util.Locale

data class SupportedLanguage(
    val code: String,
    val name: String,
    val nativeName: String,
    val locale: Locale,
    val flagEmoji: String,
    val ttsAvailable: Boolean = true
)

object LanguageCatalog {
    val HINDI = SupportedLanguage(
        code = "hi",
        name = "Hindi",
        nativeName = "हिन्दी",
        locale = Locale("hi", "IN"),
        flagEmoji = "🇮🇳"
    )
    val ENGLISH = SupportedLanguage(
        code = "en",
        name = "English",
        nativeName = "English",
        locale = Locale("en", "IN"),
        flagEmoji = "🌐"
    )
    val PUNJABI = SupportedLanguage(
        code = "pa",
        name = "Punjabi",
        nativeName = "ਪੰਜਾਬੀ",
        locale = Locale("pa", "IN"),
        flagEmoji = "🇮🇳"
    )
    val BENGALI = SupportedLanguage(
        code = "bn",
        name = "Bengali",
        nativeName = "বাংলা",
        locale = Locale("bn", "IN"),
        flagEmoji = "🇮🇳"
    )
    val MARATHI = SupportedLanguage(
        code = "mr",
        name = "Marathi",
        nativeName = "मराठी",
        locale = Locale("mr", "IN"),
        flagEmoji = "🇮🇳"
    )
    val GUJARATI = SupportedLanguage(
        code = "gu",
        name = "Gujarati",
        nativeName = "ગુજરાતી",
        locale = Locale("gu", "IN"),
        flagEmoji = "🇮🇳"
    )
    val TAMIL = SupportedLanguage(
        code = "ta",
        name = "Tamil",
        nativeName = "தமிழ்",
        locale = Locale("ta", "IN"),
        flagEmoji = "🇮🇳"
    )
    val TELUGU = SupportedLanguage(
        code = "te",
        name = "Telugu",
        nativeName = "తెలుగు",
        locale = Locale("te", "IN"),
        flagEmoji = "🇮🇳"
    )
    val URDU = SupportedLanguage(
        code = "ur",
        name = "Urdu",
        nativeName = "اردو",
        locale = Locale("ur", "IN"),
        flagEmoji = "🇮🇳"
    )
    val KANNADA = SupportedLanguage(
        code = "kn",
        name = "Kannada",
        nativeName = "ಕನ್ನಡ",
        locale = Locale("kn", "IN"),
        flagEmoji = "🇮🇳"
    )
    val MALAYALAM = SupportedLanguage(
        code = "ml",
        name = "Malayalam",
        nativeName = "മലയാളം",
        locale = Locale("ml", "IN"),
        flagEmoji = "🇮🇳"
    )

    val ALL: List<SupportedLanguage> = listOf(
        HINDI,
        ENGLISH,
        PUNJABI,
        BENGALI,
        MARATHI,
        GUJARATI,
        TAMIL,
        TELUGU,
        URDU,
        KANNADA,
        MALAYALAM
    )

    fun fromCode(code: String): SupportedLanguage {
        return ALL.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: HINDI
    }
}
