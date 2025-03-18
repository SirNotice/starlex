package net.starlexpvp.starlexHub

import net.starlexpvp.starlexHub.managers.MOTDManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin

class StarlexHub : JavaPlugin() {

    private lateinit var motdManager: MOTDManager

    override fun onEnable() {
        // Ensure the plugin data folder exists
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        // Initialize the MOTD Manager
        motdManager = MOTDManager(this)
        motdManager.loadConfig()

        // Register the PlayerJoinListener
        server.pluginManager.registerEvents(PlayerJoinListener(motdManager), this)
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}

class PlayerJoinListener(private val motdManager: MOTDManager) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        motdManager.sendMOTD(player)
    }
}