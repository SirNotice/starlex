package net.starlexpvp.starlexHub.messaging

import net.starlexpvp.starlexHub.StarlexHub
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ProxyMessaging(private val plugin: StarlexHub) : PluginMessageListener {

    private val CHANNEL = "starlex:queue"
    private val queueData = ConcurrentHashMap<UUID, QueueData>()

    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        if (channel != CHANNEL) return

        try {
            val input = DataInputStream(ByteArrayInputStream(message))
            val subChannel = input.readUTF()

            plugin.logger.info("Received message on channel $channel, subChannel: $subChannel")

            if (subChannel == "QueueData") {
                val uuid = UUID.fromString(input.readUTF())
                val inQueue = input.readBoolean()
                val server = if (inQueue) input.readUTF() else "None"
                val position = if (inQueue) input.readInt() else -1
                val total = if (inQueue) input.readInt() else 0

                plugin.logger.info("Queue data for $uuid: inQueue=$inQueue, server=$server, position=$position, total=$total")

                queueData[uuid] = QueueData(inQueue, server, position, total)

                // Immediately update this player's scoreboard
                val targetPlayer = Bukkit.getPlayer(uuid)
                if (targetPlayer != null && targetPlayer.isOnline) {
                    // Get scoreboard manager and update scoreboard
                    val manager = plugin as? StarlexHub
                    manager?.let {
                        Bukkit.getScheduler().runTask(plugin, Runnable {
                            it.scoreboardManager.updateScoreboard(targetPlayer)
                        })
                    }
                }
            }
        } catch (e: Exception) {
            plugin.logger.severe("Error processing plugin message: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Request queue data for a player
     */
    fun requestQueueData(player: Player) {
        val output = ByteArrayOutputStream()
        val out = DataOutputStream(output)

        try {
            out.writeUTF("RequestQueueData")
            out.writeUTF(player.uniqueId.toString())

            player.sendPluginMessage(plugin, CHANNEL, output.toByteArray())
            plugin.logger.info("Sent queue data request for player: ${player.name}")
        } catch (e: Exception) {
            plugin.logger.warning("Failed to send queue data request: ${e.message}")
        }
    }

    /**
     * Get queue data for a player
     */
    fun getQueueData(player: Player): QueueData {
        return queueData.getOrDefault(player.uniqueId, QueueData(false, "None", -1, 0))
    }

    data class QueueData(
        val inQueue: Boolean,
        val server: String,
        val position: Int,
        val total: Int
    )
}