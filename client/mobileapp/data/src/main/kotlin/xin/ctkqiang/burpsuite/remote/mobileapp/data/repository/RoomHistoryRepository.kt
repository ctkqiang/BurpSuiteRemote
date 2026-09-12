package xin.ctkqiang.burpsuite.remote.mobileapp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.data.database.HistoryRecordTable
import xin.ctkqiang.burpsuite.remote.mobileapp.data.database.toDomain
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.HistoryRepository

/** Room 支撑的历史读侧。读的是投影，界面拿不到实体，只拿到领域读模型（plan §42）。 */
class RoomHistoryRepository(private val historyRecordTable: HistoryRecordTable) : HistoryRepository {
    override fun observeHistoryRecords(): Flow<List<HistoryRecord>> =
        historyRecordTable.observeAll().map { entities -> entities.map { entity -> entity.toDomain() } }

    override fun observeHistoryRecord(historyIdentifier: HistoryIdentifier): Flow<HistoryRecord?> =
        historyRecordTable.observeByIdentifier(historyIdentifier.value).map { entity -> entity?.toDomain() }

    override suspend fun readHistoryRecord(historyIdentifier: HistoryIdentifier): HistoryRecord? =
        historyRecordTable.findByIdentifier(historyIdentifier.value)?.toDomain()
}
