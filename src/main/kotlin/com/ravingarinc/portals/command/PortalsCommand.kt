package com.ravingarinc.portals.command

import com.ravingarinc.api.command.BaseCommand
import com.ravingarinc.api.module.RavinPlugin
import com.ravingarinc.portals.`fun`.PortalHandler
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType

class PortalsCommand(plugin: RavinPlugin) : BaseCommand(plugin, "portals", "portals.admin") {
    init {
        val handler = plugin.getModule(PortalHandler::class.java)

        addOption("get", null, "- Get the current UUID for the current portal key", 1) { sender, args ->
            val uuid = handler.getKey()
            if(uuid == null) {
                sender.sendMessage(Component.text()
                    .content("-- Portal UUID Key --").color(NamedTextColor.DARK_PURPLE)
                    .content("\n")
                    .content("There is no current portal key! Hold an item and use /p set to create one!").color(NamedTextColor.GRAY))
            } else {
                sender.sendMessage(Component.text()
                    .content("-- Portal UUID Key --").color(NamedTextColor.DARK_PURPLE)
                    .content("\n")
                    .content(uuid.toString()).color(NamedTextColor.LIGHT_PURPLE))
            }
            return@addOption true
        }
        addOption("set", null, "- Creates a new portal key if one does not already exist and sets your current item in hand to contain that key.", 1) { sender, args ->
            if(sender is Player) {
                val item = sender.inventory.itemInMainHand
                if(item.type.isAir) {
                    sender.sendMessage("${ChatColor.RED}You cannot assign a key value to nothing! Please hold a valid item.")
                    return@addOption true
                }
                val uuid = handler.getOrCreateKey()
                val meta = item.itemMeta
                meta.persistentDataContainer.set(handler.namespace, PersistentDataType.STRING, uuid.toString())
                item.itemMeta = meta
                sender.sendMessage("${ChatColor.GREEN}Successfully added unique key to your item in hand!")
            } else {
                sender.sendMessage("${ChatColor.RED}This command can only be used by a player!")
            }
            return@addOption true
        }

        addOption("reset", null, "- Reset the current portal key, any existing portal keys will become invalid.", 1) { sender, args ->
            handler.resetKey()
            sender.sendMessage("${ChatColor.GREEN}Successfully reset portal key. Use /portals create to create a new one!")
            return@addOption true
        }

        addOption("reload", null, "- Reload from configuration.", 1) { sender, args ->
            plugin.reload()
            sender.sendMessage("${ChatColor.GREEN}Successfully reloaded plugin!");
            return@addOption true
        }

        addHelpOption(ChatColor.DARK_PURPLE, ChatColor.LIGHT_PURPLE)
    }
}