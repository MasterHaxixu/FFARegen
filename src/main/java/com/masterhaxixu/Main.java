package com.masterhaxixu;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.LoadedMultiverseWorld;
import org.mvplugins.multiverse.core.world.WorldManager;
import org.mvplugins.multiverse.core.world.options.CloneWorldOptions;
import org.mvplugins.multiverse.core.world.options.DeleteWorldOptions;
import org.mvplugins.multiverse.external.vavr.control.Option;

import com.masterhaxixu.Commands.FFARegen;

public class Main extends JavaPlugin {
    private final Map<String, ArenaConfig> arenas = new HashMap<>();
    private final Set<String> regeneratingArenas = new HashSet<>();
    private int defaultDurationMinutes;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadArenaConfig();

        if (this.getCommand("regenffa") != null) {
            this.getCommand("regenffa").setExecutor(new FFARegen(this));
        }

        startAutomaticRegenTasks();
    }

    @Override
    public void onDisable() {
    }

    public Set<String> getArenaIds() {
        return new HashSet<>(arenas.keySet());
    }

    public boolean hasArena(String arenaId) {
        return arenas.containsKey(normalizeArenaId(arenaId));
    }

    public boolean regenerateArena(String arenaId, CommandSender sender) {
        String normalizedArenaId = normalizeArenaId(arenaId);
        final ArenaConfig arenaConfig = arenas.get(normalizedArenaId);

        if (arenaConfig == null) {
            if (sender != null) {
                sender.sendMessage(ChatColor.RED + "Unknown arena: " + arenaId);
            }
            return false;
        }

        synchronized (regeneratingArenas) {
            if (regeneratingArenas.contains(normalizedArenaId)) {
                if (sender != null) {
                    sender.sendMessage(ChatColor.YELLOW + "Arena '" + normalizedArenaId + "' is already regenerating.");
                }
                return false;
            }
            regeneratingArenas.add(normalizedArenaId);
        }

        RegenSession session = prepareRegenSession(arenaConfig);
        if (session == null) {
            synchronized (regeneratingArenas) {
                regeneratingArenas.remove(normalizedArenaId);
            }
            if (sender != null) {
                sender.sendMessage(ChatColor.RED + "Failed to start regeneration for arena '" + normalizedArenaId + "'. Check console logs.");
            }
            return false;
        }

        if (sender != null) {
            sender.sendMessage(ChatColor.YELLOW + "Started regenerating arena '" + normalizedArenaId + "'.");
        }

        runRegen(session, sender);
        return true;
    }

    private void loadArenaConfig() {
        reloadConfig();
        FileConfiguration config = getConfig();
        arenas.clear();

        defaultDurationMinutes = config.getInt("duration", 10);
        if (defaultDurationMinutes <= 0) {
            defaultDurationMinutes = 10;
        }

        ConfigurationSection arenaSection = config.getConfigurationSection("arenas");

        if (arenaSection != null && !arenaSection.getKeys(false).isEmpty()) {
            for (String arenaId : arenaSection.getKeys(false)) {
                ConfigurationSection section = arenaSection.getConfigurationSection(arenaId);
                if (section == null) {
                    continue;
                }

                ArenaConfig arenaConfig = new ArenaConfig();
                arenaConfig.setArenaId(normalizeArenaId(arenaId));
                arenaConfig.setEnabled(section.getBoolean("enabled", true));
                arenaConfig.setModelWorld(section.getString("modelworld", ""));
                arenaConfig.setResetWorld(section.getString("resetworld", ""));
                arenaConfig.setTempWorld(section.getString("world-between-regen", ""));
                arenaConfig.setX(section.getInt("x", 0));
                arenaConfig.setY(section.getInt("y", 64));
                arenaConfig.setZ(section.getInt("z", 0));
                arenaConfig.setDurationMinutes(section.getInt("duration", defaultDurationMinutes));
                arenas.put(arenaConfig.getArenaId(), arenaConfig);
            }
        }

        if (arenas.isEmpty()) {
            getLogger().severe("No arenas configured. Please add at least one entry under 'arenas' in config.yml.");
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    private void startAutomaticRegenTasks() {
        for (final ArenaConfig arenaConfig : arenas.values()) {
            if (!arenaConfig.isEnabled()) {
                continue;
            }

            int minutes = arenaConfig.getDurationMinutes();
            if (minutes <= 0) {
                minutes = defaultDurationMinutes;
            }
            if (minutes <= 0) {
                continue;
            }

            final String arenaId = arenaConfig.getArenaId();
            new BukkitRunnable() {
                @Override
                public void run() {
                    regenerateArena(arenaId, null);
                }
            }.runTaskTimer(this, 0L, 20L * 60L * minutes);
        }
    }

    private RegenSession prepareRegenSession(ArenaConfig arenaConfig) {
        if (!arenaConfig.isEnabled()) {
            return null;
        }

        MultiverseCoreApi core = MultiverseCoreApi.get();
        if (core == null) {
            getLogger().severe("MultiverseCoreApi is unavailable.");
            return null;
        }

        WorldManager worldManager = core.getWorldManager();
        if (worldManager == null) {
            getLogger().severe("Multiverse WorldManager is unavailable.");
            return null;
        }

        World tempWorld = Bukkit.getServer().getWorld(arenaConfig.getTempWorld());
        World arenaWorld = Bukkit.getServer().getWorld(arenaConfig.getResetWorld());
        if (tempWorld == null || arenaWorld == null) {
            getLogger().severe(() -> "Could not find temp or arena world for arena '" + arenaConfig.getArenaId() + "'.");
            return null;
        }

        Option<LoadedMultiverseWorld> worldToClone = worldManager.getLoadedWorld(arenaConfig.getModelWorld());
        if (worldToClone.isEmpty()) {
            getLogger().severe(() -> "Could not find model world '" + arenaConfig.getModelWorld() + "' for arena '" + arenaConfig.getArenaId() + "'.");
            return null;
        }

        Bukkit.broadcastMessage(ChatColor.RED + "[" + ChatColor.GOLD + "Server Announcement" + ChatColor.RED + "] Regenerating arena '" + arenaConfig.getArenaId() + "'.");

        Map<UUID, Location> playerLocations = new HashMap<>();
        Location fallbackTempLocation = new Location(tempWorld, arenaConfig.getX(), arenaConfig.getY(), arenaConfig.getZ());
        for (Player player : arenaWorld.getPlayers()) {
            playerLocations.put(player.getUniqueId(), player.getLocation());
            player.teleport(fallbackTempLocation);
        }

        RegenSession session = new RegenSession();
        session.setArenaConfig(arenaConfig);
        session.setPlayerLocations(playerLocations);
        session.setFallbackTempLocation(fallbackTempLocation);
        return session;
    }

    private void runRegen(final RegenSession session, final CommandSender sender) {
        Runnable regenTask = new Runnable() {
            @Override
            public void run() {
                boolean success = false;

                try {
                    MultiverseCoreApi core = MultiverseCoreApi.get();
                    if (core == null) {
                        getLogger().severe("MultiverseCoreApi is unavailable.");
                    } else {
                        WorldManager worldManager = core.getWorldManager();
                        if (worldManager == null) {
                            getLogger().severe("Multiverse WorldManager is unavailable.");
                        } else {
                            Option<LoadedMultiverseWorld> worldToClone = worldManager.getLoadedWorld(session.getArenaConfig().getModelWorld());
                            if (worldToClone.isEmpty()) {
                                getLogger().severe(() -> "Could not find model world '" + session.getArenaConfig().getModelWorld() + "' for arena '" + session.getArenaConfig().getArenaId() + "'.");
                            } else {
                                Option<LoadedMultiverseWorld> worldToDelete = worldManager.getLoadedWorld(session.getArenaConfig().getResetWorld());
                                if (!worldToDelete.isEmpty()) {
                                    worldManager.deleteWorld(DeleteWorldOptions.world(worldToDelete.get()));
                                }

                                CloneWorldOptions cloneOptions = CloneWorldOptions.fromTo(worldToClone.get(), session.getArenaConfig().getResetWorld());
                                worldManager.cloneWorld(cloneOptions);
                                worldManager.loadWorld(session.getArenaConfig().getResetWorld());

                                Option<LoadedMultiverseWorld> loadedArenaWorld = worldManager.getLoadedWorld(session.getArenaConfig().getResetWorld());
                                if (!loadedArenaWorld.isEmpty()) {
                                    loadedArenaWorld.get().setAlias(session.getArenaConfig().getResetWorld());
                                }

                                success = true;
                            }
                        }
                    }
                } catch (Exception ex) {
                    getLogger().log(java.util.logging.Level.SEVERE, "Failed regenerating arena '" + session.getArenaConfig().getArenaId() + "'.", ex);
                }

                finalizeRegen(session, sender, success);
            }
        };

        if (Bukkit.isPrimaryThread()) {
            regenTask.run();
            return;
        }

        Bukkit.getScheduler().runTask(this, regenTask);
    }

    private void finalizeRegen(RegenSession session, CommandSender sender, boolean regenSuccess) {
        World newArenaWorld = Bukkit.getServer().getWorld(session.getArenaConfig().getResetWorld());
        boolean canReturnToArena = regenSuccess && newArenaWorld != null;

        if (regenSuccess && newArenaWorld == null) {
            getLogger().severe(() -> "Failed to load regenerated world '" + session.getArenaConfig().getResetWorld() + "'.");
        }

        for (Map.Entry<UUID, Location> entry : session.getPlayerLocations().entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) {
                continue;
            }

            Location oldLocation = entry.getValue();
            if (canReturnToArena) {
                Location restoredArenaLocation = new Location(newArenaWorld, oldLocation.getX(), oldLocation.getY(), oldLocation.getZ(), oldLocation.getYaw(), oldLocation.getPitch());
                player.teleport(restoredArenaLocation);
            } else {
                player.teleport(session.getFallbackTempLocation());
            }
        }

        if (canReturnToArena) {
            Bukkit.broadcastMessage(ChatColor.RED + "[" + ChatColor.GOLD + "Server Announcement" + ChatColor.RED + "] Regenerated arena '" + session.getArenaConfig().getArenaId() + "'.");
            if (sender != null) {
                sender.sendMessage(ChatColor.GREEN + "Regenerated arena '" + session.getArenaConfig().getArenaId() + "'.");
            }
        } else {
            Bukkit.broadcastMessage(ChatColor.RED + "[" + ChatColor.GOLD + "Server Announcement" + ChatColor.RED + "] Failed to regenerate arena '" + session.getArenaConfig().getArenaId() + "'. Players remain in the temp world.");
            if (sender != null) {
                sender.sendMessage(ChatColor.RED + "Failed to regenerate arena '" + session.getArenaConfig().getArenaId() + "'. Players were moved to the temp world.");
            }
        }

        synchronized (regeneratingArenas) {
            regeneratingArenas.remove(session.getArenaConfig().getArenaId());
        }
    }

    private String normalizeArenaId(String arenaId) {
        if (arenaId == null) {
            return "";
        }
        return arenaId.trim().toLowerCase();
    }

}