package com.isaacshub.essentials.`data`.local.dao

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.isaacshub.essentials.`data`.local.entities.CompletionStatus
import com.isaacshub.essentials.`data`.local.entities.CompletionStatusConverter
import com.isaacshub.essentials.`data`.local.entities.LocalCompletionEntity
import javax.`annotation`.processing.Generated
import kotlin.Boolean
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
public class CompletionDao_Impl(
  __db: RoomDatabase,
) : CompletionDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfLocalCompletionEntity: EntityInsertAdapter<LocalCompletionEntity>

  private val __completionStatusConverter: CompletionStatusConverter = CompletionStatusConverter()

  private val __updateAdapterOfLocalCompletionEntity:
      EntityDeleteOrUpdateAdapter<LocalCompletionEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfLocalCompletionEntity = object : EntityInsertAdapter<LocalCompletionEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `local_completions` (`id`,`choreId`,`completionDate`,`photoUri`,`aiVerificationResult`,`status`,`syncedToServer`,`completedAtEpochMillis`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: LocalCompletionEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.choreId)
        statement.bindText(3, entity.completionDate)
        val _tmpPhotoUri: String? = entity.photoUri
        if (_tmpPhotoUri == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpPhotoUri)
        }
        val _tmpAiVerificationResult: String? = entity.aiVerificationResult
        if (_tmpAiVerificationResult == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpAiVerificationResult)
        }
        val _tmp: String = __completionStatusConverter.fromStatus(entity.status)
        statement.bindText(6, _tmp)
        val _tmp_1: Int = if (entity.syncedToServer) 1 else 0
        statement.bindLong(7, _tmp_1.toLong())
        statement.bindLong(8, entity.completedAtEpochMillis)
      }
    }
    this.__updateAdapterOfLocalCompletionEntity = object : EntityDeleteOrUpdateAdapter<LocalCompletionEntity>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `local_completions` SET `id` = ?,`choreId` = ?,`completionDate` = ?,`photoUri` = ?,`aiVerificationResult` = ?,`status` = ?,`syncedToServer` = ?,`completedAtEpochMillis` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: LocalCompletionEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.choreId)
        statement.bindText(3, entity.completionDate)
        val _tmpPhotoUri: String? = entity.photoUri
        if (_tmpPhotoUri == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpPhotoUri)
        }
        val _tmpAiVerificationResult: String? = entity.aiVerificationResult
        if (_tmpAiVerificationResult == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpAiVerificationResult)
        }
        val _tmp: String = __completionStatusConverter.fromStatus(entity.status)
        statement.bindText(6, _tmp)
        val _tmp_1: Int = if (entity.syncedToServer) 1 else 0
        statement.bindLong(7, _tmp_1.toLong())
        statement.bindLong(8, entity.completedAtEpochMillis)
        statement.bindLong(9, entity.id)
      }
    }
  }

  public override suspend fun insert(completion: LocalCompletionEntity): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfLocalCompletionEntity.insertAndReturnId(_connection, completion)
    _result
  }

  public override suspend fun update(completion: LocalCompletionEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfLocalCompletionEntity.handle(_connection, completion)
  }

  public override fun observeByDate(dateString: String): Flow<List<LocalCompletionEntity>> {
    val _sql: String = "SELECT * FROM local_completions WHERE completionDate = ?"
    return createFlow(__db, false, arrayOf("local_completions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, dateString)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfChoreId: Int = getColumnIndexOrThrow(_stmt, "choreId")
        val _columnIndexOfCompletionDate: Int = getColumnIndexOrThrow(_stmt, "completionDate")
        val _columnIndexOfPhotoUri: Int = getColumnIndexOrThrow(_stmt, "photoUri")
        val _columnIndexOfAiVerificationResult: Int = getColumnIndexOrThrow(_stmt, "aiVerificationResult")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfSyncedToServer: Int = getColumnIndexOrThrow(_stmt, "syncedToServer")
        val _columnIndexOfCompletedAtEpochMillis: Int = getColumnIndexOrThrow(_stmt, "completedAtEpochMillis")
        val _result: MutableList<LocalCompletionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: LocalCompletionEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpChoreId: Long
          _tmpChoreId = _stmt.getLong(_columnIndexOfChoreId)
          val _tmpCompletionDate: String
          _tmpCompletionDate = _stmt.getText(_columnIndexOfCompletionDate)
          val _tmpPhotoUri: String?
          if (_stmt.isNull(_columnIndexOfPhotoUri)) {
            _tmpPhotoUri = null
          } else {
            _tmpPhotoUri = _stmt.getText(_columnIndexOfPhotoUri)
          }
          val _tmpAiVerificationResult: String?
          if (_stmt.isNull(_columnIndexOfAiVerificationResult)) {
            _tmpAiVerificationResult = null
          } else {
            _tmpAiVerificationResult = _stmt.getText(_columnIndexOfAiVerificationResult)
          }
          val _tmpStatus: CompletionStatus
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfStatus)
          _tmpStatus = __completionStatusConverter.toStatus(_tmp)
          val _tmpSyncedToServer: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfSyncedToServer).toInt()
          _tmpSyncedToServer = _tmp_1 != 0
          val _tmpCompletedAtEpochMillis: Long
          _tmpCompletedAtEpochMillis = _stmt.getLong(_columnIndexOfCompletedAtEpochMillis)
          _item = LocalCompletionEntity(_tmpId,_tmpChoreId,_tmpCompletionDate,_tmpPhotoUri,_tmpAiVerificationResult,_tmpStatus,_tmpSyncedToServer,_tmpCompletedAtEpochMillis)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getByChoreAndDate(choreId: Long, dateString: String): LocalCompletionEntity? {
    val _sql: String = "SELECT * FROM local_completions WHERE choreId = ? AND completionDate = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, choreId)
        _argIndex = 2
        _stmt.bindText(_argIndex, dateString)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfChoreId: Int = getColumnIndexOrThrow(_stmt, "choreId")
        val _columnIndexOfCompletionDate: Int = getColumnIndexOrThrow(_stmt, "completionDate")
        val _columnIndexOfPhotoUri: Int = getColumnIndexOrThrow(_stmt, "photoUri")
        val _columnIndexOfAiVerificationResult: Int = getColumnIndexOrThrow(_stmt, "aiVerificationResult")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfSyncedToServer: Int = getColumnIndexOrThrow(_stmt, "syncedToServer")
        val _columnIndexOfCompletedAtEpochMillis: Int = getColumnIndexOrThrow(_stmt, "completedAtEpochMillis")
        val _result: LocalCompletionEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpChoreId: Long
          _tmpChoreId = _stmt.getLong(_columnIndexOfChoreId)
          val _tmpCompletionDate: String
          _tmpCompletionDate = _stmt.getText(_columnIndexOfCompletionDate)
          val _tmpPhotoUri: String?
          if (_stmt.isNull(_columnIndexOfPhotoUri)) {
            _tmpPhotoUri = null
          } else {
            _tmpPhotoUri = _stmt.getText(_columnIndexOfPhotoUri)
          }
          val _tmpAiVerificationResult: String?
          if (_stmt.isNull(_columnIndexOfAiVerificationResult)) {
            _tmpAiVerificationResult = null
          } else {
            _tmpAiVerificationResult = _stmt.getText(_columnIndexOfAiVerificationResult)
          }
          val _tmpStatus: CompletionStatus
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfStatus)
          _tmpStatus = __completionStatusConverter.toStatus(_tmp)
          val _tmpSyncedToServer: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfSyncedToServer).toInt()
          _tmpSyncedToServer = _tmp_1 != 0
          val _tmpCompletedAtEpochMillis: Long
          _tmpCompletedAtEpochMillis = _stmt.getLong(_columnIndexOfCompletedAtEpochMillis)
          _result = LocalCompletionEntity(_tmpId,_tmpChoreId,_tmpCompletionDate,_tmpPhotoUri,_tmpAiVerificationResult,_tmpStatus,_tmpSyncedToServer,_tmpCompletedAtEpochMillis)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getUnsyncedCompletions(): List<LocalCompletionEntity> {
    val _sql: String = "SELECT * FROM local_completions WHERE syncedToServer = 0"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfChoreId: Int = getColumnIndexOrThrow(_stmt, "choreId")
        val _columnIndexOfCompletionDate: Int = getColumnIndexOrThrow(_stmt, "completionDate")
        val _columnIndexOfPhotoUri: Int = getColumnIndexOrThrow(_stmt, "photoUri")
        val _columnIndexOfAiVerificationResult: Int = getColumnIndexOrThrow(_stmt, "aiVerificationResult")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfSyncedToServer: Int = getColumnIndexOrThrow(_stmt, "syncedToServer")
        val _columnIndexOfCompletedAtEpochMillis: Int = getColumnIndexOrThrow(_stmt, "completedAtEpochMillis")
        val _result: MutableList<LocalCompletionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: LocalCompletionEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpChoreId: Long
          _tmpChoreId = _stmt.getLong(_columnIndexOfChoreId)
          val _tmpCompletionDate: String
          _tmpCompletionDate = _stmt.getText(_columnIndexOfCompletionDate)
          val _tmpPhotoUri: String?
          if (_stmt.isNull(_columnIndexOfPhotoUri)) {
            _tmpPhotoUri = null
          } else {
            _tmpPhotoUri = _stmt.getText(_columnIndexOfPhotoUri)
          }
          val _tmpAiVerificationResult: String?
          if (_stmt.isNull(_columnIndexOfAiVerificationResult)) {
            _tmpAiVerificationResult = null
          } else {
            _tmpAiVerificationResult = _stmt.getText(_columnIndexOfAiVerificationResult)
          }
          val _tmpStatus: CompletionStatus
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfStatus)
          _tmpStatus = __completionStatusConverter.toStatus(_tmp)
          val _tmpSyncedToServer: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfSyncedToServer).toInt()
          _tmpSyncedToServer = _tmp_1 != 0
          val _tmpCompletedAtEpochMillis: Long
          _tmpCompletedAtEpochMillis = _stmt.getLong(_columnIndexOfCompletedAtEpochMillis)
          _item = LocalCompletionEntity(_tmpId,_tmpChoreId,_tmpCompletionDate,_tmpPhotoUri,_tmpAiVerificationResult,_tmpStatus,_tmpSyncedToServer,_tmpCompletedAtEpochMillis)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun markAsSynced(id: Long) {
    val _sql: String = "UPDATE local_completions SET syncedToServer = 1 WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
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
