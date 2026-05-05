package com.masterhaxixu;

public class ArenaConfig {

    private String arenaId;
    private boolean enabled;
    private String modelWorld;
    private String resetWorld;
    private String tempWorld;
    private int x;
    private int y;
    private int z;
    private int durationMinutes;

    public String getArenaId() {
        return arenaId;
    }

    public void setArenaId(String arenaId) {
        this.arenaId = arenaId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getModelWorld() {
        return modelWorld;
    }

    public void setModelWorld(String modelWorld) {
        this.modelWorld = modelWorld;
    }

    public String getResetWorld() {
        return resetWorld;
    }

    public void setResetWorld(String resetWorld) {
        this.resetWorld = resetWorld;
    }

    public String getTempWorld() {
        return tempWorld;
    }

    public void setTempWorld(String tempWorld) {
        this.tempWorld = tempWorld;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
}
