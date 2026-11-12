package vn.bepan

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import vn.bepan.assistant.MockGateway
import vn.bepan.domain.id

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=android.app.Application::class)
class MockGatewayTest {
    @Test fun fourSampleCommandsResolveFrozenContextAndAmounts() = runBlocking {
        val mock=MockGateway()
        suspend fun call(s:String)=mock.interpret(s,"2026-11-10","2026-11-12",id(),id())
        assertEquals("2026-11-12",call("Hôm nay có bao nhiêu suất trưa?").calls.single().args.getString("date"))
        assertEquals("2026-11-10",call("Ngày đang xem có bao nhiêu tiền đi chợ?").calls.single().args.getString("date"))
        assertEquals("DINNER",call("Cắt bữa tối hôm nay cho Nam").calls.single().args.getString("meal"))
        assertEquals(85000,call("Ghi tiền mua rau hôm nay 85 nghìn").calls.single().args.getLong("amountVnd"))
        assertEquals("2026-11-12",call("Chốt hôm nay và chuẩn bị ngày mai").calls.single().args.getString("date"))
        assertEquals("2026-11-10",call("Cắt bữa trưa ngày 10/11/2026 cho Nguyễn Văn Nam").calls.single().args.getString("date"))
        assertEquals(85000,call("Ghi tiền mua rau ngày 10/11/2026 85 nghìn").calls.single().args.getLong("amountVnd"))
        assertTrue(call("Mua rau 1.000 nghìn").calls.isEmpty())
        assertEquals(1500000,call("Mua rau 1,5 triệu").calls.single().args.getLong("amountVnd"))
    }
}
