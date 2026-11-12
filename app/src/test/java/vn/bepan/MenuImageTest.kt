package vn.bepan

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import vn.bepan.data.Menu
import vn.bepan.ui.MenuImage

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=android.app.Application::class)
class MenuImageTest {
    @Test fun fourSlotsAndContentControlsHeight() {
        val slots=MenuImage.slots("2026-11-30")
        assertEquals(4,slots.size); assertEquals("2026-12-01",slots.last().first)
        val short=MenuImage.render("2026-11-30", emptyList(),540)
        val long=MenuImage.render("2026-11-30",listOf(Menu("2026-12-01","BREAKFAST",(1..60).joinToString("\n") { "Món sáng ngày kế tiếp số $it" })),540)
        assertTrue(long.height>short.height+1000); assertEquals(540,long.width)
        short.recycle(); long.recycle()
    }
}
