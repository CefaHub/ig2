package com.illit.levels;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.UUID;

public class LevelService {

    private final IllitLevelsPlugin plugin;
    private final PlayerDataStore store;

    private final int maxLevel;
    private final int baseExp;

    private final double growthBaseMult;
    private final double growthSqrtCoef;
    private final double growthLinearCoef;

    private final int endgameThreshold;
    private final double endgameHardMult;
    private final double endgameQuadCoef;
    private final double endgameExpCoef;

    public LevelService(IllitLevelsPlugin plugin, PlayerDataStore store) {
        this.plugin = plugin;
        this.store = store;

        FileConfiguration c = plugin.getConfig();
        this.maxLevel = Math.max(1, c.getInt("max-level", 100));
        this.baseExp = Math.max(1, c.getInt("base-exp", 100));

        this.growthBaseMult = c.getDouble("growth.base-mult", 1.085);
        this.growthSqrtCoef = c.getDouble("growth.sqrt-coef", 0.0025);
        this.growthLinearCoef = c.getDouble("growth.linear-coef", 0.0009);

        this.endgameThreshold = c.getInt("endgame.threshold", 90);
        this.endgameHardMult = c.getDouble("endgame.hard-mult", 1.45);
        this.endgameQuadCoef = c.getDouble("endgame.quad-coef", 0.010);
        this.endgameExpCoef = c.getDouble("endgame.exp-coef", 0.020);
    }

    public PlayerProgress getProgress(UUID uuid) {
        return store.get(uuid);
    }

    public int getLevel(UUID uuid) {
        return store.get(uuid).level();
    }

    public long getExp(UUID uuid) {
        return store.get(uuid).exp();
    }

    public void setLevel(UUID uuid, int level) {
        PlayerProgress p = store.get(uuid);
        int clamped = clamp(level, 0, maxLevel);
        store.put(uuid, new PlayerProgress(clamped, 0));
        store.markDirty(uuid);
    }

    public void reset(UUID uuid) {
        store.put(uuid, new PlayerProgress(0, 0));
        store.markDirty(uuid);
    }

    /**
     * Adds exp (non-negative). Levels up as needed until maxLevel.
     */
    public LevelUpResult addExp(UUID uuid, long amount) {
        if (amount <= 0) return new LevelUpResult(0, false);

        PlayerProgress p = store.get(uuid);
        int level = p.level();
        long exp = p.exp();

        if (level >= maxLevel) {
            // already max, do nothing
            return new LevelUpResult(0, false);
        }

        exp += amount;

        int levelsGained = 0;
        boolean reachedMax = false;

        while (level < maxLevel) {
            long need = getRequiredExpForNextLevel(level);
            if (need <= 0) break;

            if (exp >= need) {
                exp -= need;
                level++;
                levelsGained++;
                if (level >= maxLevel) {
                    exp = 0;
                    reachedMax = true;
                    break;
                }
            } else {
                break;
            }
        }

        store.put(uuid, new PlayerProgress(level, exp));
        store.markDirty(uuid);

        return new LevelUpResult(levelsGained, reachedMax);
    }

    /**
     * Required exp to go from currentLevel -> currentLevel+1.
     * For level 0 -> 1, this will be baseExp.
     * If currentLevel >= maxLevel, returns 0.
     */
    public long getRequiredExpForNextLevel(int currentLevel) {
        if (currentLevel >= maxLevel) return 0;

        int nextLevel = currentLevel + 1;
        // Growth factor is based on nextLevel to make "each new level harder".
        double lvl = (double) nextLevel;

        // Complex growth:
        // baseline growthBaseMult^(lvl-1) * (1 + sqrtCoef*sqrt(lvl) + linearCoef*lvl)
        double expFactor = Math.pow(growthBaseMult, Math.max(0.0, lvl - 1.0));
        double bump = 1.0 + growthSqrtCoef * Math.sqrt(lvl) + growthLinearCoef * lvl;
        double factor = expFactor * bump;

        // Endgame difficulty for last 10 levels (>= threshold)
        if (nextLevel >= endgameThreshold) {
            double k = (lvl - endgameThreshold + 1.0); // starts at 1
            double quad = 1.0 + endgameQuadCoef * (k * k);      // quadratic spike
            double extraExp = Math.exp(endgameExpCoef * k);     // exponential spike
            factor = factor * endgameHardMult * quad * extraExp;
        }

        long required = Math.max(1L, Math.round(baseExp * factor));

        // Safety: prevent overflow in pathological configs
        if (required < 0) required = Long.MAX_VALUE / 4;

        return required;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    public int getNextLevel(UUID uuid) {
        int level = getLevel(uuid);
        return Math.min(level + 1, maxLevel);
    }

    public long getExpToNext(UUID uuid) {
        int level = getLevel(uuid);
        if (level >= maxLevel) return 0;
        long need = getRequiredExpForNextLevel(level);
        long cur = getExp(uuid);
        return Math.max(0, need - cur);
    }

    // Convenience overloads for placeholders
    public int getLevel(OfflinePlayer player) {
        if (player == null) return 0;
        return getLevel(player.getUniqueId());
    }

    public long getExp(OfflinePlayer player) {
        if (player == null) return 0;
        return getExp(player.getUniqueId());
    }

    public int getNextLevel(OfflinePlayer player) {
        if (player == null) return 1;
        return getNextLevel(player.getUniqueId());
    }

    public long getExpToNext(OfflinePlayer player) {
        if (player == null) return getRequiredExpForNextLevel(0);
        return getExpToNext(player.getUniqueId());
    }
}
