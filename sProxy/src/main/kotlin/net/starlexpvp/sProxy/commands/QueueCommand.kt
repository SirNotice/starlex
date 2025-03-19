package net.starlexpvp.sProxy.commands

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.starlexpvp.sProxy.SProxy
import net.starlexpvp.sProxy.queue.QueueManager

class QueueCommand(private val plugin: SProxy, private val queueManager: QueueManager) : SimpleCommand {

    override fun execute(invocation: SimpleCommand.Invocation) {
        val source = invocation.source()
        val args = invocation.arguments()

        if (args.isEmpty()) {
            sendHelpMessage(source)
            return
        }

        when (args[0].lowercase()) {
            "join" -> handleJoin(source, args)
            "leave" -> handleLeave(source)
            "pause" -> handlePause(source, args)
            "unpause" -> handleUnpause(source, args)
            else -> sendHelpMessage(source)
        }
    }

    private fun handleJoin(source: CommandSource, args: Array<String>) {
        if (source !is Player) {
            source.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED))
            return
        }

        if (args.size < 2) {
            source.sendMessage(Component.text("Usage: /queue join <server>").color(NamedTextColor.RED))
            return
        }

        val serverName = args[1]
        val result = queueManager.addToQueue(source, serverName)

        when (result) {
            QueueManager.QueueResult.SUCCESS -> {
                val position = queueManager.getQueuePosition(source)
                val total = queueManager.getQueueSize(serverName)
                source.sendMessage(
                    Component.text("You have joined the queue for $serverName. Position: $position/$total").color(
                        NamedTextColor.GREEN))
            }
            QueueManager.QueueResult.ALREADY_IN_QUEUE -> {
                val position = queueManager.getQueuePosition(source)
                val total = queueManager.getQueueSize(serverName)
                source.sendMessage(
                    Component.text("You are already in the queue for $serverName. Position: $position/$total").color(
                        NamedTextColor.YELLOW))
            }
            QueueManager.QueueResult.SERVER_NOT_FOUND -> {
                source.sendMessage(Component.text("Server '$serverName' not found.").color(NamedTextColor.RED))
            }
            else -> {
                source.sendMessage(Component.text("An error occurred while joining the queue.").color(NamedTextColor.RED))
            }
        }
    }

    private fun handleLeave(source: CommandSource) {
        if (source !is Player) {
            source.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED))
            return
        }

        val result = queueManager.removeFromQueue(source)

        when (result) {
            QueueManager.QueueResult.SUCCESS -> {
                source.sendMessage(Component.text("You have left the queue.").color(NamedTextColor.GREEN))
            }
            QueueManager.QueueResult.NOT_IN_QUEUE -> {
                source.sendMessage(Component.text("You are not in any queue.").color(NamedTextColor.YELLOW))
            }
            else -> {
                source.sendMessage(Component.text("An error occurred while leaving the queue.").color(NamedTextColor.RED))
            }
        }
    }

    private fun handlePause(source: CommandSource, args: Array<String>) {
        if (!source.hasPermission("starlexpvp.command.queue.admin")) {
            source.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED))
            return
        }

        if (args.size < 2) {
            source.sendMessage(Component.text("Usage: /queue pause <server>").color(NamedTextColor.RED))
            return
        }

        val serverName = args[1]
        val result = queueManager.pauseQueue(serverName)

        when (result) {
            QueueManager.QueueResult.SUCCESS -> {
                source.sendMessage(
                    Component.text("Queue for server '$serverName' has been paused.").color(
                        NamedTextColor.GREEN))
            }
            QueueManager.QueueResult.SERVER_NOT_FOUND -> {
                source.sendMessage(Component.text("Server '$serverName' not found.").color(NamedTextColor.RED))
            }
            else -> {
                source.sendMessage(Component.text("An error occurred while pausing the queue.").color(NamedTextColor.RED))
            }
        }
    }

    private fun handleUnpause(source: CommandSource, args: Array<String>) {
        if (!source.hasPermission("starlexpvp.command.queue.admin")) {
            source.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED))
            return
        }

        if (args.size < 2) {
            source.sendMessage(Component.text("Usage: /queue unpause <server>").color(NamedTextColor.RED))
            return
        }

        val serverName = args[1]
        val result = queueManager.unpauseQueue(serverName)

        when (result) {
            QueueManager.QueueResult.SUCCESS -> {
                source.sendMessage(
                    Component.text("Queue for server '$serverName' has been unpaused.").color(
                        NamedTextColor.GREEN))
            }
            QueueManager.QueueResult.SERVER_NOT_FOUND -> {
                source.sendMessage(Component.text("Server '$serverName' not found.").color(NamedTextColor.RED))
            }
            else -> {
                source.sendMessage(Component.text("An error occurred while unpausing the queue.").color(NamedTextColor.RED))
            }
        }
    }

    private fun sendHelpMessage(source: CommandSource) {
        source.sendMessage(Component.text("=== Queue Commands ===").color(NamedTextColor.GOLD))
        source.sendMessage(Component.text("/queue join <server> - Join a server queue").color(NamedTextColor.YELLOW))
        source.sendMessage(Component.text("/queue leave - Leave your current queue").color(NamedTextColor.YELLOW))

        if (source.hasPermission("starlexpvp.command.queue.admin")) {
            source.sendMessage(Component.text("/queue pause <server> - Pause a server queue").color(NamedTextColor.YELLOW))
            source.sendMessage(Component.text("/queue unpause <server> - Unpause a server queue").color(NamedTextColor.YELLOW))
        }
    }

    override fun hasPermission(invocation: SimpleCommand.Invocation): Boolean {
        return true // Basic permission check handled inside the command
    }

    override fun suggest(invocation: SimpleCommand.Invocation): List<String> {
        val source = invocation.source()
        val args = invocation.arguments()

        if (args.isEmpty()) {
            val suggestions = mutableListOf("join", "leave")
            if (source.hasPermission("starlexpvp.command.queue.admin")) {
                suggestions.add("pause")
                suggestions.add("unpause")
            }
            return suggestions
        }

        if (args.size == 1) {
            val subcommand = args[0].lowercase()
            return when {
                "join".startsWith(subcommand) -> listOf("join")
                "leave".startsWith(subcommand) -> listOf("leave")
                "pause".startsWith(subcommand) && source.hasPermission("starlexpvp.command.queue.admin") -> listOf("pause")
                "unpause".startsWith(subcommand) && source.hasPermission("starlexpvp.command.queue.admin") -> listOf("unpause")
                else -> emptyList()
            }
        }

        if (args.size == 2) {
            val subcommand = args[0].lowercase()
            if ((subcommand == "join" || (subcommand == "pause" || subcommand == "unpause") && source.hasPermission("starlexpvp.command.queue.admin"))) {
                // Return list of server names
                return plugin.server.allServers.map { it.serverInfo.name }
            }
        }

        return emptyList()
    }
}