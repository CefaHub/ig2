package com.illit.levels;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class IllitPlaceholders extends PlaceholderExpansion {

    private final IllitLevelsPlugin plugin;
    private final LevelService levelService;

    public IllitPlaceholders(IllitLevelsPlugin plugin, LevelService levelService) {
        this.plugin = plugin;
        this.levelService = levelService;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "illit";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        switch (params.toLowerCase()) {
            case "level":
                return String.valueOf(levelService.getLevel(player));
            case "exp":
                return String.valueOf(levelService.getExp(player));
            case "level_next":
                return String.valueOf(levelService.getNextLevel(player));
            case "exp_next":
                return String.valueOf(levelService.getExpToNext(player));
            default:
                return null;
        }
    }
}
