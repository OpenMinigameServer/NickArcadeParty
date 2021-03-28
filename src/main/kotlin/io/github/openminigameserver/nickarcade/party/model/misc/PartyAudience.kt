package io.github.openminigameserver.nickarcade.party.model.misc

import io.github.openminigameserver.nickarcade.party.model.Party
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.audience.ForwardingAudience

class PartyAudience(private val party: Party) : ForwardingAudience {
    override fun audiences(): Iterable<Audience> {
        return party.membersList.filter { it.role.canReceiveMessages }.map { it.player }.filter { it.isOnline }
            .map { it.audience }
    }
}