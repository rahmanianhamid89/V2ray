package com.example.storage

import android.content.Context
import com.example.logger.AppLogger
import com.example.model.ServerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConfigRepository(private val dao: ServerConfigDao) {

    val allConfigs: Flow<List<ServerConfig>> = dao.getAllConfigs()
        .map { list -> list.map { it.toDomain() } }

    val distinctGroups: Flow<List<String>> = dao.getDistinctGroups()
        .map { groups ->
            val list = groups.filter { it.isNotBlank() }.toMutableList()
            if (!list.contains("Default")) {
                list.add(0, "Default")
            }
            list
        }

    val activeConfig: Flow<ServerConfig?> = dao.getActiveConfigFlow()
        .map { entity -> entity?.toDomain() }

    suspend fun getActiveConfig(): ServerConfig? {
        return dao.getActiveConfig()?.toDomain()
    }

    suspend fun saveConfig(config: ServerConfig): Long {
        val entity = ServerConfigEntity.fromDomain(config)
        val id = dao.insertConfig(entity)
        AppLogger.i("ConfigRepository", "Saved server config ID: $id (Protocol: ${config.protocol}, Address: ${config.address}:${config.port}, Group: ${config.groupName})")
        if (config.isActive || dao.getActiveConfig() == null) {
            setActiveConfig(if (id > 0) id else config.id)
        }
        return id
    }

    suspend fun saveAll(configs: List<ServerConfig>): List<Long> {
        val entities = configs.map { ServerConfigEntity.fromDomain(it) }
        val ids = dao.insertAll(entities)
        AppLogger.i("ConfigRepository", "Batch saved ${ids.size} configs")
        if (dao.getActiveConfig() == null && configs.isNotEmpty()) {
            val firstId = ids.firstOrNull() ?: 0L
            if (firstId > 0) {
                setActiveConfig(firstId)
            }
        }
        return ids
    }

    suspend fun updatePing(id: Long, ping: Long) {
        dao.updatePing(id, ping)
    }

    suspend fun addTraffic(id: Long, txBytes: Long, rxBytes: Long) {
        if (id > 0 && (txBytes > 0 || rxBytes > 0)) {
            dao.addTraffic(id, txBytes, rxBytes)
        }
    }

    suspend fun resetTraffic(id: Long) {
        dao.resetTraffic(id)
        AppLogger.i("ConfigRepository", "Reset traffic stats for config ID: $id")
    }

    suspend fun resetAllTraffic() {
        dao.resetAllTraffic()
        AppLogger.i("ConfigRepository", "Reset traffic stats for all configs.")
    }

    suspend fun renameGroup(oldGroupName: String, newGroupName: String) {
        dao.renameGroup(oldGroupName, newGroupName)
        AppLogger.i("ConfigRepository", "Renamed group '$oldGroupName' to '$newGroupName'")
    }

    suspend fun deleteConfigsByGroup(groupName: String) {
        dao.deleteConfigsByGroup(groupName)
        AppLogger.i("ConfigRepository", "Deleted all configs in group '$groupName'")
    }

    suspend fun deleteConfig(config: ServerConfig) {
        dao.deleteConfigById(config.id)
        AppLogger.i("ConfigRepository", "Deleted server config ID: ${config.id}")
    }

    suspend fun deleteAllConfigs() {
        dao.deleteAllConfigs()
        AppLogger.i("ConfigRepository", "Deleted all server configurations from database.")
    }

    suspend fun setActiveConfig(id: Long) {
        dao.setActiveConfig(id)
        AppLogger.i("ConfigRepository", "Set active server config ID: $id")
    }

    suspend fun ensureDefaultSampleConfigs() {
        // No sample configs created - keeping configs list clean as requested
    }

    companion object {
        @Volatile
        private var INSTANCE: ConfigRepository? = null

        fun getInstance(context: Context): ConfigRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getInstance(context)
                val repo = ConfigRepository(db.serverConfigDao())
                INSTANCE = repo
                repo
            }
        }
    }
}
