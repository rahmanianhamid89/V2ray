package com.example.storage

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerConfigDao {
    @Query("SELECT * FROM server_configs ORDER BY createdAt DESC")
    fun getAllConfigs(): Flow<List<ServerConfigEntity>>

    @Query("SELECT * FROM server_configs WHERE groupName = :groupName ORDER BY createdAt DESC")
    fun getConfigsByGroup(groupName: String): Flow<List<ServerConfigEntity>>

    @Query("SELECT DISTINCT groupName FROM server_configs")
    fun getDistinctGroups(): Flow<List<String>>

    @Query("SELECT * FROM server_configs WHERE isActive = 1 LIMIT 1")
    fun getActiveConfigFlow(): Flow<ServerConfigEntity?>

    @Query("SELECT * FROM server_configs WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveConfig(): ServerConfigEntity?

    @Query("SELECT * FROM server_configs WHERE id = :id LIMIT 1")
    suspend fun getConfigById(id: Long): ServerConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(entity: ServerConfigEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ServerConfigEntity>): List<Long>

    @Update
    suspend fun updateConfig(entity: ServerConfigEntity)

    @Query("UPDATE server_configs SET ping = :ping WHERE id = :id")
    suspend fun updatePing(id: Long, ping: Long)

    @Query("UPDATE server_configs SET uploadBytes = uploadBytes + :txBytes, downloadBytes = downloadBytes + :rxBytes WHERE id = :id")
    suspend fun addTraffic(id: Long, txBytes: Long, rxBytes: Long)

    @Query("UPDATE server_configs SET uploadBytes = 0, downloadBytes = 0 WHERE id = :id")
    suspend fun resetTraffic(id: Long)

    @Query("UPDATE server_configs SET uploadBytes = 0, downloadBytes = 0")
    suspend fun resetAllTraffic()

    @Query("UPDATE server_configs SET groupName = :newGroupName WHERE groupName = :oldGroupName")
    suspend fun renameGroup(oldGroupName: String, newGroupName: String)

    @Delete
    suspend fun deleteConfig(entity: ServerConfigEntity)

    @Query("DELETE FROM server_configs WHERE id = :id")
    suspend fun deleteConfigById(id: Long)

    @Query("DELETE FROM server_configs WHERE groupName = :groupName")
    suspend fun deleteConfigsByGroup(groupName: String)

    @Query("DELETE FROM server_configs WHERE subUrl = :subUrl")
    suspend fun deleteConfigsBySubUrl(subUrl: String)

    @Query("DELETE FROM server_configs")
    suspend fun deleteAllConfigs()

    @Query("UPDATE server_configs SET isActive = 0")
    suspend fun clearActiveFlags()

    @Query("UPDATE server_configs SET isActive = 1 WHERE id = :id")
    suspend fun setActiveFlag(id: Long)

    @Transaction
    suspend fun setActiveConfig(id: Long) {
        clearActiveFlags()
        setActiveFlag(id)
    }
}
