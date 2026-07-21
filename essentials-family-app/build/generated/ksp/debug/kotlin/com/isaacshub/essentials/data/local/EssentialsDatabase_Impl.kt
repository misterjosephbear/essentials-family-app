package com.isaacshub.essentials.`data`.local

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.isaacshub.essentials.`data`.local.dao.ChoreDao
import com.isaacshub.essentials.`data`.local.dao.ChoreDao_Impl
import com.isaacshub.essentials.`data`.local.dao.CompletionDao
import com.isaacshub.essentials.`data`.local.dao.CompletionDao_Impl
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class EssentialsDatabase_Impl : EssentialsDatabase() {
  private val _choreDao: Lazy<ChoreDao> = lazy {
    ChoreDao_Impl(this)
  }

  private val _completionDao: Lazy<CompletionDao> = lazy {
    CompletionDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(1, "a4fc6849f9fae7a7f7437442c2fcb60d", "083c7fdd6f8fffdc36ef6a3ce734e580") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `local_chores` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `photoRequirement` TEXT, `daysOfWeek` TEXT NOT NULL, `syncedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `local_completions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `choreId` INTEGER NOT NULL, `completionDate` TEXT NOT NULL, `photoUri` TEXT, `aiVerificationResult` TEXT, `status` TEXT NOT NULL, `syncedToServer` INTEGER NOT NULL, `completedAtEpochMillis` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'a4fc6849f9fae7a7f7437442c2fcb60d')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `local_chores`")
        connection.execSQL("DROP TABLE IF EXISTS `local_completions`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsLocalChores: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsLocalChores.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalChores.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalChores.put("description", TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalChores.put("photoRequirement", TableInfo.Column("photoRequirement", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalChores.put("daysOfWeek", TableInfo.Column("daysOfWeek", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalChores.put("syncedAtEpochMillis", TableInfo.Column("syncedAtEpochMillis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysLocalChores: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesLocalChores: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoLocalChores: TableInfo = TableInfo("local_chores", _columnsLocalChores, _foreignKeysLocalChores, _indicesLocalChores)
        val _existingLocalChores: TableInfo = read(connection, "local_chores")
        if (!_infoLocalChores.equals(_existingLocalChores)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |local_chores(com.isaacshub.essentials.data.local.entities.LocalChoreEntity).
              | Expected:
              |""".trimMargin() + _infoLocalChores + """
              |
              | Found:
              |""".trimMargin() + _existingLocalChores)
        }
        val _columnsLocalCompletions: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsLocalCompletions.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalCompletions.put("choreId", TableInfo.Column("choreId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalCompletions.put("completionDate", TableInfo.Column("completionDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalCompletions.put("photoUri", TableInfo.Column("photoUri", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalCompletions.put("aiVerificationResult", TableInfo.Column("aiVerificationResult", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalCompletions.put("status", TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalCompletions.put("syncedToServer", TableInfo.Column("syncedToServer", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLocalCompletions.put("completedAtEpochMillis", TableInfo.Column("completedAtEpochMillis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysLocalCompletions: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesLocalCompletions: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoLocalCompletions: TableInfo = TableInfo("local_completions", _columnsLocalCompletions, _foreignKeysLocalCompletions, _indicesLocalCompletions)
        val _existingLocalCompletions: TableInfo = read(connection, "local_completions")
        if (!_infoLocalCompletions.equals(_existingLocalCompletions)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |local_completions(com.isaacshub.essentials.data.local.entities.LocalCompletionEntity).
              | Expected:
              |""".trimMargin() + _infoLocalCompletions + """
              |
              | Found:
              |""".trimMargin() + _existingLocalCompletions)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "local_chores", "local_completions")
  }

  public override fun clearAllTables() {
    super.performClear(false, "local_chores", "local_completions")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(ChoreDao::class, ChoreDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(CompletionDao::class, CompletionDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun choreDao(): ChoreDao = _choreDao.value

  public override fun completionDao(): CompletionDao = _completionDao.value
}
