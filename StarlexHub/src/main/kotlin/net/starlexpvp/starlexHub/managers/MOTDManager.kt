// File: StarlexHub/src/main/kotlin/net/starlexpvp/starlexHub/managers/MOTDManager.kt
package net.starlexpvp.starlexHub.managers

import org.bukkit.ChatColor
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class MOTDManager(private val plugin: JavaPlugin) {

    private lateinit var messagesConfig: YamlConfiguration
    private lateinit var messagesFile: File

    /**
     * Loads the messages configuration from file
     */
    fun loadConfig() {
        // Load or create the messages.yml file
        messagesFile = File(plugin.dataFolder, "messages.yml")
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false) // Save the default messages.yml from resources
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile)
    }

    /**
     * Sends the MOTD message to a player
     */
    fun sendMOTD(player: Player) {
        // Load the MOTD from messages.yml
        val motdLines = messagesConfig.getStringList("motd")

        // Send each MOTD line to the player
        for (line in motdLines) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', line))
        }
    }

    /**
     * Gets the messages configuration
     */
    fun getMessagesConfig(): YamlConfiguration {
        return messagesConfig
    }
}