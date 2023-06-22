package com.ravingarinc.portals.api

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import com.ravingarinc.api.module.Module
import com.ravingarinc.api.module.RavinPlugin
import com.ravingarinc.api.module.warn
import io.lumine.mythic.lib.api.item.NBTItem
import kotlinx.coroutines.*
import net.Indyuce.mmoitems.api.interaction.util.DurabilityItem
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.plugin.Plugin
import org.bukkit.util.BlockVector
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random


fun <T : Module> RavinPlugin.withModule(module: Class<T>, function: T.() -> Unit) {
    val m = this.getModule(module)
    if (m.isLoaded) {
        function.invoke(m)
    } else {
        warn("Could not execute function with module ${module.name} as this module has not been loaded!")
    }
}

fun formatMilliseconds(milliseconds: Long): String {
    if (milliseconds == -1L) {
        return "Nil"
    }
    return if (milliseconds > 1000) {
        "${milliseconds / 1000.0F} s"
    } else {
        "$milliseconds ms"
    }
}

fun Double.roll(): Boolean {
    return this > 0.0 && (this >= 1.0 || Random.nextDouble() < this)
}

fun ConfigurationSection.getPercentage(path: String): Double {
    val string = getString(path)
    if (string == null) {
        warn("Could not find option at $path in ${this.name}!")
        return 0.0
    }
    return parsePercentage(string)
}

fun parsePercentage(string: String): Double {
    if(string == "null") {
        return 0.0
    }
    var double = string.replace("%", "").toDoubleOrNull()
    if (double == null) {
        warn("Could not parse $string as a percentage! Format must be 0.4 or 40%!")
        return 0.0
    }
    if (double > 1.0) {
        double /= 100.0
    }
    return double
}

fun ConfigurationSection.getRange(path: String): IntRange {
    if (!this.contains(path)) {
        warn("Could not find option at $path in ${this.name}!")
        return IntRange(1, 1)
    }
    return parseRange(getString(path)!!.replace(" ", ""))
}

fun parseRange(string: String): IntRange {
    val split: List<String> = string.split("-", "to", limit = 2)
    if (split.size == 2) {
        val min = split[0].toIntOrNull()
        val max = split[1].toIntOrNull()
        if (min == null) {
            warn("Could not parse minimum value of '${split[0]}' as a valid number!")
            return IntRange(0, 0)
        }
        if (max == null) {
            warn("Could not parse maximum value of '${split[1]}' as a valid number!")
            return IntRange(0, 0)
        }
        return IntRange(min, max)
    } else {
        string.toIntOrNull()?.let {
            return IntRange(it,it)
        }
    }
    warn("Could not parse $string as a valid range! Please use the format '3-4', '3to4' or a single number such as 3!")
    return IntRange(0, 0)
}

fun ConfigurationSection.getWorld(path: String) : World? {
    val string = getString(path)
    if(string == null) {
        warn("Could not find option at path '$path' in section '${this.name}'")
        return null
    }
    val world = Bukkit.getWorld(string)
    if(world == null) {
        warn("Could not find world called '$string' in section '${this.name}'")
    }
    return world;
}

fun ConfigurationSection.getIntPair(path: String) : Pair<Int, Int>? {
    val string = getString(path)
    if(string == null) {
        warn("Could not find option at path '$path' in section '${this.name}'")
        return null
    }
    val split = string.replace(" ", "").split(",", ";", "-", limit = 2)
    val first = split[0].toIntOrNull()
    val second = split[1].toIntOrNull()
    if(first == null || second == null) {
        return null
    }
    return Pair(first, second)
}

fun ConfigurationSection.getBlockVector(path: String) : BlockVector? {
    val string = getString(path)
    if(string == null) {
        warn("Could not find option at path '$path' in section '${this.name}'")
        return null
    }
    return parseBlockVector(string)
}

fun parseBlockVector(string: String) : BlockVector? {
    if(string == "null") return null
    val split = string.replace(" ", "").split(",", ";", "-", limit = 3)
    val x = split[0].toDoubleOrNull()
    val y = split[1].toDoubleOrNull()
    val z = split[2].toDoubleOrNull()
    if(x == null || y == null || z == null) {
        return null
    }
    return BlockVector(x, y, z)
}

/**
 * Parses the duration in either milliseconds, ticks, seconds, minutes or hours.
 * Returns a value in ticks
 */
fun ConfigurationSection.getDuration(path: String) : Long? {
    val string = getString(path)
    if(string == null) {
        warn("Could not find option at path '$path' in section '${this.name}'")
        return null
    }
    val formatted = string.replace(" ", "")

    for(type in Extensions.timeTypes) {
        return type.format(this@getDuration.name, formatted) ?: continue
    }
    warn("Could not discern duration from string '$string' in section '${this.name}'. Please specify 'ms', 't', 's', 'm', or 'h' after the number!")
    return null
}

private object Extensions {
    val timeTypes: Array<TimeFormat> = arrayOf(
        TimeFormat(arrayOf("ms", "milliseconds", "millisecond")) { it / 50L },
        TimeFormat(arrayOf("t", "ticks", "tick")) { it },
        TimeFormat(arrayOf("s", "secs", "sec", "seconds", "second")) { it * 20L },
        TimeFormat(arrayOf("m", "mins", "min", "minutes", "minute")) { it * 1200L },
        TimeFormat(arrayOf("h", "hours", "hour")) { it * 72000L }
    )
}

private class TimeFormat(val suffixes: Array<String>, val formatter: (Long) -> Long) {
    fun format(section: String, string: String) : Long? {
        for(suffix in suffixes) {
            if(string.endsWith(suffix)) {
                val long = string.replace(suffix, "").toLongOrNull()
                if(long == null) {
                    warn("Could not format duration in section '${section}' as the value '${string}' does contain a number")
                    break;
                }
                return formatter.invoke(long)
            }
        }
        return null
    }
}

fun parseMob(string: String) : Pair<MobType, Double>? {
    if(string == "null") return null
    val split = string.split(":".toRegex(), limit = 2)
    if(split.size < 2) {
        warn("Incorrect syntax for mob type '$string'. Please use the format <type>:<identifier>!")
        return null
    }
    if(split[0].equals("mythic", true) || split[0].equals("mythicmobs", true) || split[0].equals("mm", true)) {
        val subSplit = split[1].split(",".toRegex(), limit = 3)
        val id = subSplit[0]
        val weight = subSplit[1].toDoubleOrNull()
        if(weight == null) {
            warn("Could not parse weight for mob string '$string'! Please specify a valid number!")
            return null
        }
        return Pair(MythicMobType(id), weight)
    } else if (split[0].equals("vanilla", true) || split[0].equals("v", true)) {
        val subSplit = split[1].split(",".toRegex(), limit = 2)
        val id = subSplit[0]
        val weight = subSplit[1].toDoubleOrNull()
        if(weight == null) {
            warn("Could not parse weight for mob string '$string'! Please specify a valid number!")
            return null
        }
        var entity : EntityType? = null
        for(type in EntityType.values()) {
            if(type.name.equals(id, true)) {
                entity = type
            }
        }
        if(entity == null) {
            warn("Could not find vanilla entity type called '${id}' in string '${string}'")
            return null
        }
        return Pair(VanillaMobType(entity), weight)
    } else {
        warn("Unknown mob type '${split[0]}' found for string $string! Please use 'mythic' or 'vanilla'")
    }
    return null
}

fun ConfigurationSection.getMobType(path: String) : MobType? {
    val string = getString(path)
    if (string == null) {
        warn("Could not find option at path '$path' in section '${this.name}'")
        return null
    }
    return parseMobType(string)
}

fun parseMobType(string: String) : MobType? {
    val split = string.lowercase().split(":".toRegex(), limit = 2)
    if(split[0] == "mythic" || split[0] == "mythicmobs" || split[0] == "mm") {
        return MythicMobType(split[1])
    } else if (split[0] == "vanilla" || split[0] == "v") {
        val id = split[1]
        var entity : EntityType? = null
        for(type in EntityType.values()) {
            if(type.name.equals(id, true)) {
                entity = type
            }
        }
        if(entity == null) {
            warn("Could not find vanilla entity type called '${id}' in string '${string}'")
            return null
        }
        return VanillaMobType(entity)
    } else {
        warn("Unknown mob type '${split[0]}' found for string $string! Please use 'mythic' or 'vanilla'")
    }
    return null
}

fun RavinPlugin.copyResource(parent: File, sourcePath: String, destPath: String) {
    this.getResource(sourcePath)?.let {
        it.use { stream ->
            Files.copy(
                stream,
                File(parent, destPath).toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }
}

fun ConfigurationSection.getMaterialList(path: String): Set<Material> {
    val list = getStringList(path)
    return buildSet {
        for (m in list) {
            parseMaterial(m)?.let { this.add(it) }
        }
    }
}

fun ConfigurationSection.getMaterial(path: String): Material? {
    val material = this.getString(path)
    if (material == null) {
        warn("Could not find option at path '$path' in section '${this.name}'")
        return null
    }
    return parseMaterial(material)
}

fun ConfigurationSection.getSound(path: String): Sound? {
    getString(path)?.let {
        try {
            return Sound.valueOf(it.uppercase())
        } catch (exception: IllegalArgumentException) {
            warn("Could not find sound with ID of '$it'!")
        }
    }
    warn("Could not find option at path '$path' in section '${this.name}'")
    return null
}

fun parseMaterial(string: String): Material? {
    val material = Material.matchMaterial(string)
    if (material == null) {
        warn("Could not find valid material called '$string'. Please fix your config!")
    }
    return material
}

fun ItemStack.getMMOIdentifier(): String {
    return NBTItem.get(this).getString("MMOITEMS_ITEM_ID") ?: ""
}

fun ItemStack.takeDurability(player: Player, amount: Int = 1) {
    val nbt = NBTItem.get(this)
    if (nbt.hasType()) {
        val durability = DurabilityItem(player, nbt)
        if (durability.isValid) {
            durability.decreaseDurability(amount)
            return
        }
    }
    val meta = this.itemMeta
    if (meta is Damageable) {
        meta.damage = meta.damage + amount
        this.itemMeta = meta
    }
}

fun <T> CoroutineScope.compute(context: CoroutineContext = coroutineContext, block: suspend CoroutineScope.() -> T) : Deferred<T> {
    val deferred = CompletableDeferred<T>()
    this.launch(context) {
        deferred.completeWith(runCatching { block.invoke(this) })
    }
    return deferred
}

