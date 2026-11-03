package com.findtheblock.party;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a player party with public/private visibility and member tracking.
 */
public class Party {

    private final UUID leaderUuid;
    private String leaderName;
    private final Set<UUID> members = new LinkedHashSet<>();
    private boolean isPublic;
    private final int maxSize;
    private final long createdAt;

    public Party(UUID leaderUuid, String leaderName, boolean isPublic, int maxSize) {
        this.leaderUuid = leaderUuid;
        this.leaderName = leaderName;
        this.isPublic = isPublic;
        this.maxSize = maxSize;
        this.createdAt = System.currentTimeMillis();
        this.members.add(leaderUuid);
    }

    public UUID getLeaderUuid() { return leaderUuid; }
    public String getLeaderName() { return leaderName; }
    public void setLeaderName(String leaderName) { this.leaderName = leaderName; }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }

    public int getMaxSize() { return maxSize; }
    public long getCreatedAt() { return createdAt; }

    public synchronized Set<UUID> getMembers() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(members));
    }

    public synchronized boolean addMember(UUID uuid) {
        if (members.size() >= maxSize) return false;
        return members.add(uuid);
    }

    public synchronized boolean removeMember(UUID uuid) {
        return members.remove(uuid);
    }

    public synchronized boolean contains(UUID uuid) {
        return members.contains(uuid);
    }

    public synchronized int size() {
        return members.size();
    }

    public boolean isFull() {
        return members.size() >= maxSize;
    }
}
