package net.starlexpvp.starlexHub.managers

import net.luckperms.api.LuckPerms
import net.luckperms.api.event.user.UserDataRecalculateEvent
import net.starlexpvp.starlexHub.StarlexHub
import net.starlexpvp.starlexHub.messaging.ProxyMessaging
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ScoreboardManager(private val plugin: StarlexHub) {

    private lateinit var scoreboardConfig: YamlConfiguration
    private lateinit var scoreboardFile: File
    private val playerRankCache = ConcurrentHashMap<UUID, String>()
    private val proxyMessaging = ProxyMessaging(plugin)

    /**
     * Loads the scoreboard configuration from file
     */
    fun loadConfig() {
        scoreboardFile = File(plugin.dataFolder, "scoreboard.yml")
        if (!scoreboardFile.exists()) {
            plugin.saveResource("scoreboard.yml", false)
        }
        scoreboardConfig = YamlConfiguration.loadConfiguration(scoreboardFile)
        // In ScoreboardManager.kt - add to loadConfig() method
        val luckPermsProvider = Bukkit.getServicesManager().getRegistration(LuckPerms::class.java)?.provider
        luckPermsProvider?.eventBus?.subscribe(plugin, UserDataRecalculateEvent::class.java) { event ->
            val player = Bukkit.getPlayer(event.user.uniqueId) ?: return@subscribe
            updateRankCache(player)
            updateScoreboard(player)
        }
    }

    /**
     * Sets up a scoreboard for a player with a slight delay for queue data
     */
    fun setScoreboard(player: Player) {
        if (!scoreboardConfig.getBoolean("enabled", true)) return

        proxyMessaging.requestQueueData(player)

        // Schedule scoreboard update after queue data is received
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            updateScoreboard(player)
        }, 10L)
    }

    // Add a method to update the rank cache
    private fun updateRankCache(player: Player) {
        val luckPermsProvider = Bukkit.getServicesManager().getRegistration(LuckPerms::class.java)?.provider
        val user = luckPermsProvider?.userManager?.getUser(player.uniqueId)
        val rankName = user?.primaryGroup ?: "None"
        playerRankCache[player.uniqueId] = rankName
    }

    fun loadPlayerData(player: Player) {
        updateRankCache(player)
    }

    /**
     * Updates the player's scoreboard dynamically
     */
    fun updateScoreboard(player: Player) {
        if (!scoreboardConfig.getBoolean("enabled", true)) return

        val queueData = proxyMessaging.getQueueData(player)
        val manager = Bukkit.getScoreboardManager() ?: return
        val scoreboard = player.scoreboard ?: manager.newScoreboard

        // Clear old scoreboard objective
        scoreboard.getObjective("hubboard")?.unregister()

        // Create new objective
        val objective = scoreboard.registerNewObjective(
            "hubboard", "dummy",
            ChatColor.translateAlternateColorCodes('&', scoreboardConfig.getString("title") ?: "&d&lStarlex Network")
        )
        objective.displaySlot = DisplaySlot.SIDEBAR

        // Choose lines based on queue status
        val lines = if (queueData.inQueue) scoreboardConfig.getStringList("queue-lines")
        else scoreboardConfig.getStringList("normal-lines")

        // Update scoreboard lines
        var score = lines.size
        for (line in lines) {
            val processedLine = processPlaceholders(player, line)
            objective.getScore(processedLine).score = score--
        }

        player.scoreboard = scoreboard
    }

    /**
     * Processes placeholders for dynamic scoreboard content
     */
    private fun processPlaceholders(player: Player, text: String): String {
        var processed = ChatColor.translateAlternateColorCodes('&', text)

        // Basic placeholders
        processed = processed.replace("%player%", player.name)
        processed = processed.replace("%online%", Bukkit.getOnlinePlayers().size.toString())
        processed = processed.replace("%max_players%", Bukkit.getMaxPlayers().toString())

        // Use cached rank data
        val rankName = playerRankCache.getOrDefault(player.uniqueId, "None")
        processed = processed.replace("%rank%", rankName)

        // Queue placeholders
        val queueData = proxyMessaging.getQueueData(player)
        processed = processed.replace("%queue_position%", if (queueData.position == -1) "N/A" else queueData.position.toString())
        processed = processed.replace("%queue_total%", queueData.total.toString())
        processed = processed.replace("%queue_server%", queueData.server)
        processed = processed.replace("%queue_in_queue%", queueData.inQueue.toString())

        return processed
    }

    /**
     * Removes a player's scoreboard when they leave
     */
    fun removeScoreboard(player: Player) {
        player.scoreboard = Bukkit.getScoreboardManager()?.newScoreboard!!
    }

    /**
     * Starts a repeating scoreboard update task for all players
     */
    fun startScoreboardTask() {
        val updateInterval = scoreboardConfig.getLong("update-interval", 20L)

        Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            for (player in Bukkit.getOnlinePlayers()) {
                proxyMessaging.requestQueueData(player)
                Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                    updateScoreboard(player)
                }, 5L)
            }
        }, updateInterval, updateInterval)
    }

    /**
     * Gets the scoreboard configuration
     */
    fun getScoreboardConfig(): YamlConfiguration {
        return scoreboardConfig
    }
}
