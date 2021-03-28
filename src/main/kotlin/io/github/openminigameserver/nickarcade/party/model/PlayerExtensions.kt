package io.github.openminigameserver.nickarcade.party.model

import io.github.openminigameserver.nickarcade.core.data.sender.player.ArcadePlayer
import io.github.openminigameserver.nickarcade.core.separator
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor

fun ArcadePlayer.getCurrentParty(showPrompt: Boolean = false): Party? {
    return PartyManager.getParty(this).also {
        if (it == null && showPrompt) {
            audience.sendMessage(separator {
                append(text("You are not currently in a party.", NamedTextColor.RED))
            })
        }
    }
}

fun ArcadePlayer.setCurrentParty(party: Party?) = PartyManager.setPlayerParty(this, party)

fun ArcadePlayer.getOrCreateParty(): Party {
    if (getCurrentParty() == null) {
        return PartyManager.createParty(this)
    }
    return getCurrentParty() as Party
}