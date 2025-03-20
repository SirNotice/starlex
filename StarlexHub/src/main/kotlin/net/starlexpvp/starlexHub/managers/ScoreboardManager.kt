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
import java.util.concurrent.locks.ReentrantLock

class ScoreboardManager(private val plugin: StarlexHub) {

    private lateinit var scoreboardConfig: YamlConfiguration
    private lateinit var scoreboardFile: File
    private val playerRankCache = ConcurrentHashMap<UUID, String>()
    private val playerScoreboards = ConcurrentHashMap<UUID, Scoreboard>()
    private val updateLocks = ConcurrentHashMap<UUID, ReentrantLock>()
    private val currentLines = ConcurrentHashMap<UUID, MutableList<String>>()
    private val proxyMessaging = ProxyMessaging(plugin)

    fun loadConfig() {
        scoreboardFile = File(plugin.dataFolder, "scoreboard.yml")
        if (!scoreboardFile.exists()) {
            plugin.saveResource("scoreboard.yml", false)
        }
        scoreboardConfig = YamlConfiguration.loadConfiguration(scoreboardFile)

        val luckPermsProvider = Bukkit.getServicesManager().getRegistration(LuckPerms::class.java)?.provider
        luckPermsProvider?.eventBus?.subscribe(plugin, UserDataRecalculateEvent::class.java) { event ->
            val player = Bukkit.getPlayer(event.user.uniqueId) ?: return@subscribe
            updateRankCache(player)
            updateScoreboard(player)
        }
    }

    fun setScoreboard(player: Player) {
        if (!scoreboardConfig.getBoolean("enabled", true)) return

        val scoreboard = Bukkit.getScoreboardManager()?.newScoreboard ?: return
        playerScoreboards[player.uniqueId] = scoreboard
        player.scoreboard = scoreboard
        updateLocks[player.uniqueId] = ReentrantLock()
        currentLines[player.uniqueId] = mutableListOf()

        loadPlayerData(player)
        updateScoreboard(player)
    }

    fun loadPlayerData(player: Player) {
        updateRankCache(player)
        proxyMessaging.requestQueueData(player)
    }

    private fun updateRankCache(player: Player) {
        val luckPermsProvider = Bukkit.getServicesManager().getRegistration(LuckPerms::class.java)?.provider
        val user = luckPermsProvider?.userManager?.getUser(player.uniqueId)
        val rankName = user?.primaryGroup ?: "None"
        playerRankCache[player.uniqueId] = rankName
        plugin.logger.info("Updated rank for ${player.name} to $rankName")
    }

    fun updateScoreboard(player: Player) {
        val queueData = proxyMessaging.getQueueData(player)
        updateScoreboardWithQueueData(player, queueData)
    }

    fun updateScoreboardWithQueueData(player: Player, queueData: ProxyMessaging.QueueData) {
        if (!scoreboardConfig.getBoolean("enabled", true)) return

        val scoreboard = playerScoreboards[player.uniqueId] ?: return
        val lock = updateLocks[player.uniqueId] ?: return

        lock.lock()
        try {
            plugin.logger.info("Updating scoreboard for ${player.name}, inQueue: ${queueData.inQueue}, server: ${queueData.server}, position: ${queueData.position}, total: ${queueData.total}")

            var objective = scoreboard.getObjective("hubboard")
            if (objective == null) {
                objective = scoreboard.registerNewObjective(
                    "hubboard", "dummy",
                    ChatColor.translateAlternateColorCodes('&', scoreboardConfig.getString("title") ?: "&d&lStarlex Network")
                )
                objective.displaySlot = DisplaySlot.SIDEBAR
            }

            // Get the lines to display
            val lines = if (queueData.inQueue) scoreboardConfig.getStringList("queue-lines")
            else scoreboardConfig.getStringList("normal-lines")

            // Process new lines with placeholders
            val newLines = lines.mapIndexed { index, line ->
                if (line == "<blank>") {
                    // Generate a blank line with spaces based on the index
                    "${ChatColor.GRAY}${String(CharArray(index + 1) { ' ' })}"
                } else {
                    val processed = processPlaceholders(player, line, queueData)
                    if (processed == ChatColor.GRAY.toString()) {
                        "${ChatColor.GRAY}${String(CharArray(index + 1) { ' ' })}"
                    } else {
                        processed
                    }
                }
            }.toMutableList()

            // Get current lines for this player
            val oldLines = currentLines.getOrDefault(player.uniqueId, mutableListOf())

            // Force a full update if the number of lines has changed
            if (oldLines.size != newLines.size) {
                plugin.logger.info("Line count changed from ${oldLines.size} to ${newLines.size}, forcing full update for ${player.name}")
                scoreboard.entries.forEach { scoreboard.resetScores(it) }
                for (i in newLines.indices) {
                    objective.getScore(newLines[i]).score = newLines.size - i
                }
            } else {
                // Update only the lines that have changed
                for (i in newLines.indices) {
                    val oldLine = if (i < oldLines.size) oldLines[i] else null
                    val newLine = newLines[i]

                    if (oldLine != newLine) {
                        if (oldLine != null) {
                            scoreboard.resetScores(oldLine)
                        }
                        objective.getScore(newLine).score = newLines.size - i
                    }
                }
            }

            // Update the current lines
            currentLines[player.uniqueId] = newLines
        } finally {
            lock.unlock()
        }
    }

    private fun processPlaceholders(player: Player, text: String, queueData: ProxyMessaging.QueueData): String {
        var processed = ChatColor.translateAlternateColorCodes('&', text)
        processed = processed.replace("%player%", player.name)
        processed = processed.replace("%online%", Bukkit.getOnlinePlayers().size.toString())
        processed = processed.replace("%max_players%", Bukkit.getMaxPlayers().toString())
        val rankName = playerRankCache.getOrDefault(player.uniqueId, "None")
        processed = processed.replace("%rank%", rankName)
        processed = processed.replace("%queue_position%", if (queueData.position == -1) "N/A" else queueData.position.toString())
        processed = processed.replace("%queue_total%", queueData.total.toString())
        processed = processed.replace("%queue_server%", queueData.server)
        processed = processed.replace("%queue_in_queue%", queueData.inQueue.toString())
        return processed
    }

    fun removeScoreboard(player: Player) {
        playerScoreboards.remove(player.uniqueId)
        updateLocks.remove(player.uniqueId)
        currentLines.remove(player.uniqueId)
        player.scoreboard = Bukkit.getScoreboardManager()?.newScoreboard ?: return
    }

    fun startScoreboardTask() {
        val updateInterval = scoreboardConfig.getLong("update-interval", 20L)
        Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            val onlinePlayers = Bukkit.getOnlinePlayers()
            if (onlinePlayers.isNotEmpty()) {
                onlinePlayers.forEach { player ->
                    proxyMessaging.requestQueueData(player)
                }
            }
        }, updateInterval, updateInterval)
    }

    fun getScoreboardConfig(): YamlConfiguration = scoreboardConfig
}