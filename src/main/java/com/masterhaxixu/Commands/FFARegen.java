package com.masterhaxixu.Commands;

import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.masterhaxixu.Main;

public class FFARegen implements CommandExecutor {
    private final Main plugin;

    public FFARegen(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " <arena|all>");
            sender.sendMessage(ChatColor.YELLOW + "Configured arenas: " + plugin.getArenaIds());
            return true;
        }

        String target = args[0].trim().toLowerCase();

        if ("all".equals(target)) {
            int successCount = 0;
            Set<String> arenaIds = plugin.getArenaIds();
            for (String arenaId : arenaIds) {
                if (plugin.regenerateArena(arenaId, sender)) {
                    successCount++;
                }
            }
            sender.sendMessage(ChatColor.GREEN + "Triggered regen for " + successCount + " of " + arenaIds.size() + " arena(s).");
            return true;
        }

        plugin.regenerateArena(target, sender);
        return true;
    }

}