package com.ravingarinc.portals

import com.ravingarinc.api.module.RavinPluginKotlin
import com.ravingarinc.portals.command.PortalsCommand
import com.ravingarinc.portals.config.ConfigManager
import com.ravingarinc.portals.`fun`.PortalHandler

class PortalsPlus : RavinPluginKotlin() {

    override fun loadModules() {
        addModule(ConfigManager::class.java)
        addModule(PortalHandler::class.java)
    }

    override fun loadCommands() {
        PortalsCommand(this).register()
    }

}