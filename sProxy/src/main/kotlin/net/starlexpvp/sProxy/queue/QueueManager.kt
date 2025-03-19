package net.starlexpvp.sProxy.queue

import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import net.starlexpvp.sProxy.SProxy
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit


class QueueManager(private val plugin: SProxy, private val server: ProxyServer) {

    // Map of server name to queue of players
    private val queues = ConcurrentHashMap<String, ConcurrentLinkedQueue<UUID>>()

    // Map of server name to paused status
    private val pausedQueues = ConcurrentHashMap<String, Boolean>()

    // Map of player UUID to queue they're in
    private val playerQueueMap = ConcurrentHashMap<UUID, String>()

    // Initialize the queue processor
    init {
        // Populate queues for all registered servers
        for (registeredServer in server.allServers) {
            val serverName = registeredServer.serverInfo.name
            queues[serverName] = ConcurrentLinkedQueue()
            pausedQueues[serverName] = false
        }

        // Schedule the queue processor task
        server.scheduler
            .buildTask(plugin, Runnable { processQueues() }) // Explicitly wrap in Runnable
            .delay(1L, TimeUnit.SECONDS) // Optional: initial delay
            .repeat(1L, TimeUnit.SECONDS) // Repeat every second
            .schedule()
    }

    /**
     * Add a player to a queue
     */
    fun addToQueue(player: Player, serverName: String): QueueResult {
        // Check if server exists
        val targetServer = server.getServer(serverName)
        if (targetServer.isEmpty) {
            return QueueResult.SERVER_NOT_FOUND
        }

        // Check if player is already in a queue
        val currentQueue = playerQueueMap[player.uniqueId]
        if (currentQueue != null) {
            if (currentQueue == serverName) {
                return QueueResult.ALREADY_IN_QUEUE
            } else {
                // Remove them from their current queue
                queues[currentQueue]?.remove(player.uniqueId)
                playerQueueMap.remove(player.uniqueId)
            }
        }

        // Add player to queue
        queues[serverName]?.add(player.uniqueId)
        playerQueueMap[player.uniqueId] = serverName

        return QueueResult.SUCCESS
    }

    /**
     * Remove a player from any queue they may be in
     */
    fun removeFromQueue(player: Player): QueueResult {
        val serverName = playerQueueMap[player.uniqueId] ?: return QueueResult.NOT_IN_QUEUE

        queues[serverName]?.remove(player.uniqueId)
        playerQueueMap.remove(player.uniqueId)

        return QueueResult.SUCCESS
    }

    /**
     * Get a player's position in their current queue
     */
    fun getQueuePosition(player: Player): Int {
        val serverName = playerQueueMap[player.uniqueId] ?: return -1
        val queue = queues[serverName] ?: return -1

        var position = 1
        for (uuid in queue) {
            if (uuid == player.uniqueId) {
                return position
            }
            position++
        }

        return -1
    }

    /**
     * Get the total size of a server's queue
     */
    fun getQueueSize(serverName: String): Int {
        return queues[serverName]?.size ?: 0
    }

    /**
     * Get the server a player is queued for
     */
    fun getQueuedServer(player: Player): String? {
        return playerQueueMap[player.uniqueId]
    }

    /**
     * Pause a server's queue
     */
    fun pauseQueue(serverName: String): QueueResult {
        if (!queues.containsKey(serverName)) {
            return QueueResult.SERVER_NOT_FOUND
        }

        pausedQueues[serverName] = true
        return QueueResult.SUCCESS
    }

    /**
     * Unpause a server's queue
     */
    fun unpauseQueue(serverName: String): QueueResult {
        if (!queues.containsKey(serverName)) {
            return QueueResult.SERVER_NOT_FOUND
        }

        pausedQueues[serverName] = false
        return QueueResult.SUCCESS
    }

    /**
     * Check if a queue is paused
     */
    fun isQueuePaused(serverName: String): Boolean {
        return pausedQueues[serverName] ?: false
    }

    /**
     * Process all queues, sending players to their destinations if possible
     */
    private fun processQueues() {
        for ((serverName, queue) in queues) {
            // Skip processing if the queue is paused
            if (pausedQueues[serverName] == true) {
                continue
            }

            // Skip empty queues
            if (queue.isEmpty()) {
                continue
            }

            val targetServer = server.getServer(serverName).orElse(null) ?: continue

            // Try to send the first player in the queue
            val playerId = queue.peek() ?: continue
            val player = server.getPlayer(playerId).orElse(null)

            // Remove player from queue if they're offline
            if (player == null) {
                queue.poll()
                playerQueueMap.remove(playerId)
                continue
            }

            // Attempt to connect player to server
            player.createConnectionRequest(targetServer).connectWithIndication().thenAccept { success ->
                if (success) {
                    // Successfully connected, remove from queue
                    queue.poll()
                    playerQueueMap.remove(playerId)
                    player.sendMessage(
                        net.kyori.adventure.text.Component.text("You have been connected to $serverName.")
                    )
                }
            }
        }
    }

    /**
     * Clean up when a player disconnects
     */
    fun handlePlayerDisconnect(player: Player) {
        removeFromQueue(player)
    }

    /**
     * Possible queue operation results
     */
    enum class QueueResult {
        SUCCESS,
        ALREADY_IN_QUEUE,
        NOT_IN_QUEUE,
        SERVER_NOT_FOUND
    }
}