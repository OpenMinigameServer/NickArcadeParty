package io.github.openminigameserver.nickarcade.party.commands

import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import io.github.openminigameserver.hypixelapi.models.HypixelPackageRank
import io.github.openminigameserver.nickarcade.core.data.sender.player.ArcadePlayer
import io.github.openminigameserver.nickarcade.core.manager.getArcadeSender
import io.github.openminigameserver.nickarcade.party.model.MemberRole
import io.github.openminigameserver.nickarcade.party.model.PartySettings
import io.github.openminigameserver.nickarcade.party.model.getCurrentParty
import io.github.openminigameserver.nickarcade.party.model.getOrCreateParty
import io.github.openminigameserver.nickarcade.plugin.extensions.command
import io.github.openminigameserver.nickarcade.plugin.helper.commands.RequiredRank
import org.bukkit.Bukkit

object TestCommands {
    @RequiredRank(HypixelPackageRank.ADMIN)
    @CommandMethod("debugparty create")
    @CommandDescription("Create an empty party for debugging issues.")
    fun createDebugParty(sender: ArcadePlayer) = command(sender) {
        sender.getCurrentParty()?.disband()
        sender.getOrCreateParty().apply {
            Bukkit.getOnlinePlayers().forEach {
                if (it != sender.player) {
                    addMember(it.getArcadeSender())
                }
            }
            settings.setPropertyAndNotify(sender, PartySettings::developerMode, true)

        }
    }

    @RequiredRank(HypixelPackageRank.ADMIN)
    @CommandMethod("debugparty removeleader")
    @CommandDescription("Removes the leader of the party and sets it as member")
    fun removeLeaderDebugParty(sender: ArcadePlayer) {
        sender.getCurrentParty(true)?.apply {
            getLeaders().forEach { setRole(it, MemberRole.MEMBER) }
        }
    }
}