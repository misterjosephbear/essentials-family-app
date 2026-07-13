package com.isaacshub.app.timetracking.data

import kotlinx.coroutines.flow.Flow
import java.time.Instant

class TimeTrackingRepository(
    private val timeEntryDao: TimeEntryDao,
    private val routeDao: RouteDao,
    private val deductionDao: DeductionDao
) {
    fun observeEntries(): Flow<List<TimeEntryEntity>> = timeEntryDao.observeAll()

    suspend fun getEntryById(id: Long): TimeEntryEntity? = timeEntryDao.getById(id)

    suspend fun saveEntry(entry: TimeEntryEntity): Long =
        if (entry.id == 0L) {
            timeEntryDao.insert(entry.copy(createdAtEpochMillis = Instant.now().toEpochMilli()))
        } else {
            timeEntryDao.update(entry)
            entry.id
        }

    suspend fun deleteEntry(entry: TimeEntryEntity) = timeEntryDao.delete(entry)

    fun observeRoutes(): Flow<List<RouteEntity>> = routeDao.observeAll()

    suspend fun getRouteById(id: Long): RouteEntity? = routeDao.getById(id)

    suspend fun saveRoute(route: RouteEntity): Long =
        if (route.id == 0L) routeDao.insert(route) else {
            routeDao.update(route)
            route.id
        }

    suspend fun deleteRoute(route: RouteEntity) = routeDao.delete(route)

    fun observeDeductions(): Flow<List<DeductionEntity>> = deductionDao.observeAll()

    suspend fun addDeduction(deduction: DeductionEntity): Long = deductionDao.insert(deduction)

    suspend fun deleteDeduction(deduction: DeductionEntity) = deductionDao.delete(deduction)
}
