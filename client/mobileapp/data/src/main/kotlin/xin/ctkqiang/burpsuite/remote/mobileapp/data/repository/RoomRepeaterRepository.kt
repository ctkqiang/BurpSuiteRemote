// Room 支撑的 Repeater 读侧。

package xin.ctkqiang.burpsuite.remote.mobileapp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.RepeaterRequestIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.data.database.RepeaterRecordTable
import xin.ctkqiang.burpsuite.remote.mobileapp.data.database.toDomain
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.RepeaterRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.RepeaterRepository

/** Room 支撑的 Repeater 读侧。执行是命令，写路径不从这里绕过去（rules.md §5.1）。 */
class RoomRepeaterRepository(
    private val repeaterRecordTable: RepeaterRecordTable,
) : RepeaterRepository {
    override fun observeRepeaterRecords(): Flow<List<RepeaterRecord>> =
        repeaterRecordTable.observeAll().map { entities -> entities.map { entity -> entity.toDomain() } }

    override fun observeRepeaterRecord(repeaterRequestIdentifier: RepeaterRequestIdentifier): Flow<RepeaterRecord?> =
        repeaterRecordTable
            .observeByIdentifier(repeaterRequestIdentifier.value)
            .map { entity -> entity?.toDomain() }
}
