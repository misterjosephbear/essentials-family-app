package com.isaacshub.app.sleep.data

import com.isaacshub.sleep.core.SleepEntry
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId

class SleepRepository(private val dao: SleepSessionDao) {

    fun observeAll(): Flow<List<SleepSessionEntity>> = dao.observeAll()

    fun observeSince(since: Instant): Flow<List<SleepSessionEntity>> =
        dao.observeSince(since.toEpochMilli())

    suspend fun getById(id: Long): SleepSessionEntity? = dao.getById(id)

    suspend fun addManualSession(start: Instant, end: Instant): Long {
        require(end.isAfter(start)) { "End must be after start" }
        return dao.insert(
            SleepSessionEntity(
                startEpochMillis = start.toEpochMilli(),
                endEpochMillis = end.toEpochMilli(),
                source = SleepSource.MANUAL,
                confirmed = true,
                createdAtEpochMillis = Instant.now().toEpochMilli()
            )
        )
    }

    suspend fun addDetectedSession(start: Instant, end: Instant): Long {
        require(end.isAfter(start)) { "End must be after start" }
        return dao.insert(
            SleepSessionEntity(
                startEpochMillis = start.toEpochMilli(),
                endEpochMillis = end.toEpochMilli(),
                source = SleepSource.AUTO_DETECTED,
                confirmed = false,
                createdAtEpochMillis = Instant.now().toEpochMilli()
            )
        )
    }

    suspend fun confirmSession(id: Long, start: Instant, end: Instant) {
        val existing = dao.getById(id) ?: return
        dao.update(
            existing.copy(
                startEpochMillis = start.toEpochMilli(),
                endEpochMillis = end.toEpochMilli(),
                confirmed = true
            )
        )
    }

    suspend fun deleteSession(session: SleepSessionEntity) = dao.delete(session)
}

fun SleepSessionEntity.toSleepEntry(zoneId: ZoneId = ZoneId.systemDefault()): SleepEntry {
    val wakeDate = Instant.ofEpochMilli(endEpochMillis).atZone(zoneId).toLocalDate()
    return SleepEntry(
        date = wakeDate,
        durationMinutes = durationMinutes.toInt(),
        confirmed = confirmed
    )
}
