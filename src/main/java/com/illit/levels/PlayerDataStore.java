package com.illit.levels;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataStore {

    private final IllitLevelsPlugin plugin;
    private final File file;
    private YamlConfiguration yaml;

    private final Map<UUID, PlayerProgress> cache = new ConcurrentHashMap<>();
    private final Set<UUID> dirty = Collections.synchronizedSet(new HashSet<>());

    public PlayerDataStore(IllitLevelsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
        load();
    }

    public void load() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create players.yml: " + e.getMessage());
            }
        }
        this.yaml = YamlConfiguration.loadConfiguration(file);

        cache.clear();
        dirty.clear();

        for (String key : yaml.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                int level = yaml.getInt(key + ".level", 1);
                long exp = yaml.getLong(key + ".exp", 0L);
                String name = yaml.getString(key + ".name", null);
                cache.put(uuid, new PlayerProgress(level, exp, name));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public PlayerProgress get(UUID uuid) {
        return cache.computeIfAbsent(uuid, (u) -> new PlayerProgress(1, 0, null));
    }

    public void put(UUID uuid, PlayerProgress progress) {
        cache.put(uuid, progress);
    }

    public void setName(UUID uuid, String name) {
        if (name == null || name.isBlank()) return;
        PlayerProgress p = get(uuid);
        if (name.equals(p.name())) return;
        cache.put(uuid, new PlayerProgress(p.level(), p.exp(), name));
        markDirty(uuid);
    }

    public Map<UUID, PlayerProgress> snapshotAll() {
        return new HashMap<>(cache);
    }

    public void markDirty(UUID uuid) {
        dirty.add(uuid);
    }

    public void saveAll() {
        synchronized (dirty) {
            for (UUID uuid : dirty) {
                PlayerProgress p = cache.get(uuid);
                if (p == null) continue;
                String key = uuid.toString();
                yaml.set(key + ".level", p.level());
                yaml.set(key + ".exp", p.exp());
                if (p.name() != null) yaml.set(key + ".name", p.name());
            }
            dirty.clear();
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save players.yml: " + e.getMessage());
        }
    }
}
