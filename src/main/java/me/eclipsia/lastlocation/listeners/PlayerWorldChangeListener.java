package me.eclipsia.lastlocation.listeners;

import me.eclipsia.lastlocation.EclipsiaLastLocation;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

/**
 * Listens for world-change events (e.g. Multiverse /mvtp, portals, etc.).
 *
 * When a player arrives in an enabled world:
 *   - If they have a saved location in that world, teleport them to it
 *     (after the configured delay so Multiverse finishes its own processing).
 *
 * When a player leaves an enabled world:
 *   - Save their location in that world before they leave.
 *     NOTE: At the time PlayerChangedWorldEvent fires, player.getWorld()
 *     already returns the NEW world.  The FROM world is event.getFrom().
 *     We capture the exit location from the event's "from" world using the
 *     player's last known position stored in a separate pass — actually the
 *     cleanest approach is to save on the FROM side here using
 *     player.getLocation() but with the "from" world, since at MONITOR
 *     priority the player is already in the new world.
 *
 *     To keep things simple and reliable we save on QUIT (PlayerQuitListener)
 *     AND here on world-change so that logging out mid-portal or mid-switch
 *     is always covered.
 */
public class PlayerWorldChangeListener implements Listener {

    private final EclipsiaLastLocation plugin;

    public PlayerWorldChangeListener(EclipsiaLastLocation plugin) {
        this.plugin = plugin;
    }

    /**
     * LOWEST priority — fires early so our delayed task is scheduled before
     * other plugins that might teleport on NORMAL/HIGH priority.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World fromWorld = event.getFrom();
        World toWorld   = player.getWorld();

        // ── Save location in the world the player LEFT ────────────────────
        if (fromWorld != null && plugin.isWorldEnabled(fromWorld.getName())) {
            // Build the location the player was at just before the switch.
            // player.getLocation() is in toWorld now, so we reconstruct from
            // the from-world using the player's coordinates (Paper keeps them
            // momentarily valid for logging purposes).
            //
            // The most reliable source: player.getLocation() coords with fromWorld.
            // This is accurate because the position hasn't changed, only the world ref.
            Location fromLoc = player.getLocation().clone();
            fromLoc.setWorld(fromWorld);
            plugin.getLocationStorage().saveLocation(player.getUniqueId(), fromLoc);

            plugin.getLogger().fine("Saved world-change exit location for "
                    + player.getName() + " in " + fromWorld.getName());
        }

        // ── Restore location in the world the player ENTERED ─────────────
        if (toWorld == null) return;
        if (!plugin.isWorldEnabled(toWorld.getName())) return;
        if (!plugin.getLocationStorage().hasLocation(player.getUniqueId())) return;

        long delayTicks = plugin.getTeleportDelayTicks();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {

            if (!player.isOnline()) return;

            // Confirm player is still in the same destination world.
            if (!player.getWorld().getName().equalsIgnoreCase(toWorld.getName())) return;

            Location savedLoc = plugin.getLocationStorage().getLocation(player.getUniqueId());
            if (savedLoc == null || savedLoc.getWorld() == null) return;

            // Only teleport if saved location is in the world the player just entered.
            if (!savedLoc.getWorld().getName().equalsIgnoreCase(toWorld.getName())) return;

            player.teleport(savedLoc);

            plugin.getLogger().fine("Restored world-change location for " + player.getName()
                    + " → " + toWorld.getName()
                    + " (" + savedLoc.getBlockX() + ", " + savedLoc.getBlockY()
                    + ", " + savedLoc.getBlockZ() + ")");

        }, delayTicks);
    }
}
