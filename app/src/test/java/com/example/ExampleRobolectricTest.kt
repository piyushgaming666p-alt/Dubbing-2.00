package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.SubtitleCue
import com.example.domain.ai.MultilingualTranslationService
import com.example.domain.video.SrtParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string resource matches Bhasha Studio`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Bhasha Studio", appName)
    }

    @Test
    fun `test SRT export and parse roundtrip`() {
        val cues = listOf(
            SubtitleCue(
                startMs = 1000L,
                endMs = 3500L,
                text = "नमस्ते दोस्तों",
                languageCode = "hi"
            ),
            SubtitleCue(
                startMs = 4000L,
                endMs = 7000L,
                text = "भाषा स्टूडियो में आपका स्वागत है",
                languageCode = "hi"
            )
        )

        val srtText = SrtParser.exportToSrt(cues)
        assertTrue(srtText.contains("00:00:01,000 --> 00:00:03,500"))
        assertTrue(srtText.contains("नमस्ते दोस्तों"))

        val parsed = SrtParser.parseFromSrt(srtText, "hi")
        assertEquals(2, parsed.size)
        assertEquals("नमस्ते दोस्तों", parsed[0].text)
        assertEquals(1000L, parsed[0].startMs)
        assertEquals(3500L, parsed[0].endMs)
    }

    @Test
    fun `test language detection for Hindi and English`() {
        val service = MultilingualTranslationService()
        val hindiDetected = service.detectLanguage("नमस्ते दोस्तों")
        assertEquals("hi", hindiDetected.code)

        val englishDetected = service.detectLanguage("Hello everyone welcome to Bhasha Studio")
        assertEquals("en", englishDetected.code)
    }
}
