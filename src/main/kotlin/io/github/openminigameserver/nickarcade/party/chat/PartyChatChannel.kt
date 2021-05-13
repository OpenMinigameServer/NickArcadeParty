package io.github.openminigameserver.nickarcade.party.chat

import io.github.openminigameserver.nickarcade.chat.currentChannel
import io.github.openminigameserver.nickarcade.chat.impl.AbstractChatChannel
import io.github.openminigameserver.nickarcade.chat.model.ChatChannelType
import io.github.openminigameserver.nickarcade.chat.model.ChatMessageOrigin
import io.github.openminigameserver.nickarcade.core.data.sender.ArcadeSender
import io.github.openminigameserver.nickarcade.core.data.sender.player.ArcadePlayer
import io.github.openminigameserver.nickarcade.core.separator
import io.github.openminigameserver.nickarcade.party.model.getCurrentParty
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor

object PartyChatChannel : AbstractChatChannel(ChatChannelType.PARTY) {
    override suspend fun checkSender(sender: ArcadeSender, origin: ChatMessageOrigin): Boolean {
        if (sender !is ArcadePlayer) return false

        if (sender.getCurrentParty() == null) {
            if (origin == ChatMessageOrigin.CHAT && sender.currentChannel == ChatChannelType.PARTY) {
                sender.currentChannel = ChatChannelType.ALL
                sender.audience.sendMessage(separator {
                    append(text("You are not in a party and were moved to the ALL channel.", NamedTextColor.RED))
                })
            }
            return false
        }

        return true
    }

    override suspend fun getRecipients(sender: ArcadeSender, message: Component): Audience {
        return (sender as? ArcadePlayer)?.getCurrentParty()?.audience ?: Audience.empty()
    }
}