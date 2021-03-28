package io.github.openminigameserver.nickarcade.party

import io.github.nickacpt.nickarcade.commands.PartyCommands
import io.github.openminigameserver.nickarcade.core.commandAnnotationParser
import io.github.openminigameserver.nickarcade.core.commandManager
import io.github.openminigameserver.nickarcade.party.commands.TestCommands
import org.bukkit.plugin.java.JavaPlugin

class PartyPlugin : JavaPlugin() {
    override fun onEnable() {
        commandAnnotationParser.parse(PartyCommands)
        commandAnnotationParser.parse(TestCommands)
        PartyCommands.registerPartySettings(commandManager)
    }

}