package com.illit.levels;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class IllitLevelsPlugin extends JavaPlugin {

    private PlayerDataStore dataStore;
    private LevelService levelService;
    private TopService topService;
    private int autosaveTaskId = -1;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.dataStore = new PlayerDataStore(this);
        this.levelService = new LevelService(this, dataStore);
        this.topService = new TopService(this, dataStore);

        IllitCommand cmd = new IllitCommand(levelService, topService);
        getCommand("illit").setExecutor(cmd);
        getCommand("illit").setTabCompleter(cmd);

        new IllitPlaceholders(this, levelService, topService).register();

        int autosaveSeconds = Math.max(10, getConfig().getInt("storage.autosave-seconds", 60));
        autosaveTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            try {
                dataStore.saveAll();
            } catch (Exception e) {
                getLogger().warning("Autosave failed: " + e.getMessage());
            }
        }, autosaveSeconds * 20L, autosaveSeconds * 20L);

        getLogger().info("IllitLevels enabled.");
    }

    @Override
    public void onDisable() {
        if (autosaveTaskId != -1) {
            Bukkit.getScheduler().cancelTask(autosaveTaskId);
        }
        try {
            dataStore.saveAll();
        } catch (Exception e) {
            getLogger().warning("Final save failed: " + e.getMessage());
        }
        getLogger().info("IllitLevels disabled.");
    }
}
