package me.eclipsia.lastlocation.commands;

import me.eclipsia.lastlocation.EclipsiaLastLocation;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

/**
 * Handles the /ell command.
 *
 * Sub-commands:
 *   /ell reload   — reloads config.yml and locations.yml
 */
public class EllCommand implements CommandExecutor, TabCompleter {

    private final EclipsiaLastLocation plugin;

    public EllCommand(EclipsiaLastLocation plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {

        String prefix = plugin.getPrefix();

        if (args.length == 0) {
            sender.sendMessage(prefix + "§7EclipsiaLastLocation §bv"
                    + plugin.getDescription().getVersion());
            sender.sendMessage(prefix + "§7Usage: §f/ell reload");
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "reload" -> {
                if (!sender.hasPermission("eclipsialastlocation.reload")) {
                    sender.sendMessage(prefix + "§cYou don't have permission to do that.");
                    return true;
                }
                plugin.reload();
                sender.sendMessage(prefix + "§aConfiguration and locations reloaded successfully.");
                return true;
            }

            default -> {
                sender.sendMessage(prefix + "§cUnknown sub-command. Use: §f/ell reload");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (args.length == 1) {
            return List.of("reload");
        }
        return List.of();
    }
}
