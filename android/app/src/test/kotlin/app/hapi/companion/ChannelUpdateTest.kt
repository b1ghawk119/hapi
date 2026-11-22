package app.hapi.companion

import org.junit.Assert.*
import org.junit.Test

class ChannelUpdateTest {
    @Test fun onlyNewerBuildsPrompt() {
        assertNull(newerCode(99, 100))
        assertNull(newerCode(100, 100))
        assertEquals(101, newerCode(101, 100))
    }
    @Test fun downloadIsPinnedToThePromptedVersion() {
        assertEquals("https://github.com/b1ghawk119/hapi/releases/download/android-101/HAPI.apk", apkUrl(101))
    }
}
