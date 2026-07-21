package com.isaacshub.essentials.`data`.local.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.isaacshub.essentials.`data`.local.entities.DayOfWeekListConverter
import com.isaacshub.essentials.`data`.local.entities.LocalChoreEntity
import java.time.DayOfWeek
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class ChoreDao_Impl(
  __db: RoomDatabase,
) : ChoreDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfLocalChoreEntity: EntityInsertAdapter<LocalChoreEntity>

  private val __dayOfWeekListConverter: DayOfWeekListConverter = DayOfWeekListConverter()
  init {
    this.__db = __db
    this.__insertAdapterOfLocalChoreEntity = object : EntityInsertAdapter<LocalChoreEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `local_chores` (`id`,`name`,`description`,`photoRequirement`,`daysOfWeek`,`syncedAtEpochMillis`) VALUES (?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: LocalChoreEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.description)
        val _tmpPhotoRequirement: String? = entity.photoRequirement
        if (_tmpPhotoRequirement == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpPhotoRequirement)
        }
        val _tmp: String = __dayOfWeekListConverter.fromDayOfWeekList(entity.daysOfWeek)
        statement.bindText(5, _tmp)
        statement.bindLong(6, entity.syncedAtEpochMillis)
      }
    }
  }

  public override suspend fun insert(chore: LocalChoreEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfLocalChoreEntity.insert(_connection, chore)
  }

  public override suspend fun insertAll(chores: List<LocalChoreEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfLocalChoreEntity.insert(_connection, chores)
  }

  public override fun observeAll(): Flow<List<LocalChoreEntity>> {
    val _sql: String = "SELECT * FROM local_chores"
    return createFlow(__db, false, arrayOf("local_chores")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfPhotoRequirement: Int = getColumnIndexOrThrow(_stmt, "photoRequirement")
        val _columnIndexOfDaysOfWeek: Int = getColumnIndexOrThrow(_stmt, "daysOfWeek")
        val _columnIndexOfSyncedAtEpochMillis: Int = getColumnIndexOrThrow(_stmt, "syncedAtEpochMillis")
        val _result: MutableList<LocalChoreEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: LocalChoreEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpPhotoRequirement: String?
          if (_stmt.isNull(_columnIndexOfPhotoRequirement)) {
            _tmpPhotoRequirement = null
          } else {
            _tmpPhotoRequirement = _stmt.getText(_columnIndexOfPhotoRequirement)
          }
          val _tmpDaysOfWeek: List<DayOfWeek>
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfDaysOfWeek)
          _tmpDaysOfWeek = __dayOfWeekListConverter.toDayOfWeekList(_tmp)
          val _tmpSyncedAtEpochMillis: Long
          _tmpSyncedAtEpochMillis = _stmt.getLong(_columnIndexOfSyncedAtEpochMillis)
          _item = LocalChoreEntity(_tmpId,_tmpName,_tmpDescription,_tmpPhotoRequirement,_tmpDaysOfWeek,_tmpSyncedAtEpochMillis)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getById(id: Long): LocalChoreEntity? {
    val _sql: String = "SELECT * FROM local_chores WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfPhotoRequirement: Int = getColumnIndexOrThrow(_stmt, "photoRequirement")
        val _columnIndexOfDaysOfWeek: Int = getColumnIndexOrThrow(_stmt, "daysOfWeek")
        val _columnIndexOfSyncedAtEpochMillis: Int = getColumnIndexOrThrow(_stmt, "syncedAtEpochMillis")
        val _result: LocalChoreEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpPhotoRequirement: String?
          if (_stmt.isNull(_columnIndexOfPhotoRequirement)) {
            _tmpPhotoRequirement = null
          } else {
            _tmpPhotoRequirement = _stmt.getText(_columnIndexOfPhotoRequirement)
          }
          val _tmpDaysOfWeek: List<DayOfWeek>
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfDaysOfWeek)
          _tmpDaysOfWeek = __dayOfWeekListConverter.toDayOfWeekList(_tmp)
          val _tmpSyncedAtEpochMillis: Long
          _tmpSyncedAtEpochMillis = _stmt.getLong(_columnIndexOfSyncedAtEpochMillis)
          _result = LocalChoreEntity(_tmpId,_tmpName,_tmpDescription,_tmpPhotoRequirement,_tmpDaysOfWeek,_tmpSyncedAtEpochMillis)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAll() {
    val _sql: String = "DELETE FROM local_chores"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
