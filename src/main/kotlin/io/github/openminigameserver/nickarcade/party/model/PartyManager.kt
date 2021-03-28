package io.github.openminigameserver.nickarcade.party.model

import io.github.openminigameserver.nickarcade.core.data.sender.player.ArcadePlayer
import io.github.openminigameserver.nickarcade.core.data.sender.player.extra.RuntimeExtraDataTag

object PartyManager {

    private val playerParty = RuntimeExtraDataTag.of<Party>("")

    fun createParty(player: ArcadePlayer): Party {
        return Party().apply { addMember(player, role = MemberRole.LEADER) }
    }

    fun setPlayerParty(player: ArcadePlayer, party: Party?) {
        if (party != null) {
            player[playerParty] = party
            return
        }

        player[playerParty] = null
    }


    fun getParty(player: ArcadePlayer): Party? {
        return player[playerParty]
    }
}