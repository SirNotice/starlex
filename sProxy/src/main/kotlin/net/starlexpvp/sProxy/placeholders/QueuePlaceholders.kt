package net.starlexpvp.sProxy.placeholders

import com.velocitypowered.api.proxy.Player
import net.starlexpvp.sProxy.queue.QueueManager

class QueuePlaceholders(private val queueManager: QueueManager) {

    /**
     * Get a player's position in their queue
     * Format: %queue_position%
     */
    fun getPosition(player: Player): String {
        val position = queueManager.getQueuePosition(player)
        return if (position == -1) "Not in queue" else position.toString()
    }

    /**
     * Get the total size of the queue the player is in
     * Format: %queue_total%
     */
    fun getTotal(player: Player): String {
        val serverName = queueManager.getQueuedServer(player) ?: return "0"
        return queueManager.getQueueSize(serverName).toString()
    }

    /**
     * Get the server the player is queued for
     * Format: %queue_server%
     */
    fun getServer(player: Player): String {
        return queueManager.getQueuedServer(player) ?: "None"
    }

    /**
     * Check if the player is in a queue
     * Format: %queue_in_queue%
     */
    fun isInQueue(player: Player): String {
        return (queueManager.getQueuedServer(player) != null).toString()
    }
}