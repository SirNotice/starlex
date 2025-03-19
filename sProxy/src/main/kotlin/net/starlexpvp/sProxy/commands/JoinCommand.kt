package net.starlexpvp.sProxy.commands

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.permission.Tristate
import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.starlexpvp.sProxy.SProxy
import net.starlexpvp.sProxy.queue.QueueManager

class JoinCommand(private val plugin: SProxy, private val queueManager: QueueManager) : SimpleCommand {

    override fun execute(invocation: SimpleCommand.Invocation) {
        val source = invocation.source()
        val args = invocation.arguments()

        if (source !is Player) {
            source.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED))
            return
        }

        if (args.isEmpty()) {
            source.sendMessage(Component.text("Usage: /join <server>").color(NamedTextColor.RED))
            return
        }

        val serverName = args[0]
        val result = queueManager.addToQueue(source, serverName)

        when (result) {
            QueueManager.QueueResult.SUCCESS -> {
                val position = queueManager.getQueuePosition(source)
                val total = queueManager.getQueueSize(serverName)
                source.sendMessage(Component.text("You have joined the queue for $serverName. Position: $position/$total").color(NamedTextColor.GREEN))
            }
            QueueManager.QueueResult.ALREADY_IN_QUEUE -> {
                val position = queueManager.getQueuePosition(source)
                val total = queueManager.getQueueSize(serverName)
                source.sendMessage(Component.text("You are already in the queue for $serverName. Position: $position/$total").color(NamedTextColor.YELLOW))
            }
            QueueManager.QueueResult.SERVER_NOT_FOUND -> {
                source.sendMessage(Component.text("Server '$serverName' not found.").color(NamedTextColor.RED))
            }
            else -> {
                source.sendMessage(Component.text("An error occurred while joining the queue.").color(NamedTextColor.RED))
            }
        }
    }

    override fun hasPermission(invocation: SimpleCommand.Invocation): Boolean {
        return invocation.source().hasPermission("starlexpvp.command.join")
    }
}