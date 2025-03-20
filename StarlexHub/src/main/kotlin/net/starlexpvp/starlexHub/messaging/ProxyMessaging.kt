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

                val newQueueData = QueueData(inQueue, server, position, total)
                queueData[uuid] = newQueueData

                val targetPlayer = Bukkit.getPlayer(uuid)
                if (targetPlayer != null && targetPlayer.isOnline) {
                    plugin.logger.info("Directly updating scoreboard for ${targetPlayer.name} with inQueue=$inQueue")
                    plugin.scoreboardManager.updateScoreboardWithQueueData(targetPlayer, newQueueData)
                }
            }
        } catch (e: Exception) {
            plugin.logger.severe("Error processing plugin message: ${e.message}")
            e.printStackTrace()
        }
    }

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

    fun getQueueData(player: Player): QueueData {
        val data = queueData.getOrDefault(player.uniqueId, QueueData(false, "None", -1, 0))
        plugin.logger.info("Fetched queue data for ${player.name}: inQueue=${data.inQueue}")
        return data
    }

    data class QueueData(
        val inQueue: Boolean,
        val server: String,
        val position: Int,
        val total: Int
    )
}