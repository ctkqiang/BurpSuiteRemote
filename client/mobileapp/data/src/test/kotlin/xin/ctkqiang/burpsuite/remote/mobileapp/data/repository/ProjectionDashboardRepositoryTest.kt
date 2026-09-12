package xin.ctkqiang.burpsuite.remote.mobileapp.data.repository

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.InterceptIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryArchiveState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.InterceptRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.InterceptState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.HistoryRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.InterceptRepository
import java.time.Instant

class ProjectionDashboardRepositoryTest {
    @Test
    fun `the summary follows the projections and the connection state`() =
        runTest {
            val historyRecords =
                listOf(
                    historyRecord("history_1", 1, "api.example.com", HistoryArchiveState.Live),
                    historyRecord("history_2", 2, "api.admin.example.com", HistoryArchiveState.Archived),
                    historyRecord("history_3", 3, "api.latest.example.com", HistoryArchiveState.Live),
                )
            val historyRepository = mockk<HistoryRepository>()
            every { historyRepository.observeHistoryRecords() } returns flowOf(historyRecords)

            val interceptRecords =
                listOf(
                    interceptRecord("intercept_1", 4, InterceptState.Pending),
                    interceptRecord("intercept_2", 5, InterceptState.Forwarded),
                )
            val interceptRepository = mockk<InterceptRepository>()
            every { interceptRepository.observeInterceptRecords() } returns flowOf(interceptRecords)

            val dashboardRepository =
                ProjectionDashboardRepository(
                    historyRepository = historyRepository,
                    interceptRepository = interceptRepository,
                    connectionState = MutableStateFlow(ConnectionState.Connected),
                )

            val summary = dashboardRepository.observeDashboardSummary().first()

            assertEquals(ConnectionState.Connected, summary.connectionState)
            // 历史列表按序号升序，最后一条就是最近的一条，面板显示它的目标主机。
            assertEquals("api.latest.example.com", summary.targetHost)
            assertEquals(2, summary.liveRequestCount)
            assertEquals(1, summary.savedCount)
            // 已放行的拦截项不再停在队列里，因此不计入。
            assertEquals(1, summary.interceptedCount)
        }

    private fun historyRecord(
        historyIdentifier: String,
        sequenceNumber: Long,
        host: String?,
        archiveState: HistoryArchiveState,
    ): HistoryRecord =
        HistoryRecord(
            historyIdentifier = HistoryIdentifier(historyIdentifier),
            sequenceNumber = sequenceNumber,
            occurredAt = FIXED_INSTANT,
            host = host,
            method = "GET",
            scheme = "https",
            path = "/api/user",
            statusCode = 200,
            mimeType = "application/json",
            responseLength = 1_024L,
            usesTls = true,
            destinationInternetProtocolAddress = "127.0.0.1",
            listenerPort = 8_080,
            durationMilliseconds = 12L,
            isEdited = false,
            title = null,
            archiveState = archiveState,
            annotationCount = 0,
            lastAnnotatedAt = null,
            savedAt = null,
        )

    private fun interceptRecord(
        interceptIdentifier: String,
        sequenceNumber: Long,
        state: InterceptState,
    ): InterceptRecord =
        InterceptRecord(
            interceptIdentifier = InterceptIdentifier(interceptIdentifier),
            sequenceNumber = sequenceNumber,
            createdAt = FIXED_INSTANT,
            updatedAt = FIXED_INSTANT,
            state = state,
            host = "api.example.com",
            method = "POST",
            path = "/login",
        )

    private companion object {
        val FIXED_INSTANT: Instant = Instant.parse("2026-01-01T00:00:00Z")
    }
}
