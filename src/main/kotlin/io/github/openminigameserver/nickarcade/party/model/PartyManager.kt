package io.github.openminigameserver.nickarcade.party.model

import io.github.openminigameserver.nickarcade.core.data.sender.player.ArcadePlayer
import io.github.openminigameserver.nickarcade.core.data.sender.player.extra.RuntimeExtraDataTag
import java.util.*

object PartyManager {

    private val partyMap = mutableMapOf<UUID, Party>()

    private val playerParty = RuntimeExtraDataTag.of<Party>("")

    fun createParty(player: ArcadePlayer): Party {
        return createNewParty().apply { addMember(player, role = MemberRole.LEADER) }
    }

    fun createNewParty() = Party().also {
        partyMap[it.id] = it
    }

    fun removeParty(party: UUID) = partyMap.remove(party)

    fun setPlayerParty(player: ArcadePlayer, party: Party?) {
        if (party != null) {
            player[playerParty] = party
            return
        }

        player[playerParty] = null
    }


    fun getParty(partyId: UUID): Party? {
        return partyMap[partyId]
    }

    fun getParty(player: ArcadePlayer): Party? {
        return player[playerParty]
    }
}