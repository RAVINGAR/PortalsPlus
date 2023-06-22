package com.ravingarinc.portals.`fun`

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import com.ravingarinc.api.I
import com.ravingarinc.api.module.RavinPlugin
import com.ravingarinc.api.module.SuspendingModuleListener
import com.ravingarinc.portals.api.*
import com.ravingarinc.portals.config.ConfigManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bukkit.*
import org.bukkit.Particle.DustOptions
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.BlockState
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.world.PortalCreateEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.BlockVector
import org.bukkit.util.Vector
import java.lang.IllegalArgumentException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import kotlin.collections.ArrayList
import kotlin.random.Random

class PortalHandler(plugin: RavinPlugin) : SuspendingModuleListener(PortalHandler::class.java, plugin, ConfigManager::class.java) {
    private lateinit var configManager: ConfigManager
    private var uuid: UUID? = null
    val namespace = NamespacedKey(plugin, "portal_uuid")

    private var consumeKey = false
    private var eventChance: Double = 1.0
    private var mobSpawnAmount: IntRange = 0..0
    private lateinit var mobList: WeightedCollection<MobType>

    val sideDirections = arrayOf(BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH)

    val litBlocks : MutableSet<Block> = ConcurrentHashMap.newKeySet()
    override suspend fun suspendLoad() {
        configManager = plugin.getModule(ConfigManager::class.java)
        uuid = configManager.getKey()

        consumeKey = configManager.config.config.getBoolean("key.consume-on-use")
        mobList = WeightedCollection()
        configManager.config.consume("event") {
            eventChance = it.getPercentage("event-chance")
            mobSpawnAmount = it.getRange("mob-spawn-amount")
            it.getStringList("mobs").forEach { line -> parseMob(line)?.let { result ->
                mobList.add(result.first, result.second)
            } }
        }
        super.suspendLoad()
    }

    override suspend fun suspendCancel() {
        super.suspendCancel()
        litBlocks.clear()
    }

    fun getOrCreateKey() : UUID {
        uuid?.let { return it }
        val newId = UUID.randomUUID()
        uuid = newId
        configManager.saveKey(newId)
        return newId
    }

    fun getKey() : UUID? {
        return uuid
    }

    fun resetKey() {
        uuid = null
        configManager.resetKey()
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return
        if(block.type != Material.OBSIDIAN) return
        val item = event.item ?: return
        if(!item.hasItemMeta()) return
        val meta = item.itemMeta
        val value = meta.persistentDataContainer[namespace, PersistentDataType.STRING] ?: return
        try {
            val uuid = UUID.fromString(value)
            if(uuid == this.uuid) {
                plugin.launch {
                    val b = block.getRelative(event.blockFace)
                    if(b.type.isAir) {
                        val hand = event.hand
                        if(consumeKey && hand != null) {
                            if(item.amount == 1) {
                                event.player.inventory.setItem(hand, null)
                            } else {
                                item.amount = item.amount - 1
                                event.player.inventory.setItem(hand, item)
                            }
                        }
                        litBlocks.add(b)
                        b.type = Material.FIRE
                        plugin.launch {
                            delay(1.ticks)
                            if(litBlocks.remove(b)) b.type = Material.AIR
                        }
                    }
                }
            }
        } catch(ignored: IllegalArgumentException) {}
    }

    @EventHandler
    fun onPortalCreate(event: PortalCreateEvent) {
        if(event.reason != PortalCreateEvent.CreateReason.FIRE) return
        val world = event.world
        if(world.environment != World.Environment.NORMAL && world.environment != World.Environment.CUSTOM) return

        val states = event.blocks
        for(state in states) {
            if(litBlocks.remove(state.block)) {
                handleCreationEvent(world, states)
                return
            }
        }
        event.isCancelled = true
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun handleCreationEvent(world: World, states: List<BlockState>) = plugin.launch {
        if(Random.nextDouble() > eventChance) return@launch
        var lowest = 320
        var highest = -96
        var xSum = 0
        var zSum = 0
        var i = 0
        for(block in states) {
            xSum += block.x
            zSum += block.z
            if(block.y > highest) {
                highest = block.y
            }
            if(block.y < lowest) {
                lowest = block.y
            }
            i++
        }

        val x = xSum / i
        val z = zSum / i
        val amount = mobSpawnAmount.random()

        val locations = this.compute {
            val list = ArrayList<Location>()
            for(index in 0 until amount) {
                val loc = findValidSpawnLocation(world, x, lowest, highest, z, 8) ?: continue
                list.add(loc)
            }
            return@compute list
        }

        val average = world.getNearbyPlayers(
            Location(world, x.toDouble(), lowest.toDouble(), z.toDouble()), 8.0)
            .map { it.level }.average().toInt()

        world.playSound(Location(world, x.toDouble(), lowest.toDouble(), z.toDouble()), Sound.ITEM_TRIDENT_RETURN, 1.5F, 0.5F)
        for(loc in locations.await()) {
            val particleLoc = loc.clone().add(0.0, 1.0, 0.0)
            world.spawnParticle(Particle.REDSTONE, particleLoc, 30, 0.4, 0.75, 0.4, DustOptions(Color.PURPLE, 1F))
            world.spawnParticle(Particle.PORTAL, particleLoc, 45, 0.4, 0.75, 0.4)
            world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_2, 0.7F, 1.0F)
            world.playSound(loc, Sound.ENTITY_GHAST_WARN, 0.6F, 0.1F)
            delay(Random.nextInt(2, 5).ticks)
            world.spawnParticle(Particle.REDSTONE, particleLoc, 30, 0.4, 0.75, 0.4, DustOptions(Color.PURPLE, 1F))
            world.spawnParticle(Particle.PORTAL, particleLoc, 45, 0.4, 0.75, 0.4)
        }
        delay(5.ticks)
        for(loc in locations.await()) {
            val particleLoc = loc.clone().add(0.0, 1.0, 0.0)
            world.spawnParticle(Particle.REDSTONE, particleLoc, 35, 0.6, 0.5, 0.6, DustOptions(Color.PURPLE, 1F))
            world.spawnParticle(Particle.SMOKE_NORMAL, particleLoc, 15, 0.5, 0.5, 0.5, 0.0)
            world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT , 1.0F, 1.0F)
            world.playSound(loc, Sound.ENTITY_GHAST_HURT , 0.5F, 0.5F)
            mobList.random().spawn(average, BlockVector(loc.x, loc.y, loc.z), world)
            delay(Random.nextInt(2, 5).ticks)
        }

    }

    private fun findValidSpawnLocation(world: World, x: Int, lowY: Int, highY: Int, z: Int, radius: Int) : Location? {
        for(i in 0 until 8) {
            val randX = x + Random.nextInt(radius * 2) - radius
            val randZ = z + Random.nextInt(radius * 2) - radius

            val result = world.rayTraceBlocks(
                Location(world, randX.toDouble() + 0.5, highY.toDouble() - 1, randZ.toDouble() + 0.5),
                Vector(0.0, -1.0, 0.0),
                (highY - lowY) + 4.0,
                FluidCollisionMode.NEVER,
                true) ?: continue
            result.hitBlock?.let {
                return it.location.add(0.5, 1.0, 0.5)
            }
        }
        return null
    }
}