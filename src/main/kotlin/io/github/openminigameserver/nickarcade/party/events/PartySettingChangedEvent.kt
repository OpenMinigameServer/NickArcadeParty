package io.github.openminigameserver.nickarcade.party.events

import io.github.openminigameserver.nickarcade.core.data.sender.player.ArcadePlayer
import io.github.openminigameserver.nickarcade.party.model.misc.PartySetting
import io.github.openminigameserver.nickarcade.party.model.Party
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import kotlin.reflect.KProperty

class PartySettingChangedEvent<T>(
    val party: Party,
    val player: ArcadePlayer,
    val setting: PartySetting,
    val prop: KProperty<T>,
    val newValue: T
) : Event() {
    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}