package com.findtheblock.party;

import com.findtheblock.FindTheBlock;
import com.findtheblock.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player party groups, public party finder, and leader match controls.
 */
public class PartyManager {

    private final FindTheBlock plugin;
    // Maps Member UUID -> Party
    private final Map<UUID, Party> memberPartyMap = new ConcurrentHashMap<>();
    // Maps Leader UUID -> Party
    private final Map<UUID, Party> partiesByLeader = new ConcurrentHashMap<>();
    // Maps Invited Player UUID -> Leader UUID
    private final Map<UUID, UUID> pendingInvites = new ConcurrentHashMap<>();

    public PartyManager(FindTheBlock plugin) {
        this.plugin = plugin;
    }

    public boolean isPartyLeader(UUID uuid) {
        Party party = memberPartyMap.get(uuid);
        return party != null && party.getLeaderUuid().equals(uuid);
    }

    public Party getParty(UUID uuid) {
        return memberPartyMap.get(uuid);
    }

    public Party getPartyByLeader(UUID leaderUuid) {
        return partiesByLeader.get(leaderUuid);
    }

    public UUID getPartyLeader(UUID uuid) {
        Party party = memberPartyMap.get(uuid);
        return party != null ? party.getLeaderUuid() : null;
    }

    public Set<UUID> getPartyMembers(UUID leaderUuid) {
        Party party = partiesByLeader.get(leaderUuid);
        return party != null ? party.getMembers() : Collections.singleton(leaderUuid);
    }

    public Set<UUID> getPlayerPartyMembers(UUID playerUuid) {
        Party party = memberPartyMap.get(playerUuid);
        return party != null ? party.getMembers() : Collections.singleton(playerUuid);
    }

    public List<Party> getPublicParties() {
        List<Party> list = new ArrayList<>();
        for (Party p : partiesByLeader.values()) {
            if (p.isPublic() && !p.isFull()) {
                list.add(p);
            }
        }
        return list;
    }

    public boolean createParty(Player leader, boolean isPublic) {
        UUID uuid = leader.getUniqueId();
        if (memberPartyMap.containsKey(uuid)) {
            Chat.send(leader, "<yellow>You are already in a party!</yellow>");
            return false;
        }

        Party party = new Party(uuid, leader.getName(), isPublic, 8);
        partiesByLeader.put(uuid, party);
        memberPartyMap.put(uuid, party);

        String visibility = isPublic ? "<green>Public</green>" : "<yellow>Private</yellow>";
        Chat.send(leader, "<green>Party created (" + visibility + ").</green>");
        return true;
    }

    public void setPublic(Player leader, boolean isPublic) {
        UUID uuid = leader.getUniqueId();
        Party party = partiesByLeader.get(uuid);
        if (party == null) {
            Chat.send(leader, "<red>You are not the party leader.</red>");
            return;
        }
        party.setPublic(isPublic);
        String visibility = isPublic ? "<green>Public</green>" : "<yellow>Private</yellow>";
        broadcastToParty(party, "<yellow>Party set to " + visibility + ".</yellow>");
    }

    public void joinPublicParty(Player player, UUID leaderUuid) {
        UUID uuid = player.getUniqueId();
        if (memberPartyMap.containsKey(uuid)) {
            Chat.send(player, "<yellow>Leave your current party first.</yellow>");
            return;
        }

        Party party = partiesByLeader.get(leaderUuid);
        if (party == null || !party.isPublic()) {
            Chat.send(player, "<red>Party unavailable.</red>");
            return;
        }

        if (party.isFull()) {
            Chat.send(player, "<red>Party is full.</red>");
            return;
        }

        party.addMember(uuid);
        memberPartyMap.put(uuid, party);

        Chat.send(player, "<green>Joined <yellow>" + party.getLeaderName() + "'s</yellow> party.</green>");
        broadcastToParty(party, "<green>" + player.getName() + " joined the party.</green>");
    }

    public void invitePlayer(Player leader, Player target) {
        UUID leaderUuid = leader.getUniqueId();
        Party party = partiesByLeader.get(leaderUuid);
        if (party == null) {
            Chat.send(leader, "<red>Only the party leader can invite players.</red>");
            return;
        }

        if (party.isFull()) {
            Chat.send(leader, "<red>Party is full.</red>");
            return;
        }

        if (memberPartyMap.containsKey(target.getUniqueId())) {
            Chat.send(leader, "<yellow>" + target.getName() + " is already in a party.</yellow>");
            return;
        }

        pendingInvites.put(target.getUniqueId(), leaderUuid);

        Chat.send(leader, "<green>Invited " + target.getName() + ".</green>");
        Chat.send(target, "<gold>" + leader.getName() + " invited you to a party.</gold> " +
                          "<click:run_command:/ftb party accept><green>[Accept]</green></click> " +
                          "<click:run_command:/ftb party deny><red>[Decline]</red></click>");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingInvites.remove(target.getUniqueId(), leaderUuid)) {
                Chat.send(target, "<gray>Party invite from " + leader.getName() + " expired.</gray>");
            }
        }, 600L); // 30 seconds
    }

    public void acceptInvite(Player player) {
        UUID uuid = player.getUniqueId();
        UUID leaderUuid = pendingInvites.remove(uuid);

        if (leaderUuid == null || !partiesByLeader.containsKey(leaderUuid)) {
            Chat.send(player, "<red>You have no pending party invites!</red>");
            return;
        }

        if (memberPartyMap.containsKey(uuid)) {
            Chat.send(player, "<yellow>You are already in a party! Leave your party first.</yellow>");
            return;
        }

        Party party = partiesByLeader.get(leaderUuid);
        if (party == null || party.isFull()) {
            Chat.send(player, "<red>This party is full or no longer exists!</red>");
            return;
        }

        party.addMember(uuid);
        memberPartyMap.put(uuid, party);

        Chat.send(player, "<green>Joined <yellow>" + party.getLeaderName() + "'s</yellow> party!</green>");
        broadcastToParty(party, "<green>" + player.getName() + " joined the party!</green>");
    }

    public void denyInvite(Player player) {
        UUID leaderUuid = pendingInvites.remove(player.getUniqueId());
        if (leaderUuid != null) {
            Chat.send(player, "<gray>Declined party invite.</gray>");
            Player leader = Bukkit.getPlayer(leaderUuid);
            if (leader != null) {
                Chat.send(leader, "<gray>" + player.getName() + " declined your party invite.</gray>");
            }
        } else {
            Chat.send(player, "<gray>No pending invites to decline.</gray>");
        }
    }

    public void leaveParty(Player player) {
        UUID uuid = player.getUniqueId();
        Party party = memberPartyMap.remove(uuid);

        if (party == null) {
            Chat.send(player, "<yellow>You are not in a party.</yellow>");
            return;
        }

        if (party.getLeaderUuid().equals(uuid)) {
            disbandParty(uuid);
        } else {
            party.removeMember(uuid);
            Chat.send(player, "<yellow>You left the party.</yellow>");
            broadcastToParty(party, "<yellow>" + player.getName() + " left the party.</yellow>");
        }
    }

    public void disbandParty(UUID leaderUuid) {
        Party party = partiesByLeader.remove(leaderUuid);
        if (party != null) {
            for (UUID memberUuid : party.getMembers()) {
                memberPartyMap.remove(memberUuid);
                Player p = Bukkit.getPlayer(memberUuid);
                if (p != null) {
                    Chat.send(p, "<red>Party disbanded.</red>");
                }
            }
        }
    }

    public void broadcastToParty(Party party, String message) {
        if (party == null) return;
        for (UUID memberUuid : party.getMembers()) {
            Player p = Bukkit.getPlayer(memberUuid);
            if (p != null && p.isOnline()) {
                Chat.send(p, message);
            }
        }
    }
}
