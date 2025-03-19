package net.starlexpvp.sProxy.messaging

import com.google.common.io.ByteArrayDataInput
import com.google.common.io.ByteArrayDataOutput
import com.google.common.io.ByteStreams
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PluginMessageEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.messages.ChannelIdentifier
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import net.starlexpvp.sProxy.SProxy
import net.starlexpvp.sProxy.placeholders.QueuePlaceholders
import net.starlexpvp.sProxy.queue.QueueManager
import java.util.UUID

class ProxyMessagingHandler(
    private val plugin: SProxy,
    private val server: ProxyServer,
    private val queueManager: QueueManager,
    private val queuePlaceholders: QueuePlaceholders
) {

    private val CHANNEL_ID: ChannelIdentifier = MinecraftChannelIdentifier.from("starlex:queue")

    init {
        server.channelRegistrar.register(CHANNEL_ID)
    }

    @Subscribe
    fun onPluginMessage(event: PluginMessageEvent) {
        if (event.identifier != CHANNEL_ID) {
            return
        }

        // Prevent server -> server or player -> proxy communication
        if (event.source !is Player || event.target !is ProxyServer) {
            return
        }

        val sender = event.source as Player
        val data: ByteArrayDataInput = ByteStreams.newDataInput(event.data)
        val subChannel = data.readUTF()

        if (subChannel.equals("RequestQueueData")) {
            val playerUUID = UUID.fromString(data.readUTF())
            val targetPlayer = server.getPlayer(playerUUID).orElse(null) ?: return

            val isInQueue = queueManager.getQueuedServer(targetPlayer) != null
            val serverName = queueManager.getQueuedServer(targetPlayer) ?: "None"
            val position = queueManager.getQueuePosition(targetPlayer)
            val total = if (serverName != "None") queueManager.getQueueSize(serverName) else 0

            // Send response back to the server
            val out: ByteArrayDataOutput = ByteStreams.newDataOutput()
            out.writeUTF("QueueData")
            out.writeUTF(playerUUID.toString())
            out.writeBoolean(isInQueue)

            if (isInQueue) {
                out.writeUTF(serverName)
                out.writeInt(position)
                out.writeInt(total)
            }

            // Forward the message to the player's current server
            sender.currentServer.ifPresent { server ->
                server.sendPluginMessage(CHANNEL_ID, out.toByteArray())
            }
        }
    }
}