package io.github.openminigameserver.nickarcade.party.model

import com.fasterxml.jackson.annotation.JsonIgnore
import io.github.openminigameserver.nickarcade.core.data.sender.misc.ArcadeWatcherSender
import io.github.openminigameserver.nickarcade.core.data.sender.player.ArcadePlayer
import io.github.openminigameserver.nickarcade.core.separator
import io.github.openminigameserver.nickarcade.party.events.PartyPlayerLeaveEvent
import io.github.openminigameserver.nickarcade.party.model.misc.PartyAudience
import io.github.openminigameserver.nickarcade.plugin.extensions.launchAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.newline
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor.*
import java.util.*
import kotlin.time.Duration

val partyExpiryTime = Duration.minutes(1)

data class Party(
    private val members: MutableMap<UUID, PartyMember> = mutableMapOf()
) {
    val membersList
        get() = members.values

    fun getLeaders(): List<PartyMember> {
        return getMembersWithRole(MemberRole.LEADER)
    }

    fun getMembersWithRole(role: MemberRole): List<PartyMember> {
        return membersList.filter { it.role == role }
    }

    var settings: PartySettings = PartySettings(this)

    val nonLeaderMembers
        get() = getMembersWithRole(MemberRole.MEMBER)

    private val nonLeaderMembersCount
        get() = nonLeaderMembers.count()

    fun isLeader(it: PartyMember): Boolean {
        return it.role == MemberRole.LEADER
    }

    fun isLeader(it: ArcadePlayer): Boolean {
        return getPlayerRole(it) == MemberRole.LEADER
    }

    fun getPlayerRole(player: ArcadePlayer): MemberRole {
        return members[player.uuid]?.role ?: MemberRole.NONE
    }

    val totalMembersCount
        get() = members.filterNot { it.value.role <= MemberRole.PENDING_INVITE }.count()

    fun hasPendingInvite(player: ArcadePlayer): Boolean {
        return getPlayerRole(player) == MemberRole.PENDING_INVITE
    }

    fun switchOwner(newOwner: ArcadePlayer, addOldOwner: Boolean = true) {
        setRole(newOwner, MemberRole.LEADER)

        if (addOldOwner) {
            getMembersWithRole(MemberRole.LEADER).forEach {
                setRole(it, MemberRole.MEMBER)
            }
        }
    }

    fun setRole(it: PartyMember, role: MemberRole) {
        setRole(it.player, role)
    }

    private fun setRole(it: ArcadePlayer, role: MemberRole) {
        val shouldSetParty = members[it.uuid]?.role == MemberRole.PENDING_INVITE
        members[it.uuid]?.role = role
        if (shouldSetParty) {
            it.setCurrentParty(this)
        }

        checkForBrokenParty()
    }

    private fun checkForBrokenParty() {
        if (getLeaders().isEmpty()) {
            ArcadeWatcherSender.sendMessage(text {
                it.append(text("Party with id ", RED))
                it.append(text("[", GOLD))
                it.append(text(id.toString(), GOLD))
                it.append(text("]", GOLD))
                it.append(text(" lost all the party leaders and thus, was broken.", RED))
            })
            val newLeader = nonLeaderMembers.firstOrNull()

            if (newLeader != null) {
                setRole(newLeader, MemberRole.LEADER)

                audience.sendMessage(separator {
                    append(text("An internal error occurred and your party has no leaders.", RED))
                    append(newline())
                    append(text(newLeader.player.getChatName(true)))
                    append(text(" is now the new party leader.", GOLD))
                })
            } else {
                // No members????? Disband it then
                disband()
            }
        }
    }

    fun invitePlayer(sender: ArcadePlayer, target: ArcadePlayer) {
        if (!canInvitePlayers(sender)) {
            sender.audience.sendMessage(separator {
                append(text("You can't invite players to this party!", RED))
            })
            return
        }

        if (hasPendingInvite(target)) {
            sender.audience.sendMessage(separator {
                append(text(target.getChatName(true)))
                append(text(" has already been invited to the party.", RED))
            })
            return
        }
        if (!target.isOnline) {
            sender.audience.sendMessage(separator {
                append(text("You cannot invite that player since they're not online.", RED))
            })
            return
        }

        launchAsync(scheduleInviteExpirationActions(sender, target))

        audience.sendMessage(separator(BLUE) {
            append(text(sender.getChatName(true)))
            append(text(" invited ", YELLOW))
            append(text(target.getChatName(true)))
            append(text(" to the party! They have ", YELLOW))
            append(text(partyExpiryTime.inWholeSeconds, RED))
            append(text(" seconds to accept.", YELLOW))
        })

        val command = "/party accept ${sender.actualDisplayName}"
        target.audience.sendMessage(separator(BLUE) {
            append(text(sender.getChatName(true)))
            append(text(" invited you to their party!", YELLOW))
            append(newline())
            append(text {
                it.append(text("You have ", YELLOW))
                it.append(text(partyExpiryTime.inWholeSeconds, RED))
                it.append(text(" seconds to accept. ", YELLOW))
                it.append(text("Click here to join!", GOLD))
            })
        }.clickEvent(ClickEvent.runCommand(command)).hoverEvent(text("Click to run $command")))
    }

    private fun canInvitePlayers(sender: ArcadePlayer): Boolean {
        return getPlayerRole(sender).canInvitePlayers
    }

    private fun scheduleInviteExpirationActions(
        sender: ArcadePlayer,
        target: ArcadePlayer
    ): suspend CoroutineScope.() -> Unit = scope@{
        addMember(target, role = MemberRole.PENDING_INVITE)
        delay(partyExpiryTime)
        if (!hasPendingInvite(target)) {
            return@scope
        }
        removeMember(target)
        target.audience.sendMessage(separator {
            append(text("The party invite from ", YELLOW))
            append(text(sender.getChatName(true)))
            append(text(" has expired.", YELLOW))
        })

        audience.sendMessage(separator {
            append(text("The party invite to ", YELLOW))
            append(text(target.getChatName(true)))
            append(text(" has expired.", YELLOW))
        })
    }

    fun addMember(member: ArcadePlayer, broadcast: Boolean = false, role: MemberRole = MemberRole.MEMBER) {
        //Remove member from old party
        member.getCurrentParty()?.removeMember(member, broadcast = true)

        this.members.putIfAbsent(member.uuid, PartyMember(member, role))
        if (role >= MemberRole.MEMBER) {
            member.setCurrentParty(this)
        }

        if (broadcast && role >= MemberRole.MEMBER) {
            broadcastPlayerJoin(member)
        }
    }

    private fun broadcastPlayerJoin(member: ArcadePlayer) {
        audience.sendMessage(separator {
            append(text(member.getChatName(true)))
            append(text(" joined the party.", YELLOW))
        })
    }

    fun removeMember(
        member: ArcadePlayer,
        broadcast: Boolean = false,
        isKick: Boolean = false
    ) {
        if (broadcast) {
            audience.sendMessage(separator {
                append(text(member.getChatName(true)))
                if (isKick) {
                    append(text(" has been kicked from the party.", YELLOW))
                } else {
                    append(text(" left the party.", YELLOW))
                }
            })
        }
        members.remove(member.uuid)
        member.setCurrentParty(null)
        PartyPlayerLeaveEvent(this, member).callEvent()
    }

    private fun TextComponent.Builder.appendPlayerData(member: PartyMember) {
        val it = member.player
        append(text(it.getChatName(true)))
        append(Component.space())
        append(text('●', if (it.isOnline) GREEN else RED))
        append(Component.space())
    }

    fun disband() {
        PartyManager.removeParty(id)
        if (totalMembersCount == 0) return
        membersList.toList().forEach {
            removeMember(it.player)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Party

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    fun acceptPendingInvite(target: ArcadePlayer) {
        if (!hasPendingInvite(target)) {
            target.audience.sendMessage(separator {
                append(text("That party has been disbanded.", RED))
            })
            return
        }

        setRole(target, MemberRole.MEMBER)
        broadcastPlayerJoin(target)
    }

    fun canModifySettings(player: ArcadePlayer): Boolean {
        return getPlayerRole(player).canModifySettings
    }

    fun isPrivateGameParty(): Boolean {
        return settings.privateMode
    }

    fun isDeveloperGameParty(): Boolean {
        return settings.developerMode
    }

    val listMessage: Component
        get() {
            return separator {
                append(text("Party members ($totalMembersCount)", GOLD)); append(newline())
                append(newline())
                append(text("Party Leaders: ", YELLOW))
                getMembersWithRole(MemberRole.LEADER).forEach { appendPlayerData(it) }
                if (nonLeaderMembersCount > 0) {
                    append(newline())
                    append(text("Party Members: ", YELLOW))
                    nonLeaderMembers.forEach {
                        appendPlayerData(it)
                    }
                }
            }
        }

    val id: UUID = UUID.randomUUID()

    @JsonIgnore
    val audience = PartyAudience(this)
}
