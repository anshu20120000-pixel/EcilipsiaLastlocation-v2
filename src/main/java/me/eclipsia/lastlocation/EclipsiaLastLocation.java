package me.eclipsia.lastlocation;

import me.eclipsia.lastlocation.commands.EllCommand;
import me.eclipsia.lastlocation.listeners.PlayerJoinListener;
import me.eclipsia.lastlocation.listeners.PlayerQuitListener;
import me.eclipsia.lastlocation.listeners.PlayerWorldChangeListener;
import me.eclipsia.lastlocation.storage.LocationStorage;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * EclipsiaLastLocation — main plugin entry point.
 *
 * Lifecycle:
 *   onEnable  → load config, init storage, register listeners & commands
 *   onDisable → flush storage to disk
 */
public final class EclipsiaLastLocation extends JavaPlugin {

    private LocationStorage locationStorage;

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void onEnable() {
        /* Save default config.yml if it doesn't exist */
        saveDefaultConfig();

        /* Initialise YAML storage (plugins/EclipsiaLastLocation/locations.yml) */
        locationStorage = new LocationStorage(this);
        locationStorage.load();

        /* Register event listeners */
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerWorldChangeListener(this), this);

        /* Register /ell command */
        EllCommand ellCommand = new EllCommand(this);
        var cmd = getCommand("ell");
        if (cmd != null) {
            cmd.setExecutor(ellCommand);
            cmd.setTabCompleter(ellCommand);
        }

        getLogger().info("EclipsiaLastLocation v" + getDescription().getVersion() + " enabled.");
        getLogger().info("Tracking worlds: " + getEnabledWorlds());
    }

    @Override
    public void onDisable() {
        if (locationStorage != null) {
            locationStorage.save();
        }
        getLogger().info("EclipsiaLastLocation disabled — locations saved.");
    }

    // ── Public helpers ───────────────────────────────────────────────────────

    /** Returns the storage manager. */
    public LocationStorage getLocationStorage() {
        return locationStorage;
    }

    /** Returns the configured world whitelist (lower-cased for safe comparison). */
    public List<String> getEnabledWorlds() {
        return getConfig().getStringList("enabled-worlds")
                .stream()
                .map(String::toLowerCase)
                .toList();
    }

    /** Returns true if worldName is in the enabled-worlds list. */
    public boolean isWorldEnabled(String worldName) {
        return getEnabledWorlds().contains(worldName.toLowerCase());
    }

    /** Returns the teleport delay in ticks (config value is in seconds, 20 ticks = 1 second). */
    public long getTeleportDelayTicks() {
        double seconds = getConfig().getDouble("teleport-delay", 2.0);
        return Math.max(1L, (long) (seconds * 20));
    }

    /** Returns the colour-translated message prefix. */
    public String getPrefix() {
        String raw = getConfig().getString("prefix", "&8[&bELL&8] &r");
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', raw);
    }

    /** Reloads config and storage from disk. */
    public void reload() {
        reloadConfig();
        locationStorage.load();
        getLogger().info("Config and locations reloaded.");
    }
}
