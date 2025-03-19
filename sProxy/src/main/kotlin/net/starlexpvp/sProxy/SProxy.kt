package net.starlexpvp.sProxy

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import net.starlexpvp.sProxy.commands.JoinCommand
import net.starlexpvp.sProxy.commands.QueueCommand
import net.starlexpvp.sProxy.placeholders.QueuePlaceholders
import net.starlexpvp.sProxy.queue.QueueManager
import org.slf4j.Logger

@Plugin(
    id = "sproxy",
    name = "sProxy",
    version = BuildConstants.VERSION
)
class SProxy @Inject constructor(val server: ProxyServer, val logger: Logger) {

    lateinit var queueManager: QueueManager
    lateinit var queuePlaceholders: QueuePlaceholders

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        // Initialize queue manager
        queueManager = QueueManager(this, server)

        // Initialize placeholders
        queuePlaceholders = QueuePlaceholders(queueManager)

        // Register commands
        server.commandManager.register("join", JoinCommand(this, queueManager))
        server.commandManager.register("queue", QueueCommand(this, queueManager))

        logger.info("sProxy has been initialized.")
    }

    @Subscribe
    fun onPlayerDisconnect(event: DisconnectEvent) {
        // Clean up when a player disconnects
        queueManager.handlePlayerDisconnect(event.player)
    }
}