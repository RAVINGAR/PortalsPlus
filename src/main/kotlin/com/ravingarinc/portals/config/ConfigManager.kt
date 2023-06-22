package com.ravingarinc.portals.config

import com.github.shynixn.mccoroutine.bukkit.launch
import com.ravingarinc.api.module.RavinPlugin
import com.ravingarinc.api.module.SuspendingModule
import com.ravingarinc.api.module.warn
import com.ravingarinc.portals.api.copyResource
import kotlinx.coroutines.Dispatchers
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.lang.IllegalArgumentException
import java.util.*

class ConfigManager(plugin: RavinPlugin) : SuspendingModule(ConfigManager::class.java, plugin) {
    val config: ConfigFile = ConfigFile(plugin, "config.yml")
    override suspend fun suspendLoad() {

    }

    fun getKey() : UUID? {
        val string = config.config.getString("key.uuid")
        if(string.isNullOrEmpty()) return null
        try {
            return UUID.fromString(string)
        } catch(exception: IllegalArgumentException) {
            warn("Could not parse UUID from config.yml option 'key.uuid'. Please do not modify this yourself!")
            config.config.set("key.uuid", "")
            config.save()
        }
        return null
    }

    fun saveKey(uuid: UUID) {
        config.reload()
        config.config.set("key.uuid", uuid.toString())
        config.save()
    }

    fun resetKey() {
        config.reload()
        config.config.set("key.uuid", "")
    }

    override suspend fun suspendCancel() {
        config.reload()
    }
}
