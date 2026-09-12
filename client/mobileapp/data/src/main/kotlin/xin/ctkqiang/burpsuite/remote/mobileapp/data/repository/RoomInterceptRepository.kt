package xin.ctkqiang.burpsuite.remote.mobileapp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.InterceptIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.data.database.InterceptRecordTable
import xin.ctkqiang.burpsuite.remote.mobileapp.data.database.toDomain
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.InterceptRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.InterceptRepository

/** Room 支撑的拦截读侧。放行与丢弃是命令，写路径不从这里绕过去（rules.md §5.1）。 */
class RoomInterceptRepository(private val interceptRecordTable: InterceptRecordTable) : InterceptRepository {
    override fun observeInterceptRecords(): Flow<List<InterceptRecord>> =
        interceptRecordTable.observeAll().map { entities -> entities.map { entity -> entity.toDomain() } }

    override fun observeInterceptRecord(interceptIdentifier: InterceptIdentifier): Flow<InterceptRecord?> =
        interceptRecordTable.observeByIdentifier(interceptIdentifier.value).map { entity -> entity?.toDomain() }
}
