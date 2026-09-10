package org.skepsun.kototoro.core.prefs

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.net.ConnectivityManager
import androidx.preference.PreferenceManager
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AppSettingsPageSaveDefaultsTest {

    private val context = mockk<Context>()
    private val preferences = mockk<SharedPreferences>()
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)

    @BeforeEach
    fun setUp() {
        mockkStatic(PreferenceManager::class)
        every { PreferenceManager.getDefaultSharedPreferences(context) } returns preferences
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockk<ConnectivityManager>()
        every { context.resources } returns mockk<Resources> {
            every { getStringArray(any()) } returns emptyArray()
        }
        every { preferences.edit() } returns editor
        every { preferences.contains(any()) } returns false
        every { preferences.getInt(any(), any()) } answers { secondArg() }
        every { preferences.getString(any(), any()) } answers { secondArg() }
        every { preferences.getStringSet(any(), any()) } answers {
            (secondArg<Set<String>?>() ?: emptySet()).toMutableSet()
        }
        every { preferences.getBoolean(any(), any()) } answers { secondArg() }
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(PreferenceManager::class)
    }

    @Test
    fun `page save manga title length defaults to 24 characters`() {
        AppSettings(context).pagesSaveMangaTitleLength shouldBe 24
    }

    @Test
    fun `page save manga title length clamps oversized stored values`() {
        every {
            preferences.getInt(
                AppSettings.KEY_PAGES_SAVE_MANGA_TITLE_LENGTH,
                AppSettings.DEFAULT_PAGES_SAVE_MANGA_TITLE_LENGTH,
            )
        } returns 100

        AppSettings(context).pagesSaveMangaTitleLength shouldBe AppSettings.PAGES_SAVE_MANGA_TITLE_LENGTH_MAX
    }
}
