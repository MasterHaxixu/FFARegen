package com.masterhaxixu;

import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;

public class RegenSession {

    private ArenaConfig arenaConfig;
    private Map<UUID, Location> playerLocations;
    private Location fallbackTempLocation;

    public ArenaConfig getArenaConfig() {
        return arenaConfig;
    }

    public void setArenaConfig(ArenaConfig arenaConfig) {
        this.arenaConfig = arenaConfig;
    }

    public Map<UUID, Location> getPlayerLocations() {
        return playerLocations;
    }

    public void setPlayerLocations(Map<UUID, Location> playerLocations) {
        this.playerLocations = playerLocations;
    }

    public Location getFallbackTempLocation() {
        return fallbackTempLocation;
    }

    public void setFallbackTempLocation(Location fallbackTempLocation) {
        this.fallbackTempLocation = fallbackTempLocation;
    }
}
