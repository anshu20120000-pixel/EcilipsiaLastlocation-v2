package me.eclipsia.lastlocation.listeners;

import me.eclipsia.lastlocation.EclipsiaLastLocation;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Listens for players joining the server.
 *
 * The teleport is executed with a configurable delay so that other plugins
 * (EssentialsSpawn, AuthMe, DeluxeHub, FastLogin, Multiverse spawn) finish
 * their own teleports before we restore the logout location.
 *
 * Strategy:
 *   1. Player joins → LOWEST priority so we see the event early.
 *   2. Schedule a delayed task (teleport-delay seconds in ticks).
 *   3. In the task, verify the player is still online and still in an
 *      enabled world, then teleport.
 */
public class PlayerJoinListener implements Listener {

    private final EclipsiaLastLocation plugin;

    public PlayerJoinListener(EclipsiaLastLocation plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getLocationStorage().hasLocation(player.getUniqueId())) {
            return; // First join or no saved location — nothing to do.
        }

        long delayTicks = plugin.getTeleportDelayTicks();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {

            // Guard: player may have disconnected during the delay window.
            if (!player.isOnline()) return;

            Location savedLoc = plugin.getLocationStorage().getLocation(player.getUniqueId());
            if (savedLoc == null || savedLoc.getWorld() == null) return;

            String savedWorldName = savedLoc.getWorld().getName();

            // Only teleport if the saved world is still in the enabled list.
            if (!plugin.isWorldEnabled(savedWorldName)) return;

            // Only teleport if the player is currently in an enabled world.
            // (If they spawned in lobby due to another plugin, skip.)
            if (player.getWorld() == null) return;
            String currentWorld = player.getWorld().getName();
            if (!plugin.isWorldEnabled(currentWorld)) return;

            player.teleport(savedLoc);

            plugin.getLogger().fine("Restored join location for " + player.getName()
                    + " → " + savedWorldName
                    + " (" + savedLoc.getBlockX() + ", " + savedLoc.getBlockY()
                    + ", " + savedLoc.getBlockZ() + ")");

        }, delayTicks);
    }
}
