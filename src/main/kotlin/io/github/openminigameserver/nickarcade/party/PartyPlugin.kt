package io.github.openminigameserver.nickarcade.party

import io.github.nickacpt.nickarcade.commands.PartyCommands
import io.github.openminigameserver.nickarcade.chat.ChatChannelsManager
import io.github.openminigameserver.nickarcade.core.commandAnnotationParser
import io.github.openminigameserver.nickarcade.core.commandManager
import io.github.openminigameserver.nickarcade.core.manager.PlayerDataManager
import io.github.openminigameserver.nickarcade.core.separator
import io.github.openminigameserver.nickarcade.display.displayOverrides
import io.github.openminigameserver.nickarcade.display.managers.ProfilesManager
import io.github.openminigameserver.nickarcade.display.nick.RandomNickGenerator
import io.github.openminigameserver.nickarcade.party.chat.PartyChatChannel
import io.github.openminigameserver.nickarcade.party.commands.TestCommands
import io.github.openminigameserver.nickarcade.party.events.PartyPlayerLeaveEvent
import io.github.openminigameserver.nickarcade.party.events.PartySettingChangedEvent
import io.github.openminigameserver.nickarcade.party.model.PartySettings
import io.github.openminigameserver.nickarcade.plugin.extensions.event
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import io.github.openminigameserver.nickarcade.display.commands.TestCommands as DisplayTestCommands

class PartyPlugin : JavaPlugin() {
    override fun onEnable() {
        ChatChannelsManager.registerChannel(PartyChatChannel)
        commandAnnotationParser.parse(PartyCommands)
        commandAnnotationParser.parse(TestCommands)
        PartyCommands.registerPartySettings(commandManager)

        event<PartyPlayerLeaveEvent> {
            if (player.displayOverrides?.isPartyDisguise == true) {
                player.displayOverrides?.resetDisguise()
                PlayerDataManager.reloadProfile(player)
            }
        }

        event<PartySettingChangedEvent<Boolean>> {
            val toggleMessage = if (newValue) "enabled" else "disabled"
            val colour = if (newValue) NamedTextColor.GREEN else NamedTextColor.RED

            party.audience.sendMessage(
                separator {
                    append(text(player.getChatName(actualData = true, colourPrefixOnly = false)))
                    append(text(" has $toggleMessage ${setting.description}", colour))
                }
            )

        }

        event<PartySettingChangedEvent<Boolean>>(forceBlocking = true) {
            if (prop == PartySettings::allNick) {
                party.membersList.map { it.player }.forEach {
                    val hasDisguise = it.displayOverrides?.displayProfile != null
                    if (hasDisguise && newValue) return@forEach
                    if (newValue) {
                        it.displayOverrides?.apply {
                            val name = RandomNickGenerator.getNewName()
                            displayProfile = ProfilesManager.profiles.random().copy(name, name, name, UUID.randomUUID())
                            overrides = DisplayTestCommands.randomPlayerOverrides()
                            isPartyDisguise = true
                        }
                    } else if (it.displayOverrides?.isPartyDisguise == true) {
                        it.displayOverrides?.resetDisguise()
                    }

                    PlayerDataManager.reloadProfile(it)
                }
            }
        }
    }

}