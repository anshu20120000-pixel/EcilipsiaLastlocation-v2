package me.eclipsia.lastlocation.storage;

import me.eclipsia.lastlocation.EclipsiaLastLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Handles reading/writing player last-logout locations to
 * plugins/EclipsiaLastLocation/locations.yml
 *
 * Data format (per UUID key):
 *   <uuid>:
 *     world: world
 *     x: 128.5
 *     y: 64.0
 *     z: -200.3
 *     yaw: 90.0
 *     pitch: 0.0
 */
public class LocationStorage {

    private final EclipsiaLastLocation plugin;
    private final File storageFile;
    private YamlConfiguration yaml;

    /** In-memory cache so we don't hit disk on every access. */
    private final Map<UUID, Location> cache = new HashMap<>();

    public LocationStorage(EclipsiaLastLocation plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "locations.yml");
    }

    // ── I/O ─────────────────────────────────────────────────────────────────

    /** Loads (or reloads) all locations from disk into the in-memory cache. */
    public void load() {
        cache.clear();

        if (!storageFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                storageFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create locations.yml", e);
            }
        }

        yaml = YamlConfiguration.loadConfiguration(storageFile);

        for (String key : yaml.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String worldName = yaml.getString(key + ".world");
                double x     = yaml.getDouble(key + ".x");
                double y     = yaml.getDouble(key + ".y");
                double z     = yaml.getDouble(key + ".z");
                float  yaw   = (float) yaml.getDouble(key + ".yaw");
                float  pitch = (float) yaml.getDouble(key + ".pitch");

                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    // World not loaded yet — store a placeholder with null world;
                    // we resolve it lazily when teleporting.
                    plugin.getLogger().warning(
                            "World '" + worldName + "' not loaded for UUID " + uuid + ". Will resolve later.");
                    // Still cache with null world so we keep the coordinates.
                    cache.put(uuid, buildLocation(null, x, y, z, yaw, pitch));
                } else {
                    cache.put(uuid, buildLocation(world, x, y, z, yaw, pitch));
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Skipping invalid UUID key in locations.yml: " + key);
            }
        }

        plugin.getLogger().info("Loaded " + cache.size() + " saved location(s).");
    }

    /** Saves all cached locations to disk. */
    public void save() {
        yaml = new YamlConfiguration();

        for (Map.Entry<UUID, Location> entry : cache.entrySet()) {
            String key = entry.getKey().toString();
            Location loc = entry.getValue();

            if (loc.getWorld() == null) continue; // skip orphaned entries

            yaml.set(key + ".world", loc.getWorld().getName());
            yaml.set(key + ".x",     loc.getX());
            yaml.set(key + ".y",     loc.getY());
            yaml.set(key + ".z",     loc.getZ());
            yaml.set(key + ".yaw",   (double) loc.getYaw());
            yaml.set(key + ".pitch", (double) loc.getPitch());
        }

        try {
            yaml.save(storageFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save locations.yml", e);
        }
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    /**
     * Saves a player's location.
     * Only saves if the world is in the enabled-worlds list.
     */
    public void saveLocation(UUID uuid, Location location) {
        if (location.getWorld() == null) return;
        if (!plugin.isWorldEnabled(location.getWorld().getName())) return;

        cache.put(uuid, location.clone());
    }

    /**
     * Returns the saved location for a player, or null if none exists.
     * Lazily resolves world references that were unloaded at startup.
     */
    public Location getLocation(UUID uuid) {
        Location loc = cache.get(uuid);
        if (loc == null) return null;

        // Lazy world resolution
        if (loc.getWorld() == null && yaml != null) {
            String worldName = yaml.getString(uuid + ".world");
            if (worldName != null) {
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    loc = buildLocation(world, loc.getX(), loc.getY(), loc.getZ(),
                            loc.getYaw(), loc.getPitch());
                    cache.put(uuid, loc);
                }
            }
        }

        return loc;
    }

    /** Returns true if a location is saved for this UUID. */
    public boolean hasLocation(UUID uuid) {
        return cache.containsKey(uuid);
    }

    /** Removes a player's saved location. */
    public void removeLocation(UUID uuid) {
        cache.remove(uuid);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Location buildLocation(World world, double x, double y, double z,
                                   float yaw, float pitch) {
        return new Location(world, x, y, z, yaw, pitch);
    }
}
