package com.illit.levels;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class IllitPlaceholders extends PlaceholderExpansion {

    private final IllitLevelsPlugin plugin;
    private final LevelService levelService;
    private final TopService topService;

    public IllitPlaceholders(IllitLevelsPlugin plugin, LevelService levelService, TopService topService) {
        this.plugin = plugin;
        this.levelService = levelService;
        this.topService = topService;
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
        String p = params.toLowerCase();

        // Required placeholders
        switch (p) {
            case "level":
                return String.valueOf(levelService.getLevel(player));
            case "exp":
                return String.valueOf(levelService.getExp(player));
            case "level_next":
                return String.valueOf(levelService.getNextLevel(player));
            case "exp_next":
                return String.valueOf(levelService.getExpToNext(player));
        }

        // Formatted placeholders
        if (p.equals("level_format") || p.equals("level_formatted")) {
            int lvl = levelService.getLevel(player);
            return lvl + "/" + levelService.getMaxLevel();
        }

        if (p.equals("exp_format") || p.equals("exp_formatted")) {
            int lvl = levelService.getLevel(player);
            if (lvl >= levelService.getMaxLevel()) return "0/0";
            long cur = levelService.getExp(player);
            long need = levelService.getRequiredExpForNextLevel(lvl);
            return cur + "/" + need;
        }

        if (p.equals("progress_percent")) {
            int lvl = levelService.getLevel(player);
            if (lvl >= levelService.getMaxLevel()) return "100";
            long cur = levelService.getExp(player);
            long need = levelService.getRequiredExpForNextLevel(lvl);
            if (need <= 0) return "0";
            int rounded = (int) Math.max(0, Math.min(100, Math.round((cur * 100.0) / need)));
            return String.valueOf(rounded);
        }

        if (p.equals("progress_bar")) {
            int lvl = levelService.getLevel(player);
            int len = Math.max(5, plugin.getConfig().getInt("format.bar-length", 10));
            String filled = plugin.getConfig().getString("format.bar-filled", "█");
            String empty = plugin.getConfig().getString("format.bar-empty", "░");
            if (lvl >= levelService.getMaxLevel()) return repeat(filled, len);

            long cur = levelService.getExp(player);
            long need = levelService.getRequiredExpForNextLevel(lvl);
            if (need <= 0) return repeat(empty, len);

            double ratio = Math.max(0.0, Math.min(1.0, cur / (double) need));
            int f = (int) Math.round(ratio * len);
            if (f > len) f = len;
            return repeat(filled, f) + repeat(empty, len - f);
        }

        // Top placeholders: %illit_top_<rank>_name% / %illit_top_<rank>_level%
        if (p.startsWith("top_")) {
            String[] parts = p.split("_");
            if (parts.length == 3) {
                try {
                    int rank = Integer.parseInt(parts[1]);
                    String field = parts[2];
                    TopService.TopEntry e = topService.getRank(rank);
                    if (e == null) return "";
                    return switch (field) {
                        case "name" -> e.name();
                        case "level" -> String.valueOf(e.level());
                        default -> null;
                    };
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }

        return null;
    }

    private String repeat(String s, int n) {
        if (s == null || s.isEmpty() || n <= 0) return "";
        return s.repeat(n);
    }
}
