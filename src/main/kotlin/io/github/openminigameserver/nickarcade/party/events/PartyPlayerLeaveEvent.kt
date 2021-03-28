package io.github.openminigameserver.nickarcade.party.events

import io.github.openminigameserver.nickarcade.core.data.sender.player.ArcadePlayer
import io.github.openminigameserver.nickarcade.party.model.Party
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class PartyPlayerLeaveEvent(val party: Party, val player: ArcadePlayer) : Event() {
    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}