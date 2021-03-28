package io.github.nickacpt.nickarcade.commands

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.arguments.CommandArgument
import cloud.commandframework.kotlin.MutableCommandBuilder
import cloud.commandframework.kotlin.extension.buildAndRegister
import io.github.openminigameserver.hypixelapi.models.HypixelPackageRank
import io.github.openminigameserver.nickarcade.core.data.sender.ArcadeSender
import io.github.openminigameserver.nickarcade.core.data.sender.player.ArcadePlayer
import io.github.openminigameserver.nickarcade.core.separator
import io.github.openminigameserver.nickarcade.party.model.PartySettings
import io.github.openminigameserver.nickarcade.party.model.getCurrentParty
import io.github.openminigameserver.nickarcade.party.model.getOrCreateParty
import io.github.openminigameserver.nickarcade.party.model.misc.PartySetting
import io.github.openminigameserver.nickarcade.plugin.extensions.command
import io.github.openminigameserver.nickarcade.plugin.helper.commands.NickArcadeCommandManager
import io.github.openminigameserver.nickarcade.plugin.helper.commands.RequiredRank
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure


object PartyCommands {

    fun registerPartySettings(manager: NickArcadeCommandManager<ArcadeSender>) {
        val partySettingsKClass = PartySettings::class
        val properties = partySettingsKClass.memberProperties.filter { it.hasAnnotation<PartySetting>() }
        properties.forEach { prop ->
            if (prop !is KMutableProperty1<PartySettings, *>) return@forEach
            val setting = prop.findAnnotation<PartySetting>() ?: return@forEach

            manager.buildAndRegister("party", aliases = arrayOf("p")) {
                literal("settings", aliases = arrayOf("setting"))
                literal(setting.aliases.first(), aliases = setting.aliases.drop(1).toTypedArray())
                createPartyPropSubcommand(prop, setting)
            }
        }
    }

    private fun MutableCommandBuilder<ArcadeSender>.createPartyPropSubcommand(
        prop: KProperty1<PartySettings, *>,
        setting: PartySetting
    ) {
        senderType(ArcadePlayer::class.java)
        val isToggle = prop.returnType.jvmErasure == Boolean::class
        if (!isToggle) {
            val returnType = prop.returnType.jvmErasure.java
            this.argument(CommandArgument.ofType(returnType, "value"))
        }

        handler {
            @Suppress("UNCHECKED_CAST") val partyProp = prop as KMutableProperty1<PartySettings, Any?>

            val sender = it.sender as? ArcadePlayer ?: return@handler
            command(sender, setting.requiredRank) {
                val party = sender.getCurrentParty(true) ?: return@command
                if (!party.canModifySettings(sender)) {
                    sender.audience.sendMessage(separator {
                        append(text("You are not the party leader!", NamedTextColor.RED))
                    })
                    return@command
                }

                if (isToggle) {
                    val newValue = (partyProp.get(party.settings) as Boolean).not()
                    party.settings.setPropertyAndNotify(sender, partyProp, newValue)
                } else {
                    party.settings.setPropertyAndNotify(sender, partyProp, it["value"])
                }
            }
        }
    }

    @CommandMethod("party|p <target>")
    @CommandDescription("Invite a player to a party")
    fun invitePlayerShort(senderPlayer: ArcadePlayer, @Argument("target") target: ArcadePlayer) =
        invitePlayer(senderPlayer, target)

    @CommandMethod("pl")
    @CommandDescription("Invite a player to a party")
    fun listPartyShort(senderPlayer: ArcadePlayer) =
        listParty(senderPlayer)

    @CommandMethod("party|p invite <player>")
    @CommandDescription("Invite a player to a party")
    fun invitePlayer(
        senderPlayer: ArcadePlayer,
        @Argument("player")
        target: ArcadePlayer
    ) = command(senderPlayer) {
        val party = senderPlayer.getOrCreateParty()

        if (senderPlayer == target) {
            senderPlayer.audience.sendMessage(separator {
                append(text("You cannot party yourself!", NamedTextColor.RED))
            })
            return@command
        }
        party.invitePlayer(senderPlayer, target)
    }

    @CommandMethod("party|p leave")
    @CommandDescription("Leave the current party")
    fun partyLeavePlayer(
        senderPlayer: ArcadePlayer
    ) = command(senderPlayer) {
        val party = senderPlayer.getCurrentParty(true) ?: return@command

        party.removeMember(senderPlayer, true)
    }

    @CommandMethod("party|p disband")
    @CommandDescription("Disbands the party, removing everyone in it.")
    fun disbandParty(senderPlayer: ArcadePlayer) = command(senderPlayer) {
        val party = senderPlayer.getCurrentParty() ?: return@command

        party.audience.sendMessage(separator {
            append(text(senderPlayer.getChatName(actualData = true, colourPrefixOnly = false)))
            append(text(" has disbanded the party!", NamedTextColor.YELLOW))
        })

        party.disband()
    }

    @CommandMethod("party|p hijack <target>")
    @CommandDescription("Hijacks a party by replacing it's owner")
    fun hijackParty(senderPlayer: ArcadePlayer, @Argument("target") target: ArcadePlayer) = command(senderPlayer) {
        val party = target.getCurrentParty() ?: return@command

        party.switchOwner(senderPlayer)

        party.audience.sendMessage(separator {
            append(text(senderPlayer.getChatName(actualData = true, colourPrefixOnly = false)))
            append(text(" has hijacked the party!", NamedTextColor.YELLOW))
        })
    }

    @CommandMethod("party|p accept <target>")
    @CommandDescription("Accept a party invite from a player")
    fun acceptParty(senderPlayer: ArcadePlayer, @Argument("target") target: ArcadePlayer) = command(senderPlayer) {
        target.getCurrentParty()?.acceptPendingInvite(senderPlayer)
    }

    @CommandMethod("party|p kick <target>")
    @CommandDescription("Kick a player from the party")
    fun kickParty(senderPlayer: ArcadePlayer, @Argument("target") target: ArcadePlayer) = command(senderPlayer) {
        val party = senderPlayer.getCurrentParty(true) ?: return@command
        if (!party.isLeader(senderPlayer)) {
            senderPlayer.audience.sendMessage(separator {
                append(text("You are not the party leader.", NamedTextColor.RED))
            })
            return@command
        }

        party.removeMember(target, broadcast = true, isKick = true)

    }

    @CommandMethod("party|p list")
    @CommandDescription("Lists the current party members")
    fun listParty(senderPlayer: ArcadePlayer) = command(senderPlayer) {
        val party = senderPlayer.getCurrentParty(true) ?: return@command

        senderPlayer.audience.sendMessage(party.listMessage)
    }

    @RequiredRank(HypixelPackageRank.ADMIN)
    @CommandMethod("pl <target>")
    @CommandDescription("Lists the party members of a party owned by someone else")
    fun listPartyOtherShort(senderPlayer: ArcadePlayer, @Argument("target") target: ArcadePlayer) =
        listPartyOther(senderPlayer, target)

    @RequiredRank(HypixelPackageRank.ADMIN)
    @CommandMethod("party|p list <target>")
    @CommandDescription("Lists the party members of a party owned by someone else")
    fun listPartyOther(senderPlayer: ArcadePlayer, @Argument("target") target: ArcadePlayer) = command(senderPlayer) {
        val party = target.getCurrentParty() ?: run {
            senderPlayer.audience.sendMessage(separator {
                append(text(target.getChatName(actualData = true, colourPrefixOnly = false)))
                append(text(" is not in a party right now.", NamedTextColor.RED))
            })
            return@command
        }

        senderPlayer.audience.sendMessage(party.listMessage)
    }
}
