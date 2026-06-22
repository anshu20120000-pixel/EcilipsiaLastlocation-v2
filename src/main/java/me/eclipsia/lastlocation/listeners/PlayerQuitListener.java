package me.eclipsia.lastlocation.listeners;

import me.eclipsia.lastlocation.EclipsiaLastLocation;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listens for players quitting and saves their current location
 * if the world they are in is on the enabled-worlds whitelist.
 */
public class PlayerQuitListener implements Listener {

    private final EclipsiaLastLocation plugin;

    public PlayerQuitListener(EclipsiaLastLocation plugin) {
        this.plugin = plugin;
    }

    /**
     * MONITOR priority — runs after all other plugins have handled the quit event,
     * ensuring we capture the player's true final location.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Location location = player.getLocation();

        if (location.getWorld() == null) return;

        String worldName = location.getWorld().getName();

        if (!plugin.isWorldEnabled(worldName)) {
            // Player is in a non-tracked world — do nothing.
            return;
        }

        plugin.getLocationStorage().saveLocation(player.getUniqueId(), location);

        plugin.getLogger().fine("Saved logout location for " + player.getName()
                + " in world " + worldName);
    }
}
