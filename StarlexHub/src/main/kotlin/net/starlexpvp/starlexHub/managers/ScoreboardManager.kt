package net.starlexpvp.starlexHub.managers

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
import java.util.HashMap
import java.util.UUID

class ScoreboardManager(private val plugin: StarlexHub) {

    private lateinit var scoreboardConfig: YamlConfiguration
    private lateinit var scoreboardFile: File
    private val playerScoreboards = HashMap<UUID, Scoreboard>()
    private val proxyMessaging = ProxyMessaging(plugin)

    /**
     * Loads the scoreboard configuration from file
     */
    fun loadConfig() {
        // Load or create the scoreboard.yml file
        scoreboardFile = File(plugin.dataFolder, "scoreboard.yml")
        if (!scoreboardFile.exists()) {
            plugin.saveResource("scoreboard.yml", false)
        }
        scoreboardConfig = YamlConfiguration.loadConfiguration(scoreboardFile)
    }

    /**
     * Creates and sets a scoreboard for a player
     */
    fun setScoreboard(player: Player) {
        if (!scoreboardConfig.getBoolean("enabled", true)) {
            return
        }

        // Request queue data before setting the scoreboard
        proxyMessaging.requestQueueData(player)

        val manager = Bukkit.getScoreboardManager()
        val scoreboard = manager?.newScoreboard ?: return

        val objective = scoreboard.registerNewObjective(
            "hubboard",
            "dummy",
            ChatColor.translateAlternateColorCodes('&', scoreboardConfig.getString("title") ?: "&d&lStarlex Network")
        )


        objective.displaySlot = DisplaySlot.SIDEBAR

        // Set the scoreboard lines
        val lines = scoreboardConfig.getStringList("lines")
        var score = lines.size

        for (line in lines) {
            val processedLine = processPlaceholders(player, line)
            objective.getScore(processedLine).score = score
            score--
        }

        player.scoreboard = scoreboard
        playerScoreboards[player.uniqueId] = scoreboard
    }

    /**
     * Updates the scoreboard for a player
     */
    fun updateScoreboard(player: Player) {
        if (!scoreboardConfig.getBoolean("enabled", true)) {
            return
        }

        // Request fresh queue data
        proxyMessaging.requestQueueData(player)

        // If player doesn't have a scoreboard yet, set one
        if (!playerScoreboards.containsKey(player.uniqueId)) {
            setScoreboard(player)
            return
        }

        // Get the player's existing scoreboard
        val scoreboard = playerScoreboards[player.uniqueId] ?: return

        // Clear existing objective
        val oldObjective = scoreboard.getObjective("hubboard")
        oldObjective?.unregister()

        // Create new objective
        val objective = scoreboard.registerNewObjective(
            "hubboard",
            "dummy",
            ChatColor.translateAlternateColorCodes('&', scoreboardConfig.getString("title") ?: "&d&lStarlex Network")
        )


        objective.displaySlot = DisplaySlot.SIDEBAR

        // Update the scoreboard lines
        val lines = scoreboardConfig.getStringList("lines")
        var score = lines.size

        for (line in lines) {
            val processedLine = processPlaceholders(player, line)
            objective.getScore(processedLine).score = score
            score--
        }

        player.scoreboard = scoreboard
    }

    /**
     * Process placeholders in a string
     */
    private fun processPlaceholders(player: Player, text: String): String {
        var processed = ChatColor.translateAlternateColorCodes('&', text)

        // Basic placeholders
        processed = processed.replace("%player%", player.name)
        processed = processed.replace("%online%", Bukkit.getOnlinePlayers().size.toString())
        processed = processed.replace("%max_players%", Bukkit.getMaxPlayers().toString())

        // Queue placeholders
        val queueData = proxyMessaging.getQueueData(player)
        processed = processed.replace("%queue_position%", if (queueData.position == -1) "N/A" else queueData.position.toString())
        processed = processed.replace("%queue_total%", queueData.total.toString())
        processed = processed.replace("%queue_server%", queueData.server)
        processed = processed.replace("%queue_in_queue%", queueData.inQueue.toString())

        return processed
    }

    /**
     * Removes a player's scoreboard
     */
    fun removeScoreboard(player: Player) {
        playerScoreboards.remove(player.uniqueId)
    }

    /**
     * Start the scoreboard update task
     */
    fun startScoreboardTask() {
        val updateInterval = scoreboardConfig.getLong("update-interval", 20L)
        plugin.server.scheduler.scheduleSyncRepeatingTask(plugin, {
            for (player in Bukkit.getOnlinePlayers()) {
                updateScoreboard(player)
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