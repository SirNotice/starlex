package net.starlexpvp.starlexHub

import net.starlexpvp.starlexHub.managers.MOTDManager
import net.starlexpvp.starlexHub.managers.ScoreboardManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin

class StarlexHub : JavaPlugin() {

    private lateinit var motdManager: MOTDManager
    private lateinit var scoreboardManager: ScoreboardManager

    override fun onEnable() {
        // Ensure the plugin data folder exists
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        // Initialize the MOTD Manager
        motdManager = MOTDManager(this)
        motdManager.loadConfig()

        // Initialize the Scoreboard Manager
        scoreboardManager = ScoreboardManager(this)
        scoreboardManager.loadConfig()
        scoreboardManager.startScoreboardTask()

        // Register event listeners
        server.pluginManager.registerEvents(HubListener(motdManager, scoreboardManager), this)
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}

class HubListener(
    private val motdManager: MOTDManager,
    private val scoreboardManager: ScoreboardManager
) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        motdManager.sendMOTD(player)
        scoreboardManager.setScoreboard(player)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        scoreboardManager.removeScoreboard(player)
    }
}