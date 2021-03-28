package io.github.openminigameserver.nickarcade.party.model

import io.github.openminigameserver.hypixelapi.models.HypixelPackageRank
import io.github.openminigameserver.nickarcade.core.data.sender.player.ArcadePlayer
import io.github.openminigameserver.nickarcade.party.model.misc.PartySetting
import io.github.openminigameserver.nickarcade.party.events.PartySettingChangedEvent
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.findAnnotation

data class PartySettings(
    val party: Party,
    @property:PartySetting("Private Game", HypixelPackageRank.SUPERSTAR, "private")
    var privateMode: Boolean = false,
    @property:PartySetting("Developer Game", HypixelPackageRank.ADMIN, aliases = ["developer", "dev"])
    var developerMode: Boolean = false,
    @property:PartySetting("All Nicked", HypixelPackageRank.NONE, "allnicked", "allnick")
    var allNick: Boolean = false,
) {
    fun <T> setPropertyAndNotify(sender: ArcadePlayer, prop: KMutableProperty1<PartySettings, T>, value: T) {
        prop.set(this, value)
        prop.findAnnotation<PartySetting>()?.let { setting ->
            PartySettingChangedEvent(
                party,
                sender,
                setting,
                prop,
                value
            ).callEvent()
        }
    }
}