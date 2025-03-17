package net.starlexpvp.starlexHub

import org.bukkit.ChatColor
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class StarlexHub : JavaPlugin() {

    private lateinit var messagesConfig: YamlConfiguration
    private lateinit var messagesFile: File

    override fun onEnable() {
        // Ensure the plugin data folder exists
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        // Load or create the messages.yml file
        messagesFile = File(dataFolder, "messages.yml")
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false) // Save the default messages.yml from resources
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile)

        // Register the PlayerJoinListener
        server.pluginManager.registerEvents(PlayerJoinListener(this), this)
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    fun getMessagesConfig(): YamlConfiguration {
        return messagesConfig
    }
}

class PlayerJoinListener(private val plugin: StarlexHub) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        // Load the MOTD from messages.yml
        val motdLines = plugin.getMessagesConfig().getStringList("motd")

        // Send each MOTD line to the player
        for (line in motdLines) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', line))
        }
    }
}